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
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.spec.javaee.EJBFacet;
import org.jboss.forge.spec.javaee.JTAFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 *
 */
@Alias("repo")
@RequiresProject
public class RepositoryPlugin implements Plugin {
	@Inject
	private ShellPrompt prompt;

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

	@SetupCommand
	public void setup(final PipeOut out) {
		if (!project.hasFacet(RepositoryFacet.class)) {
			request.fire(new InstallFacets(RepositoryFacet.class));
		} else {
			Configuration projectConfiguration = getProjectConfiguration();
			Object pkg = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
			Object suffix = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
			if (pkg == null || suffix == null) {
				request.fire(new InstallFacets(RepositoryFacet.class));
			}
		}
		
		if (project.hasFacet(RepositoryFacet.class)) {
//			Configuration projectConfiguration = getProjectConfiguration();
//			Object pkg = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
//			Object suffix = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
//			if (pkg != null && suffix != null) {
				ShellMessages.success(out, "Repository service installed.");
			} else {
				ShellMessages.error(out, "Repository service could not be installed.");
			}
//		}
	}

	@SuppressWarnings("unchecked")
	@Command(value = "new-repository", help = "creates a new Repository for an existing @Entity object.")
	public void newRepository(final PipeOut out,
			@Option(required = false) JavaResource... targets)
			throws FileNotFoundException {
		/*
		 * Make sure we have all the features we need for this to work.
		 */
		if (!project.hasAllFacets(Arrays.asList(EJBFacet.class,
				PersistenceFacet.class))) {
			request.fire(new InstallFacets(true, JTAFacet.class,
					EJBFacet.class, PersistenceFacet.class));
		}

		if (((targets == null) || (targets.length < 1))
				&& (currentResource instanceof JavaResource)) {
			targets = new JavaResource[] { (JavaResource) currentResource };
		}

		List<JavaResource> javaTargets = selectTargets(out, targets);
		if (javaTargets.isEmpty()) {
			ShellMessages.error(out,
					"Must specify a domain @Entity on which to operate.");
			return;
		}

		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		RepoGeneratedResources repoGeneratedResources = new RepoGeneratedResources();
		for (JavaResource jr : javaTargets) {

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

			if (!java.getJavaResource(resource).exists()
					|| prompt.promptBoolean("Repository ["
							+ resource.getQualifiedName()
							+ "] already, exists. Overwrite?")) {
				repoGeneratedResources.addToRepositories(java
						.saveJavaSource(resource));
				ShellMessages.success(out, "Generated repository for ["
						+ entity.getQualifiedName() + "]");

			} else
				ShellMessages.info(out, "Aborted repository generation for ["
						+ entity.getQualifiedName() + "]");
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

	private List<JavaResource> selectTargets(final PipeOut out,
			Resource<?>[] targets) throws FileNotFoundException {
		List<JavaResource> results = new ArrayList<JavaResource>();
		if (targets == null) {
			targets = new Resource<?>[] {};
		}
		for (Resource<?> r : targets) {
			if (r instanceof JavaResource) {
				JavaSource<?> entity = ((JavaResource) r).getJavaSource();
				if (entity instanceof JavaClass) {
					if (entity.hasAnnotation(Entity.class)) {
						results.add((JavaResource) r);
					} else {
						displaySkippingResourceMsg(out, entity);
					}
				} else {
					displaySkippingResourceMsg(out, entity);
				}
			}
		}
		return results;
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
