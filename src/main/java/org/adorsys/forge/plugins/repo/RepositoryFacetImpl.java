package org.adorsys.forge.plugins.repo;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.BaseJavaEEFacet;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.ShellPrompt;

public class RepositoryFacetImpl extends BaseJavaEEFacet implements
		RepositoryFacet {

	@Inject
	private ShellPrompt prompt;

	@Inject
	private ConfigurationFactory configurationFactory;

	// Do not refer this field directly. Use the getProjectConfiguration()
	// method instead.
	private Configuration configuration;

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
				.setVersion("0.5"),
				(Dependency) DependencyBuilder.create()
					.setGroupId("org.apache.deltaspike.modules")
					.setArtifactId("deltaspike-data-module-impl")
					.setVersion("0.5"));

	}

	
	@Override
	public boolean isInstalled() {
		boolean installed = super.isInstalled();
		if(!installed) return false;

		// Make sure cofiguration param are available.
		Configuration projectConfiguration = getProjectConfiguration();
		Object pkg = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
		Object suffix = projectConfiguration.getProperty(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
		return pkg != null && suffix != null;
	}

	@Override
	public boolean install() {
		if (!project.hasFacet(RepositoryFacet.class)) {
	
			for (Dependency requirement : getRequiredDependencies()) {
				if (!getInstaller().isInstalled(project, requirement)) {
					DependencyFacet deps = project.getFacet(DependencyFacet.class);
					if (!deps.hasEffectiveManagedDependency(requirement)
							&& !deps.hasDirectManagedDependency(JAVAEE6)) {
						getInstaller().installManaged(project, JAVAEE6);
					}
					getInstaller()
							.install(project, requirement, ScopeType.COMPILE);
				}
			}
		}
		
		Configuration projectConfiguration = getProjectConfiguration();
		String pkg = prompt.promptCommon(
				"In what package do you want to store the Repository class?",
				PromptType.JAVA_PACKAGE, project.getFacet(MetadataFacet.class)
						.getTopLevelPackage() + ".repo");

		String repoSuffix = prompt.prompt(
				"How do you want to name the Repository suffix?", "Repository");
		projectConfiguration.setProperty(
				RepositoryFacet.REPO_REPO_CLASS_PACKAGE, pkg);
		projectConfiguration.setProperty(
				RepositoryFacet.REPO_REPO_CLASS_SUFFIX, repoSuffix);
		return super.install();
	}

	/**
	 * Important: Use this method always to obtain the configuration. Do not
	 * invoke this inside a constructor since the returned {@link Configuration}
	 * instance would not be the project scoped one.
	 * 
	 * @return The project scoped {@link Configuration} instance
	 */
	private Configuration getProjectConfiguration() {
		if (this.configuration == null) {
			this.configuration = configurationFactory.getProjectConfig(project);
		}
		return this.configuration;
	}

}
