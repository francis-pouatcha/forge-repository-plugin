package org.adorsys.forge.plugins.repo;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.PluginUtils;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.ShellMessages;
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
	private PluginUtils pluginUtils;

	@SetupCommand
	public void setup(final PipeOut out) {
		if (!project.hasFacet(RepositoryFacet.class)) {
			request.fire(new InstallFacets(RepositoryFacet.class));
		} else {
			Configuration projectConfiguration = configurationFactory.getProjectConfig(project);
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

			repositoryGenerator.generateFrom(entity, idType, repoGeneratedResources, override, out);
			repoGeneratedResources.addToEntities(jr);
		}
		if (!repoGeneratedResources.getEntities().isEmpty()) {
			generatedEvent.fire(repoGeneratedResources);
		}
	}

}
