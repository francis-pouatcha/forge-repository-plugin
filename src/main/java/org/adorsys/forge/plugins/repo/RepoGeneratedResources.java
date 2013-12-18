package org.adorsys.forge.plugins.repo;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.resources.java.JavaResource;

public class RepoGeneratedResources {
	private final List<JavaResource> repositories;
	private final List<JavaResource> entities;

	public RepoGeneratedResources() {
		repositories = new ArrayList<JavaResource>();
		entities = new ArrayList<JavaResource>();
	}

	public List<JavaResource> getRepositories() {
		return repositories;
	}

	public List<JavaResource> getEntities() {
		return entities;
	}

	public void addToRepositories(JavaResource repository) {
		repositories.add(repository);
	}

	public void addToEntities(JavaResource repository) {
		entities.add(repository);
	}
}
