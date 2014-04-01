package org.adorsys.forge.plugins.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.adorsys.forge.plugins.repo.RepositoryFacet;
import org.adorsys.forge.plugins.repo.RepositoryGenerator;
import org.adorsys.forge.plugins.utils.ContentTypeCompleter;
import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.adorsys.forge.plugins.utils.PluginUtils;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.adorsys.javaext.admin.LoginRole;
import org.adorsys.javaext.admin.LoginTable;
import org.adorsys.javaext.admin.PermissionTable;
import org.adorsys.javaext.admin.RoleTable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.EnumConstant;
import org.jboss.forge.parser.java.Import;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaEnum;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.project.ProjectScoped;
import org.jboss.forge.spec.javaee.EJBFacet;
import org.jboss.forge.spec.javaee.JTAFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.spec.javaee.RestActivatorType;
import org.jboss.forge.spec.javaee.RestFacet;

@Alias("reporest")
@RequiresProject
@RequiresFacet({RepositoryFacet.class, RestFacet.class})
public class Rest2Plugin implements Plugin {

	@Inject
	private Project project;

	@Inject
	private Event<InstallFacets> request;

	@Inject
	private Event<RepoGeneratedResources> generatedEvent;

	@Inject
	@Current
	private Resource<?> currentResource;

	@Inject
	@ProjectScoped
	private Configuration configuration;

	@Inject
	private RepositoryGenerator repositoryGenerator;
	
	@Inject
	EntityBasedResourceGenerator entityResourceGenerator;

	@Inject
	private PluginUtils pluginUtils;
	
	@Inject
	private Shell shell;

	@Inject
	FreemarkerTemplateProcessor processor;	

	@SetupCommand
	public void setup(
			@Option(name = "activatorType", defaultValue = "WEB_XML") RestActivatorType activatorType,
			final PipeOut out) 
	{
		if (!project.hasFacet(RepositoryFacet.class)) {
			request.fire(new InstallFacets(RepositoryFacet.class));
		}
		
		if (!project.hasFacet(RestFacet.class)) {
			configuration.setProperty(RestFacet.ACTIVATOR_CHOICE,
					activatorType.toString());
			request.fire(new InstallFacets(RestFacet.class));
		}

		if (project.hasFacet(RestFacet.class)) {
			ShellMessages.success(out,
					"Rest Web Services (JAX-RS) is installed.");
		}
		entityResourceGenerator.writeMergerUtils();		
	}

	@SuppressWarnings("unchecked")
	@Command(value = "endpoint-from-entity", help = "Creates a REST endpoint from an existing domain @Entity object")
	public void endpointFromEntity(
			final PipeOut out,
			@Option(name = "contentType", defaultValue = MediaType.APPLICATION_XML, completer = ContentTypeCompleter.class) String contentType,
			@Option(name = "jpaClass", required = false, type = PromptType.JAVA_CLASS) JavaResource jpaClass,
			@Option(name = "jpaPackage", required = false, type = PromptType.FILE_PATH) Resource<?> jpaPackage,
			@Option(name="overrride", flagOnly=true, required=false ) boolean override)
			throws FileNotFoundException 
	{
		/*
		 * Make sure we have all the features we need for this to work.
		 */
		if (!project.hasAllFacets(Arrays.asList(EJBFacet.class,
				PersistenceFacet.class))) {
			request.fire(new InstallFacets(true, JTAFacet.class,
					EJBFacet.class, PersistenceFacet.class));
		}

		List<JavaResource> targetsToProcess = pluginUtils.calculateTargetToProcess(jpaClass, jpaPackage, out);
		if (targetsToProcess.isEmpty()) {
			ShellMessages.info(out,
					"No class with annotation @Entity found to operate on.");
			return;
		}

		RepoGeneratedResources repoGeneratedResources = new RepoGeneratedResources();

		for (JavaResource jr : targetsToProcess) {

			JavaClass entity = (JavaClass) (jr).getJavaSource();

			String idType = pluginUtils.resolveIdType(entity);
			if (idType==null) continue;

			repoGeneratedResources.addToEntities(jr);
			JavaInterface repoResource = repositoryGenerator.generateFrom(entity, idType, repoGeneratedResources, override, out);

			entityResourceGenerator.generateFrom(entity, idType, contentType, repoResource, repoGeneratedResources, override, out);
		}
		
		if (!repoGeneratedResources.getEntities().isEmpty()) {
			generatedEvent.fire(repoGeneratedResources);
		}
	}
	
