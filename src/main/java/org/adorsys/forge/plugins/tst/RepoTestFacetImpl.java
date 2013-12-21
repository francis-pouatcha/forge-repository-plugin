package org.adorsys.forge.plugins.tst;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.BaseJavaEEFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyInstaller;

public class RepoTestFacetImpl extends BaseJavaEEFacet implements
		RepoTestFacet {

	@Inject
	public RepoTestFacetImpl(DependencyInstaller installer) {
		super(installer);
	}

	@Override
	protected List<Dependency> getRequiredDependencies() {
		return Collections.emptyList();
	}
}
