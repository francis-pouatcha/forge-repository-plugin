package org.adorsys.forge.plugins.utils;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.resources.java.JavaResource;

public class RepoGeneratedResources {
	private final List<JavaResource> repositories = new ArrayList<JavaResource>();
	private final List<JavaResource> entities = new ArrayList<JavaResource>();
	private final List<JavaResource> endpoints = new ArrayList<JavaResource>();
	private final List<JavaResource> others = new ArrayList<JavaResource>();
	public List<JavaResource> getRepositories() {
		return repositories;
	}
	public List<JavaResource> getEntities() {
		return entities;
	}
	public List<JavaResource> getEndpoints() {
		return endpoints;
	}
	public List<JavaResource> getOthers() {
		return others;
	}
	
	public void addToRepositories(JavaResource jr) {
		repositories.add(jr);
	}
	public void addToEntities(JavaResource jr) {
		entities.add(jr);
	}
	public void addToEndpoints(JavaResource jr) {
		endpoints.add(jr);
	}
	public void addToOthers(JavaResource jr) {
		others.add(jr);
	}
	
}
