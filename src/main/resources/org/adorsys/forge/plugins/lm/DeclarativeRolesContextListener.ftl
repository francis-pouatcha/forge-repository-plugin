package ${topPackage}.lm;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import ${topPackage}.jpa.${RoleEnum};

@WebListener
public class DeclarativeRolesContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext servletContext =  sce.getServletContext();
		${RoleEnum}[] roleEnums = ${RoleEnum}.values();
		for (${RoleEnum} roleEnum : roleEnums) {
			servletContext.declareRoles(roleEnum.name());
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub

	}

}