	@Command(value = "access-control", help = "Creates a REST endpoint from an existing domain @Entity object")
	public void accessControl(
			final PipeOut out,
			@Option(name = "roleTable", type = PromptType.JAVA_CLASS) JavaResource roleTable,
			@Option(name = "permissionTable", type = PromptType.JAVA_CLASS) JavaResource permissionTable,
			@Option(name = "loginTable", type = PromptType.JAVA_CLASS) JavaResource loginTable) throws FileNotFoundException
	{
		JavaClass roleTableSource;
		JavaClass loginTableSource;
		JavaClass permissionTableSource;
		try {
			roleTableSource = (JavaClass) roleTable.getJavaSource();
			loginTableSource = (JavaClass) loginTable.getJavaSource();
			permissionTableSource = (JavaClass) permissionTable.getJavaSource();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		
		Map<Object, Object> map = new HashMap<Object, Object>();
		MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
		map.put("topPackage", metadataFacet.getTopLevelPackage());
		map.put("projectName", StringUtils.substringBefore(metadataFacet.getProjectName(), "."));
		map.put("RoleTable", roleTableSource.getName());
		map.put("LoginTable", loginTableSource.getName());
		
		Annotation<JavaClass> loginTableAnnotation = loginTableSource.getAnnotation(LoginTable.class);
		map.put("loginNameField", loginTableAnnotation.getStringValue("loginNameField"));
		map.put("fullNameField", loginTableAnnotation.getStringValue("fullNameField"));
		map.put("passwordField", loginTableAnnotation.getStringValue("passwordField"));
		
		
		List<String> roleNames = new ArrayList<String>();
		List<String> loginRoles = new ArrayList<String>();
		Annotation<JavaClass> roleTableAnnotation = roleTableSource.getAnnotation(RoleTable.class);
		if(roleTableAnnotation==null)throw new IllegalStateException("Missing RoleTable annotation");
		map.put("roleNameField", roleTableAnnotation.getStringValue("roleNameField"));
		
		String enumClassValue = roleTableAnnotation.getStringValue("enumClass");
		JavaEnum roleEnum;
		try {
			roleEnum = findEnum(loginTableSource, enumClassValue);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		List<EnumConstant<JavaEnum>> enumConstants = roleEnum.getEnumConstants();
		for (EnumConstant<JavaEnum> enumConstant : enumConstants) {
			roleNames.add(enumConstant.getName());
			if(enumConstant.hasAnnotation(LoginRole.class)){
				loginRoles.add(enumConstant.getName());
			}
		}
		map.put("RoleEnum", roleEnum.getName());
		
		map.put("roleNames", roleNames);
		map.put("loginRoles", loginRoles);
		
		map.put("PermissionTable", permissionTableSource.getName());
		Annotation<JavaClass> permissionTableAnnotation = permissionTableSource.getAnnotation(PermissionTable.class);
		map.put("permissionNameField", permissionTableAnnotation.getStringValue("permissionNameField"));
		map.put("permissionActionField", permissionTableAnnotation.getStringValue("permissionActionField"));
		String actionEnumClass = permissionTableAnnotation.getStringValue("actionEnumClass");
		map.put("PermissionActionEnum", StringUtils.substringBefore(actionEnumClass, ".class"));
		
		JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
		DirectoryResource basePackageResource = facet.getBasePackageResource();
		List<JavaResource> targetsToProcess = pluginUtils.calculateTargetToProcess(null, basePackageResource, out);
		if (targetsToProcess.isEmpty()) {
			ShellMessages.info(out,
					"No class with annotation @Entity found to operate on.");
			return;
		}
		Set<String> entities = new HashSet<String>();
		for (JavaResource javaResource : targetsToProcess) {
			JavaSource<?> javaSource = javaResource.getJavaSource();
			entities.add(javaSource.getQualifiedName());
		}
		map.put("jpaEntities", new ArrayList<String>(entities));
		
		String lm = project.getFacet(MetadataFacet.class).getTopLevelPackage() + "." + "lm";
		createFile("org/adorsys/forge/plugins/lm/LoginModule.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/DeclarativeRolesContextListener.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/LoginFailledServlet.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/LoginFormServlet.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/SecurityConstants.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/SimpleGroup.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/SimplePrincipal.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/PermissionQueryEndpoint.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/PingEndpoint.ftl", lm, map);
		createFile("org/adorsys/forge/plugins/lm/LogoutServlet.ftl", lm, map);

		String startup = project.getFacet(MetadataFacet.class).getTopLevelPackage() + "." + "startup";
		createFile("org/adorsys/forge/plugins/startup/InitUserAccountService.ftl", startup, map);

		
		try {
			ResourceFacet resources = project.getFacet(ResourceFacet.class);
			DirectoryResource resourceFolder = resources.getResourceFolder();
			DirectoryResource parent = (DirectoryResource) resourceFolder.getParent();
			File srcMain = parent.getUnderlyingResourceObject();
			File webinf = new File(srcMain, "webapp/WEB-INF/");
			webinf.mkdirs();
			String webxml = processor.processTemplate(map,
					"org/adorsys/forge/plugins/lm/web.xml.ftl");
			IOUtils.write(webxml, new FileOutputStream(new File(webinf, "web.xml")));
			String jbosswebxml = processor.processTemplate(map,
					"org/adorsys/forge/plugins/lm/jboss-web.xml.ftl");
			IOUtils.write(jbosswebxml, new FileOutputStream(new File(webinf, "jboss-web.xml")));
		} catch (IOException ioe){
			throw new IllegalStateException(ioe);
		}
		
	}
	
	private void createFile(String file, String pkg, Map<Object, Object> map){
		String output = processor.processTemplate(map,file);
		JavaClass klass = JavaParser.parse(JavaClass.class, output);
		klass.setPackage(pkg);
		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		try {
			java.saveJavaSource(klass);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}		
	}

	private JavaEnum findEnum(final JavaClass fromClass, final String entity)
			throws FileNotFoundException {
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		// trim the .class ending if any
		String entityName = StringUtils.substringBefore(entity, ".class");
		if(StringUtils.contains(entityName, ".")){// Fully qualified package
			return getJavaEnumFrom(java.getJavaResource(entityName));
		}

		try {
			// try same package
			return getJavaEnumFrom(java.getJavaResource(fromClass.getPackage() + "." +entityName));
		} catch (FileNotFoundException f){
			// get the fully qualified class name.
			List<Import> imports = fromClass.getImports();
			for (Import import1 : imports) {
				if(!StringUtils.equals(entityName, import1.getSimpleName())) continue;
				entityName = import1.getQualifiedName();
				return getJavaEnumFrom(java.getJavaResource(entityName));
			}
			throw f;
		}
		
	}

	private JavaEnum getJavaEnumFrom(final Resource<?> resource)
			throws FileNotFoundException {
		JavaSource<?> source = ((JavaResource) resource).getJavaSource();
		if (!source.isEnum()) {
			throw new IllegalStateException(
					"Current resource is not a JavaEnum!");
		}
		return (JavaEnum) source;
	}
}
