package org.adorsys.forge.plugins.tst;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.resources.java.JavaResource;

public class GeneratedResources {
	private final List<JavaResource> testClasses;

	public GeneratedResources() {
		testClasses = new ArrayList<JavaResource>();
	}

	public List<JavaResource> getTestClasses() {
		return testClasses;
	}

	public void addToTestClasses(JavaResource repository) {
		testClasses.add(repository);
	}
}
