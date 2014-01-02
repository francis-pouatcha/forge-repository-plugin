package org.adorsys.forge.plugins.rest;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.adorsys.forge.plugins.repo.RepositoryFacet;
import org.adorsys.forge.plugins.repo.RepositoryGenerator;
import org.adorsys.forge.plugins.utils.ContentTypeCompleter;
import org.adorsys.forge.plugins.utils.PluginUtils;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.events.InstallFacets;
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

	@SetupCommand
	public void setup(
			@Option(name = "activatorType", defaultValue = "WEB_XML") RestActivatorType activatorType,
			final PipeOut out) 
	{
		if (!project.hasFacet(RepositoryFacet.class)) {
//			try {
//				shell.execute("repogen setup");
//			} catch (Exception e) {
//				throw new IllegalStateException();
//			}
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
}
