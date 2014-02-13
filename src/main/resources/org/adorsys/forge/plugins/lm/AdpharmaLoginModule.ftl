package org.adorsys.adpharma.server.lm;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.metamodel.SingularAttribute;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.adorsys.adpharma.server.jpa.RoleName;
import org.adorsys.adpharma.server.jpa.Users;
import org.adorsys.adpharma.server.jpa.UsersRoleNameAssoc;
import org.adorsys.adpharma.server.jpa.UsersRoleNameAssoc_;
import org.adorsys.adpharma.server.jpa.Users_;
import org.adorsys.adpharma.server.rest.UsersEJB;
import org.adorsys.adpharma.server.rest.UsersRoleNameAssocEJB;

public class AdpharmaLoginModule implements LoginModule {

	protected Subject subject;
	protected CallbackHandler callbackHandler;
	protected Map<String, ?> sharedState;
	protected Map<String, ?> options;
	protected Logger log;
	protected boolean trace = false;

	/**
	 * Flag indicating if the login phase succeeded. Subclasses that override
	 * the login method must set this to true on successful completion of login
	 */
	protected boolean loginOk;
	/** the principal to use when a null username and password are seen */
	protected Principal unauthenticatedIdentity;

	private UsersEJB usersEJB;

	private UsersRoleNameAssocEJB usersRoleNameAssocEJB;

	private Users user;

	// --- Begin LoginModule interface methods
	/**
	 * Initializes the login module. This stores the subject, callbackHandler
	 * and sharedState and options for the login session. Subclasses should
	 * override if they need to process their own options. A call to
	 * super.initialize(...) must be made in the case of an override.
	 * <p>
	 * 
	 * @option password-stacking: If this is set to "useFirstPass", the login
	 *         identity will be taken from the
	 *         <code>javax.security.auth.login.name</code> value of the
	 *         sharedState map, and the proof of identity from the
	 *         <code>javax.security.auth.login.password</code> value of the
	 *         sharedState map.
	 * @option principalClass: A Principal implementation that support actor
	 *         taking a String argument for the princpal name.
	 * @option unauthenticatedIdentity: the name of the principal to assign and
	 *         authenticate when a null username and password are seen.
	 * 
	 * @param subject
	 *            the Subject to update after a successful login.
	 * @param callbackHandler
	 *            the CallbackHandler that will be used to obtain the the user
	 *            identity and credentials.
	 * @param sharedState
	 *            a Map shared between all configured login module instances
	 * @param options
	 *            the parameters passed to the login module.
	 */
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		InitialContext initialContext;
		try {
			initialContext = new InitialContext();
			usersEJB = (UsersEJB) initialContext.lookup("java:module/UsersEJB");
			usersRoleNameAssocEJB = (UsersRoleNameAssocEJB) initialContext
					.lookup("java:module/UsersRoleNameAssocEJB");
		} catch (NamingException e1) {
			throw new IllegalStateException(e1);
		}

		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = options;
		log = Logger.getLogger(getClass().getName());
		trace = log.isLoggable(Level.FINER);
		if (trace) {
			log.finer("initialize");

			// log securityDomain, if set.
			log.finer("Security domain: "
					+ (String) options
							.get(SecurityConstants.SECURITY_DOMAIN_OPTION));
		}

