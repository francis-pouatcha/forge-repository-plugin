package ${topPackage}.startup;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;

import ${topPackage}.jpa.${RoleEnum};
import ${topPackage}.jpa.${RoleTable};
import ${topPackage}.jpa.${RoleTable}_;
import ${topPackage}.jpa.${LoginTable};
import ${topPackage}.jpa.${LoginTable}${RoleTable}Assoc;
import ${topPackage}.jpa.${PermissionActionEnum};
import ${topPackage}.jpa.${PermissionTable};
import ${topPackage}.jpa.${PermissionTable}_;
import ${topPackage}.jpa.${RoleTable}${PermissionTable}Assoc;
import ${topPackage}.rest.${RoleTable}EJB;
import ${topPackage}.rest.${LoginTable}EJB;
import ${topPackage}.rest.${PermissionTable}EJB;
import ${topPackage}.rest.${LoginTable}${RoleTable}AssocEJB;
import ${topPackage}.rest.${RoleTable}${PermissionTable}AssocEJB;

import org.apache.commons.lang3.StringUtils;


@Singleton
@Startup
public class InitUserAccountService {

	@Inject
	private ${LoginTable}EJB loginEJB;
	
	@Inject
	private ${RoleTable}EJB roleEJB;
	
	@Inject
	private ${LoginTable}${RoleTable}AssocEJB assocEJB;

	@Inject
	private ${PermissionTable}EJB permissionEJB;

	@Inject
	private ${RoleTable}${PermissionTable}AssocEJB roleNamePermissionNameAssocEJB;
	
	@PostConstruct
	protected void postConstruct(){

      	Properties logins = new Properties();
      	Properties roles = new Properties();
		Properties permissions = new Properties();
      	InputStream loginStream = this.getClass().getClassLoader().getResourceAsStream("logins.properties");
      	if(loginStream !=null){
      		try {
	    	  	logins.load(loginStream);
	    	  	InputStream roleStream = this.getClass().getClassLoader().getResourceAsStream("roles.properties");
	    	  	if(roleStream!=null){
	    			roles.load(roleStream);
	    	  	}
				InputStream permissionStream = this.getClass().getClassLoader()
						.getResourceAsStream("permissions.properties");
				if (permissionStream != null) {
					permissions.load(permissionStream);
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
      	}

      	<#list jpaEntities as entityFqn>
      	createPermission("${entityFqn}");
      	</#list>

		${RoleEnum}[] roleEnums = ${RoleEnum}.values();
		for (${RoleEnum} roleEnum : roleEnums) {
			${RoleTable} role = new ${RoleTable}();
			role.set${roleNameField?cap_first}(roleEnum.name());
			role = roleEJB.create(role);

			String permissionString = (String) permissions.get(role.getName());
			if (StringUtils.isBlank(permissionString))
				continue;
			String[] perms = permissionString.split(",");
			for (String perm : perms) {
				String permName = StringUtils.substringBefore(perm, "(");
				if (StringUtils.isBlank(permName))
					continue;
				String action = null;
				if (perm.contains("(")) {
					action = StringUtils.substringAfter(perm, "(");
					action = StringUtils.substringBefore(action, ")");
				}

				addPermission(role.getName(), permName, action);
			}

		}


      	
      	
      	Set<Entry<Object,Object>> loginSet = logins.entrySet();
      	for (Entry<Object, Object> entry : loginSet) {
    	  	String loginName = (String) entry.getKey();
    	  	String password = (String) entry.getValue();
			${LoginTable} login = new ${LoginTable}();
			login.set${loginNameField?cap_first}(loginName);
			login.set${fullNameField?cap_first}(loginName);
			login.set${passwordField?cap_first}(password);
			login = loginEJB.create(login);
    	  	String roleNames = roles.getProperty(loginName);
    	  	if(StringUtils.isBlank(roleNames)) continue;
    	  	String[] split = roleNames.split(",");
    	  	for (String roleName : split) {
    	  		roleName = roleName.trim();
    	  		${RoleTable} searchRole = new ${RoleTable}();
    	  		searchRole.set${roleNameField?cap_first}(roleName);
				List<${RoleTable}> found = roleEJB.findBy(searchRole, 0, 1, new SingularAttribute[]{${RoleTable}_.${roleNameField}});
				if(found.isEmpty())throw new IllegalStateException("${RoleTable} with ${roleNameField} " + roleName + " not found.");
				${RoleTable} role = found.iterator().next();
				
				${LoginTable}${RoleTable}Assoc roleAssoc = new ${LoginTable}${RoleTable}Assoc();
				roleAssoc.setSource(login);
				roleAssoc.setTarget(role);
				roleAssoc.setSourceQualifier("roleNames");
				roleAssoc.setTargetQualifier("source");
				roleAssoc = assocEJB.create(roleAssoc);
			}
      	}
	}
	
   private void createPermission(String entityFqn){
	   ${PermissionActionEnum}[] values = ${PermissionActionEnum}.values();
	   for (${PermissionActionEnum} action : values) {
		   createPermission(entityFqn, action);
		   createPermission(entityFqn, action);
		   createPermission(entityFqn, action);
	   }
   }   
   private void createPermission(String entityFqn, ${PermissionActionEnum} action){
	   ${PermissionTable} entity = new ${PermissionTable}();
	   entity.set${permissionActionField?cap_first}(action);
	   entity.set${permissionNameField?cap_first}(entityFqn);
	   permissionEJB.create(entity);
   }


	private void addPermission(String roleName, String permName, String action) {
		${PermissionActionEnum} permissionActionEnum = ${PermissionActionEnum}.valueOf(action);
		${PermissionTable} entity = new ${PermissionTable}();
		entity.setAction(permissionActionEnum);
		entity.setName(permName);
		
		List<${PermissionTable}> permFound = permissionEJB.findBy(entity, 0, 1, new SingularAttribute[] { ${PermissionTable}_.name, ${PermissionTable}_.action });
		if(permFound.isEmpty()) throw new IllegalStateException("Permission with name " + permName + " and action " + action + " not found");
		${PermissionTable} permissionName = permFound.iterator().next();
		
		${RoleTable} searchRole = new ${RoleTable}();
		searchRole.setName(roleName);
		List<${RoleTable}> roleFound = roleEJB.findBy(searchRole, 0, 1,
				new SingularAttribute[] { ${RoleTable}_.name });
		if (roleFound.isEmpty())
			throw new IllegalStateException("${RoleTable} with name "
					+ roleName + " not found.");
		${RoleTable} role = roleFound.iterator().next();
		${RoleTable}${PermissionTable}Assoc roleNamePermissionNameAssoc = new ${RoleTable}${PermissionTable}Assoc();
		roleNamePermissionNameAssoc.setSource(role);
		roleNamePermissionNameAssoc.setTarget(permissionName);
		roleNamePermissionNameAssoc.setSourceQualifier("permissions");
		roleNamePermissionNameAssoc.setTargetQualifier("source");
		roleNamePermissionNameAssocEJB.create(roleNamePermissionNameAssoc);
	}
	
}
