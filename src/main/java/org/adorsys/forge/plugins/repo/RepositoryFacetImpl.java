package org.adorsys.forge.plugins.repo;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

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
	public RepositoryFacetImpl(DependencyInstaller installer) {
		super(installer);
	}

	@Override
	protected List<Dependency> getRequiredDependencies() {
		return Arrays
				.asList((Dependency) DependencyBuilder
						.create("org.apache.deltaspike.modules:deltaspike-data-module-api:0.5:compile"),
						(Dependency) DependencyBuilder
								.create("org.apache.deltaspike.modules:deltaspike-data-module-impl:0.5:runtime"));

	}

	@Override
	public boolean install() {
		for (Dependency requirement : getRequiredDependencies()) {
			if (!getInstaller().isInstalled(project, requirement)) {
				DependencyFacet deps = project.getFacet(DependencyFacet.class);
				if (!deps.hasEffectiveManagedDependency(requirement)
						&& !deps.hasDirectManagedDependency(JAVAEE6)) {
					getInstaller().installManaged(project, JAVAEE6);
				}
				getInstaller()
						.install(project, requirement, ScopeType.PROVIDED);
			}
		}
		Configuration projectConfiguration = getProjectConfiguration();
		String pkg = prompt.promptCommon(
				"In what package do you want to store the Repository class?",
				PromptType.JAVA_PACKAGE, project.getFacet(MetadataFacet.class)
						.getTopLevelPackage() + ".repo");

		String repoPrefix = prompt.prompt(
				"How do you want to name the Repository prefix?", "Repository");
		projectConfiguration.setProperty(
				RepositoryFacet.REPO_REPO_CLASS_PACKAGE, pkg);
		projectConfiguration.setProperty(
				RepositoryFacet.REPO_REPO_CLASS_PREFIX, repoPrefix);
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
// project add-dependency
// org.hibernate:hibernate-jpamodelgen:1.3.0.Final:provided;
// project add-managed-dependency
// org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-bom:2.0.1:import:pom;
// project add-dependency
// org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-depchain:2.0.1:test:pom;
// project add-dependency ;
// project add-dependency ;
