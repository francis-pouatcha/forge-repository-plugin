package org.adorsys.forge.plugins.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.BaseJavaEEFacet;
import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.ShellPrompt;

public class RepositoryFacetImpl extends BaseJavaEEFacet implements
		RepositoryFacet {

	@Inject
	private ShellPrompt prompt;

	@Inject
	private ConfigurationFactory configurationFactory;
	
	@Inject
	FreemarkerTemplateProcessor processor;	

	
	@Inject
	public RepositoryFacetImpl(final DependencyInstaller installer) {
		super(installer);
	}

	@Override
	protected List<Dependency> getRequiredDependencies() {
		return Arrays.asList(
					(Dependency) DependencyBuilder.create()
				.setGroupId("org.apache.deltaspike.modules")
				.setArtifactId("deltaspike-data-module-api")
				.setVersion("0.5").setScopeType(ScopeType.COMPILE),
				(Dependency) DependencyBuilder.create()
					.setGroupId("org.apache.deltaspike.modules")
					.setArtifactId("deltaspike-data-module-impl")
					.setVersion("0.5").setScopeType(ScopeType.COMPILE),
				(Dependency) DependencyBuilder.create()
					.setGroupId("org.apache.commons")
					.setArtifactId("commons-lang3")
					.setVersion("3.1").setScopeType(ScopeType.COMPILE),
				(Dependency) DependencyBuilder.create()
				.setGroupId("javax.enterprise")
				.setArtifactId("cdi-api")
				.setScopeType(ScopeType.PROVIDED),					
				(Dependency) DependencyBuilder.create()
				.setGroupId("org.hibernate")
				.setArtifactId("hibernate-jpamodelgen")
				.setVersion("1.3.0.Final")
				.setScopeType(ScopeType.PROVIDED)					
				);
	}
	
	@Override
	public boolean isInstalled() {
		boolean installed = super.isInstalled();
		if(!installed) return false;

		// Make sure cofiguration param are available.
		Configuration projectConfiguration = configurationFactory.getProjectConfig(project);
		Object pkg = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
		Object suffix = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
		return pkg != null && suffix != null;
	}

	@Override
	public boolean install() {

		if (!project.hasFacet(RepositoryFacet.class)) {
			DependencyFacet deps = project.getFacet(DependencyFacet.class);
			
			if (!deps.hasDirectManagedDependency(JAVAEE6)) {
				getInstaller().installManaged(project, JAVAEE6);
			}
	
			for (Dependency requirement : getRequiredDependencies()) {
				if (!deps.hasDirectDependency(requirement)) {
					getInstaller().install(project, requirement);
				}
			}
		}
		
		Configuration projectConfiguration = configurationFactory.getProjectConfig(project);
		if(projectConfiguration.getString(RepositoryFacet.REPO_REPO_CLASS_PACKAGE)==null){
			String pkg = prompt.promptCommon(
					"In what package do you want to store the Repository class?",
					PromptType.JAVA_PACKAGE, project.getFacet(MetadataFacet.class)
							.getTopLevelPackage() + ".repo");
			projectConfiguration.setProperty(
					RepositoryFacet.REPO_REPO_CLASS_PACKAGE, pkg);
		}
		
		if(projectConfiguration.getString(RepositoryFacet.REPO_REPO_CLASS_SUFFIX)==null){
			String repoSuffix = prompt.prompt(
					"How do you want to name the Repository suffix?", "Repository");
			projectConfiguration.setProperty(
					RepositoryFacet.REPO_REPO_CLASS_SUFFIX, repoSuffix);
		}
		Map<Object, Object> map = new HashMap<Object, Object>();
		MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
		map.put("topPackage", metadataFacet.getTopLevelPackage());
		map.put("projectName", StringUtils.substringBefore(metadataFacet.getProjectName(), "."));
		
		createFile("org/adorsys/forge/plugins/repo/DataSourceProducer.jv", projectConfiguration.getString(RepositoryFacet.REPO_REPO_CLASS_PACKAGE), map);
//
//
//
		String beansXMLRelativeFileName = "META-INF" + File.separator + "beans.xml";
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		FileResource<?> beansXmlFile = (FileResource<?>) resources.getResourceFolder().getChild(beansXMLRelativeFileName);
		if(!beansXmlFile.exists()){
			String output = processor.processTemplate(map,
					"org/adorsys/forge/plugins/rest/beans.xml.jv");
			resources.createResource(output.toCharArray(), beansXMLRelativeFileName);
		}
		return super.install();
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

}
