package ${topPackage}.startup;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import ${topPackage}.jpa.Gender;
import ${topPackage}.jpa.RoleName;
import ${topPackage}.jpa.Users;
import ${topPackage}.jpa.UsersRoleNameAssoc;
import ${topPackage}.rest.RoleNameEJB;
import ${topPackage}.rest.UsersEJB;
import ${topPackage}.rest.UsersRoleNameAssocEJB;

@Singleton
@Startup
public class InitUserAccountService {

	private static final String ADMIN_LOGIN_NAME = "admin";
	private static final String ADMIN_PWD = "admin";
	private static final String USER_ROLE_NAME = "user";
	private static final String ADMIN_ROLE_NAME = "admin";
	
	@Inject
	private RoleNameEJB roleNameEJB;
	
	@Inject
	private UsersEJB usersEJB;
	
	@Inject
	private UsersRoleNameAssocEJB usersRoleNameAssocEJB;
	
	@PostConstruct
	protected void initUsersAndRoles(){
		
		RoleName userRoleName = new RoleName();
		userRoleName.setName(USER_ROLE_NAME);
		userRoleName = roleNameEJB.create(userRoleName);
		
		RoleName adminRoleName = new RoleName();
		adminRoleName.setName(ADMIN_ROLE_NAME);
		adminRoleName = roleNameEJB.create(adminRoleName);
		
		Users adminUser = new Users();
		adminUser.setGender(Gender.NEUTRAL);
		adminUser.setFirstName("Admin First Name");
		adminUser.setLastName("Admin Last Name");
		adminUser.setFullName("Admin Last Name");
		adminUser.setPassword(ADMIN_PWD);
		adminUser.setUserName(ADMIN_LOGIN_NAME);
		
		adminUser = usersEJB.create(adminUser);
		
		UsersRoleNameAssoc adminUserRole = new UsersRoleNameAssoc();
		adminUserRole.setSource(adminUser);
		adminUserRole.setTarget(userRoleName);
		adminUserRole.setSourceQualifier("roleNames");
		adminUserRole.setTargetQualifier("source");
		adminUserRole = usersRoleNameAssocEJB.create(adminUserRole);
		
		UsersRoleNameAssoc adminAdminRoleName = new UsersRoleNameAssoc();
		adminAdminRoleName.setSource(adminUser);
		adminAdminRoleName.setTarget(adminRoleName);
		adminAdminRoleName.setSourceQualifier("roleNames");
		adminAdminRoleName.setTargetQualifier("source");
		adminAdminRoleName = usersRoleNameAssocEJB.create(adminAdminRoleName);
	}
}
