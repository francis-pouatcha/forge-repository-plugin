package ${topPackage}.startup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import ${topPackage}.rest.${RoleTable}EJB;
import ${topPackage}.rest.${LoginTable}EJB;
import ${topPackage}.rest.${PermissionTable}EJB;
import ${topPackage}.rest.${LoginTable}${RoleTable}AssocEJB;

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
	
	@PostConstruct
	protected void postConstruct(){

		${RoleEnum}[] roleEnums = ${RoleEnum}.values();
		for (${RoleEnum} roleEnum : roleEnums) {
			${RoleTable} role = new ${RoleTable}();
			role.set${roleNameField?cap_first}(roleEnum.name());
			role = roleEJB.create(role);
		}		

      	Properties logins = new Properties();
      	Properties roles = new Properties();
      	InputStream loginStream = this.getClass().getClassLoader().getResourceAsStream("logins.properties");
      	if(loginStream !=null){
      		try {
	    	  	logins.load(loginStream);
	    	  	InputStream roleStream = this.getClass().getClassLoader().getResourceAsStream("roles.properties");
	    	  	if(roleStream!=null){
	    			roles.load(roleStream);
	    	  	}
			} catch (IOException e) {
				throw new IllegalStateException(e);
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
    	  	List<String> roleList = new ArrayList<String>(split.length);
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
      	
      	<#list jpaEntities as entityFqn>
      	createPermission("${entityFqn}");
      	</#list>
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
	
}