		// Check for unauthenticatedIdentity option.
		String name = (String) options.get("unauthenticatedIdentity");
		if (name != null) {
			try {
				unauthenticatedIdentity = new SimplePrincipal(name);
				if (trace)
					log.finer("Saw unauthenticatedIdentity=" + name);
			} catch (Exception e) {
				log.warning("Failed to create custom unauthenticatedIdentity: "
						+ e.getMessage());
			}
		}
	}

	@Override
	public boolean login() throws LoginException {
		NameCallback nameCallback = new NameCallback("Enter your user name: ");
		PasswordCallback passwordCallback = new PasswordCallback(
				"Enter your password", false);
		try {
			callbackHandler.handle(new Callback[] { nameCallback,
					passwordCallback });
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (UnsupportedCallbackException e) {
			throw new IllegalStateException(e);
		}

		String userName = nameCallback.getName();
		Users retrieveUsers = retrieveUsers(userName);
		char[] password = passwordCallback.getPassword();
		if (!new String(password).equals(retrieveUsers.getPassword()))
			return false;
		user = retrieveUsers;
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		if (user == null)
			return false;

		/*
		 * The set of principals of this subject. We will add the 
		 * SecurityConstants.CALLER_PRINCIPAL_GROUP and the 
		 * SecurityConstants.ROLES_GROUP to this set.
		 */
		Set<Principal> principals = subject.getPrincipals();
		
		/*
		 * The user identity.
		 */
		Principal identity = new SimplePrincipal(user.getUserName());
		principals.add(identity);
		
		// get the CallerPrincipal group
		Group callerGroup = findGroup(SecurityConstants.CALLER_PRINCIPAL_GROUP, principals);
		if (callerGroup == null) {
			callerGroup = new SimpleGroup(SecurityConstants.CALLER_PRINCIPAL_GROUP);
			principals.add(callerGroup);
		}
		// Add this principal to the group.
		callerGroup.addMember(identity);

		// get the Roles group
		Group[] roleSets = getRoleSets();
		for (Group group : roleSets) {
			Group sunjectGroup = findGroup(group.getName(), principals);
			if (sunjectGroup == null) {
				sunjectGroup = new SimpleGroup(group.getName());
				principals.add(sunjectGroup);
			}
			// Copy the group members to the Subject group
			Enumeration<? extends Principal> members = group.members();
			while (members.hasMoreElements()) {
				Principal role = (Principal) members.nextElement();
				sunjectGroup.addMember(role);
			}
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		if (trace)
			log.finer("abort");
		user = null;
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (trace)
			log.finer("logout");
		
		// Remove all principals and groups of classes defined here.
		Set<Principal> principals = subject.getPrincipals();
		Set<SimplePrincipal> principals2Remove = subject.getPrincipals(SimplePrincipal.class);
		principals.removeAll(principals2Remove);
		
		return true;
	}

	private Users retrieveUsers(String userName) throws FailedLoginException {
		Users entity = new Users();
		entity.setUserName(userName);
		@SuppressWarnings("rawtypes")
		SingularAttribute[] attributes = new SingularAttribute[] { Users_.userName };
		@SuppressWarnings("unchecked")
		List<Users> found = usersEJB.findBy(entity, 0, 1, attributes);
		if (found.isEmpty()) {
			throw new FailedLoginException(
					"PB00019: Processing Failed: No matching username found with user name: "
							+ userName);
		}
		return found.iterator().next();
	}

	private Group[] getRoleSets() {
		UsersRoleNameAssoc entity = new UsersRoleNameAssoc();
		entity.setSource(user);
		@SuppressWarnings("rawtypes")
		SingularAttribute[] attributes = new SingularAttribute[] { UsersRoleNameAssoc_.source };
		@SuppressWarnings("unchecked")
		List<UsersRoleNameAssoc> found = usersRoleNameAssocEJB.findBy(entity,
				0, 1, attributes);
		SimpleGroup simpleGroup = new SimpleGroup(SecurityConstants.ROLES_GROUP);
		for (UsersRoleNameAssoc usersRoleNameAssoc : found) {
			RoleName target = usersRoleNameAssoc.getTarget();
			simpleGroup.addMember(new SimplePrincipal(target.getName()));
		}

		return new Group[] { simpleGroup };
	}
	
	private Group findGroup(String name, Set<Principal> principals){
		for (Principal principal : principals) {
			if(!(principal instanceof Group)) continue;
			Group group = Group.class.cast(principal);
			if(name.equals(group.getName())) return group;
		}
		return null;
	}
}
