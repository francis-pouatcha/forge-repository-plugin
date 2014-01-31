package org.adorsys.forge.plugins.tst;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.BaseJavaEEFacet;
import org.apache.maven.model.Model;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.packaging.PackagingType;

public class RepoTestFacetImpl extends BaseJavaEEFacet implements RepoTestFacet {

	@Inject
	public RepoTestFacetImpl(DependencyInstaller installer) {
		super(installer);
	}

	@Override
	protected List<Dependency> getRequiredDependencies() {
		return Arrays.asList(
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.resteasy")
				.setArtifactId("resteasy-client")
				.setVersion("3.0.6.Final")
				.setScopeType(ScopeType.PROVIDED),
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.resteasy")
				.setArtifactId("resteasy-jackson-provider")
				.setVersion("3.0.6.Final")
				.setScopeType(ScopeType.PROVIDED),
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.resteasy")
				.setArtifactId("resteasy-jaxb-provider")
				.setVersion("3.0.6.Final")
				.setScopeType(ScopeType.PROVIDED),
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.as")
				.setArtifactId("jboss-as-arquillian-container-remote")
				.setVersion("7.2.0.Final")
				.setScopeType(ScopeType.TEST),
				(Dependency)DependencyBuilder.create()
				.setGroupId("junit")
				.setArtifactId("junit")
				.setVersion("4.11").setScopeType(ScopeType.TEST),
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.shrinkwrap.descriptors")
				.setArtifactId("shrinkwrap-descriptors-api-javaee")
				.setScopeType(ScopeType.TEST),
				(Dependency)DependencyBuilder.create()
				.setGroupId("org.jboss.shrinkwrap.descriptors")
				.setArtifactId("shrinkwrap-descriptors-impl-javaee")
				.setScopeType(ScopeType.TEST)		
				);
	}

	@Override
	public boolean install() {
		if (!project.hasFacet(RepoTestFacet.class)) {
			DependencyFacet deps = project.getFacet(DependencyFacet.class);
			
			if (!deps.hasDirectManagedDependency(JAVAEE6)) {
				getInstaller().installManaged(project, JAVAEE6);
			}
	
			getInstaller().installManaged(project, DependencyBuilder.create()
			.setGroupId("org.jboss.shrinkwrap.resolver")
			.setArtifactId("shrinkwrap-resolver-bom")
			.setVersion("2.0.1").setScopeType(ScopeType.IMPORT)
			.setPackagingType(PackagingType.BASIC));

			getInstaller().install(project, DependencyBuilder.create()
			.setGroupId("org.jboss.shrinkwrap.resolver")
			.setArtifactId("shrinkwrap-resolver-depchain")
			.setScopeType(ScopeType.TEST)
			.setPackagingType(PackagingType.BASIC));

			getInstaller().installManaged(project, DependencyBuilder.create()
			.setGroupId("org.jboss.arquillian")
			.setArtifactId("arquillian-bom")
			.setVersion("1.1.2.Final").setScopeType(ScopeType.IMPORT)
			.setPackagingType(PackagingType.BASIC));

			deps.removeDependency(DependencyBuilder.create()
			.setGroupId("org.jboss.spec.javax.ws.rs")
			.setArtifactId("jboss-jaxrs-api_1.1_spec")
			.setScopeType(ScopeType.PROVIDED));
			
			getInstaller().install(project, DependencyBuilder.create()
				.setGroupId("org.jboss.resteasy")
				.setArtifactId("resteasy-client")
				.setVersion("3.0.6.Final")
				.setScopeType(ScopeType.PROVIDED));
			
			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.jboss.resteasy")
					.setArtifactId("resteasy-jackson-provider")
					.setVersion("3.0.6.Final")
					.setScopeType(ScopeType.PROVIDED));

			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.jboss.resteasy")
					.setArtifactId("resteasy-jaxb-provider")
					.setVersion("3.0.6.Final")
					.setScopeType(ScopeType.PROVIDED));

			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.jboss.as")
					.setArtifactId("jboss-as-arquillian-container-remote")
					.setVersion("7.2.0.Final")
					.setScopeType(ScopeType.TEST));

			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.apache.commons")
					.setArtifactId("commons-lang3")
					.setVersion("3.1")
					.setScopeType(ScopeType.TEST));
			
			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("junit")
					.setArtifactId("junit")
					.setVersion("4.11").setScopeType(ScopeType.TEST));
			
			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.jboss.shrinkwrap.descriptors")
					.setArtifactId("shrinkwrap-descriptors-api-javaee")
					.setScopeType(ScopeType.TEST));
			
			getInstaller().install(project, DependencyBuilder.create()
					.setGroupId("org.jboss.shrinkwrap.descriptors")
					.setArtifactId("shrinkwrap-descriptors-impl-javaee")
					.setScopeType(ScopeType.TEST));		
		}
		
		return super.install();
	}
	
	
}
