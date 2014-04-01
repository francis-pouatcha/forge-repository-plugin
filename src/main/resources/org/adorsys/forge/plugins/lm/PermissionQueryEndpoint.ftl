package ${topPackage}.lm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import ${topPackage}.jpa.${LoginTable};
import ${topPackage}.jpa.${LoginTable}${RoleTable}Assoc;
import ${topPackage}.jpa.${LoginTable}_;
import ${topPackage}.jpa.${PermissionTable};
import ${topPackage}.jpa.${RoleTable};
import ${topPackage}.jpa.${RoleTable}${PermissionTable}Assoc;
import ${topPackage}.rest.${LoginTable}EJB;

/**
 * Allows the application query permissions associated with a login name. Each permission will carry
 * the form <ServerSideComponentName>(<Action,Action,Action>):<ServerSideComponentName>(<Action,Action,Action>)
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Path("/perms")
public class PermissionQueryEndpoint {

	@Inject
	private ${LoginTable}EJB loginEJB;

	@SuppressWarnings("unchecked")
	@POST
	@Consumes({ "application/json", "application/xml" })
	@Produces({ "application/json", "application/xml" })
	public String perms(String loginName) {
		${LoginTable} login = new ${LoginTable}();
		login.setLoginName(loginName);
		List<${LoginTable}> logins = loginEJB.findBy(login, 0, -1,
				new SingularAttribute[] { ${LoginTable}_.loginName });
		${LoginTable} foundLogin = logins.iterator().next();

		Set<${LoginTable}${RoleTable}Assoc> roleNames = foundLogin.get${RoleTable}s();
		Map<String, List<String>> permissionMap = new HashMap<String, List<String>>(); 
		for (${LoginTable}${RoleTable}Assoc l : roleNames) {
			${RoleTable} roleName = l.getTarget();
			Set<${RoleTable}${PermissionTable}Assoc> permissions = roleName.getPermissions();
			for (${RoleTable}${PermissionTable}Assoc p : permissions) {
				${PermissionTable} permissionName = p.getTarget();
				String pn = permissionName.getName();
				List<String> actionList = null;
				if(permissionMap.containsKey(pn)){
					actionList = permissionMap.get(pn);
				} else {
					actionList = new ArrayList<String>();
					permissionMap.put(pn, actionList);
				}
				if(!actionList.contains(permissionName.getAction().name())){
					actionList.add(permissionName.getAction().name());
				}
			}
		}
		StringBuilder result = new StringBuilder();
		Set<Entry<String,List<String>>> entrySet = permissionMap.entrySet();
		for (Entry<String, List<String>> e : entrySet) {
			if(result.length()>0)result.append(':');
			result.append(e.getKey()).append('(');
			List<String> actions = e.getValue();
			for (String action : actions) {
				result.append(action).append(',');
			}
			result.append(')');
		}
		return result.toString();
	}
}