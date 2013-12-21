package org.adorsys.forge.plugins.repo;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Member;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.java.util.Types;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.EJBFacet;
import org.jboss.forge.spec.javaee.JTAFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 *
 */
@Alias("repogen")
@RequiresProject
@RequiresFacet({ RepositoryFacet.class, JavaSourceFacet.class })
@Help("This plugin generates jpa repository classes for jap entities.")
public class RepositoryPlugin implements Plugin {
//	@Inject
//	private ShellPrompt prompt;

	@Inject
	private Project project;

	@Inject
	private Event<InstallFacets> request;

	@Inject
	@Current
	private Resource<?> currentResource;

	@Inject
	private RepositoryGenerator repositoryGenerator;

	@Inject
	private Event<RepoGeneratedResources> generatedEvent;

	@Inject
	private ConfigurationFactory configurationFactory;
	
	@Inject
	JavaSourceFacet java;	

	@SetupCommand
	public void setup(final PipeOut out) {
		if (!project.hasFacet(RepositoryFacet.class)) {
			request.fire(new InstallFacets(RepositoryFacet.class));
		} else {
			Configuration projectConfiguration = getProjectConfiguration();
			Object pkg = projectConfiguration
					.getProperty(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
			Object suffix = projectConfiguration
					.getProperty(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
			if (pkg == null || suffix == null) {
				request.fire(new InstallFacets(RepositoryFacet.class));
			}
		}

		if (project.hasFacet(RepositoryFacet.class)) {
			ShellMessages.success(out, "Repository service installed.");
		} else {
			ShellMessages.error(out,
					"Repository service could not be installed.");
		}
	}

	@SuppressWarnings("unchecked")
	@Command(value = "new-repository", help = "creates a new Repository for an existing @Entity object.")
	public void newRepository(@Option(name = "jpaClass", required = false, type = PromptType.JAVA_CLASS) JavaResource jpaClass,
			@Option(name = "jpaPackage", required = false, type = PromptType.FILE_PATH) Resource<?> jpaPackage,
			@Option(name="overrride", flagOnly=true, required=false ) boolean override,
			final PipeOut out)
			throws FileNotFoundException {
		/*
		 * Make sure we have all the features we need for this to work.
		 */
		if (!project.hasAllFacets(Arrays.asList(EJBFacet.class,
				PersistenceFacet.class))) {
			request.fire(new InstallFacets(true, JTAFacet.class,
					EJBFacet.class, PersistenceFacet.class));
		}
		
		List<JavaResource> targetsToProcess = new ArrayList<JavaResource>();
		
		if(jpaClass!=null){
			targetsToProcess.add(jpaClass);
		}
		
		if(jpaPackage!=null){
			if(jpaPackage instanceof JavaResource){
				JavaResource jr = (JavaResource) jpaPackage;
				selectTargets(out, targetsToProcess, jr);
			} else if (jpaPackage instanceof DirectoryResource){
				DirectoryResource dr = (DirectoryResource) jpaPackage;
				selectTargets(out, targetsToProcess, dr);
			} else {
				ShellMessages.warn(out,
						"Provided class is neiter a java package nor a java file");
			}
		}
		if(jpaClass==null && jpaPackage==null && currentResource!=null){
			if(currentResource instanceof JavaResource){
				JavaResource jr = (JavaResource) jpaPackage;
				selectTargets(out, targetsToProcess, jr);
			} else if (currentResource instanceof DirectoryResource){
				DirectoryResource dr = (DirectoryResource) jpaPackage;
				selectTargets(out, targetsToProcess, dr);				
			} else {
				ShellMessages.warn(out,
						"Current resource is neiter a java package nor a java file");
			}
		}

		if (targetsToProcess.isEmpty()) {
			ShellMessages.info(out,
					"No class with annotation @Entity found to operate on.");
			return;
		}

		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		RepoGeneratedResources repoGeneratedResources = new RepoGeneratedResources();
		for (JavaResource jr : targetsToProcess) {

			JavaClass entity = (JavaClass) (jr).getJavaSource();

			String idType = resolveIdType(entity);
			if (!Types.isBasicType(idType)) {
				ShellMessages
						.error(out,
								"Skipped class ["
										+ entity.getQualifiedName()
										+ "] because @Id type ["
										+ idType
										+ "] is not supported by repository generation.");
				continue;
			}

			JavaInterface resource = repositoryGenerator.generateFrom(entity,
					idType);

			repoGeneratedResources.addToEntities(jr);

			if(!java.getJavaResource(resource).exists()){
				repoGeneratedResources.addToRepositories(java
						.saveJavaSource(resource));
				ShellMessages.success(out, "Generated repository for ["
						+ entity.getQualifiedName() + "]");
			} else if (override){
				repoGeneratedResources.addToRepositories(java
						.saveJavaSource(resource));
				ShellMessages.success(out, "Generated repository for ["
						+ entity.getQualifiedName() + "] ovveriding existing file.");
			} else {
				ShellMessages.info(out, "Aborted repository generation for ["
						+ entity.getQualifiedName() + "] Repository file exists.");
			}
		}
		if (!repoGeneratedResources.getEntities().isEmpty()) {
			generatedEvent.fire(repoGeneratedResources);
		}
	}

	private String resolveIdType(JavaClass entity) {
		for (Member<JavaClass, ?> member : entity.getMembers()) {
			if (member.hasAnnotation(Id.class)) {
				if (member instanceof Method) {
					return ((Method<?>) member).getReturnType();
				}
				if (member instanceof Field) {
					return ((Field<?>) member).getType();
				}
			}
		}
		return "Object";
	}

	private void selectTargets(final PipeOut out,List<JavaResource> ressourcesToProcess,
			JavaResource javaResource) throws FileNotFoundException {
		
		if(ressourcesToProcess.contains(javaResource)) return;
		
		JavaSource<?> entity = javaResource.getJavaSource();
		if (entity instanceof JavaClass) {
			if (entity.hasAnnotation(Entity.class)) {
				ressourcesToProcess.add(javaResource);
			} else {
				displaySkippingResourceMsg(out, entity);
			}
		} else {
			displaySkippingResourceMsg(out, entity);
		}
	}

	private void selectTargets(final PipeOut out,List<JavaResource> ressourcesToProcess,
			DirectoryResource directoryResource) throws FileNotFoundException {
		
		DirectoryResource sourceFolder = java.getSourceFolder();
		if(!directoryResource.getFullyQualifiedName().startsWith(sourceFolder.getFullyQualifiedName())){
			ShellMessages.warn(out, "selected directory is not in the source folder. Will not be processed. ["
					+ directoryResource.getFullyQualifiedName() + "]");
			return;
		}
		List<Resource<?>> javaResources = directoryResource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof JavaResource;
					}
				});
		if (javaResources != null) {
			for (Resource<?> resource2 : javaResources) {
				JavaResource jr = (JavaResource) resource2;
				selectTargets(out, ressourcesToProcess, jr);
			}
		}
		List<Resource<?>> dirResources = directoryResource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof DirectoryResource;
					}
				});
		for (Resource<?> resource2 : dirResources) {
			DirectoryResource dr = (DirectoryResource) resource2;
			selectTargets(out, ressourcesToProcess, dr);
		}
	}

	private void displaySkippingResourceMsg(final PipeOut out,
			final JavaSource<?> entity) {
		if (!out.isPiped()) {
			ShellMessages.info(out, "Skipped non-@Entity Java resource ["
					+ entity.getQualifiedName() + "]");
		}
	}

	private Configuration configuration;

	private Configuration getProjectConfiguration() {
		if (this.configuration == null) {
			this.configuration = configurationFactory.getProjectConfig(project);
		}
		return this.configuration;
	}

}
