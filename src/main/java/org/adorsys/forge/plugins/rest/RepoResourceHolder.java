package org.adorsys.forge.plugins.rest;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.resources.java.JavaResource;

public class RepoResourceHolder {

	private final JavaInterface repositoryClass;
	
	private final JavaClass entityClass;
	
	private final JavaResource repoResource;

	public RepoResourceHolder(JavaInterface repositoryClass,
			JavaClass entityClass, JavaResource repoResource) {
		super();
		this.repositoryClass = repositoryClass;
		this.entityClass = entityClass;
		this.repoResource = repoResource;
	}

	public JavaInterface getRepositoryClass() {
		return repositoryClass;
	}

	public JavaClass getEntityClass() {
		return entityClass;
	}

	public JavaResource getRepoResource() {
		return repoResource;
	}
}
