package org.adorsys.forge.plugins.repo;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;

public class RepositoryGenerator {
	@Inject
	private Project project;

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	@Inject
	private Configuration configuration;

	public JavaInterface generateFrom(JavaClass entity, String idType) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("entity", entity);
		map.put("idType", idType);
		map.put("repoPrefix", getRepoPrefix());
		String output = processor.processTemplate(map,
				"org/adorsys/forge/plugins/repo/Repository.jv");
		JavaInterface resource = JavaParser.parse(JavaInterface.class, output);
		resource.addImport(entity.getQualifiedName());
		resource.setPackage(getPackageName());
		return resource;
	}

	private String getPackageName() {
		if (project.hasFacet(RepositoryFacet.class)) {
			return configuration
					.getString(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
		} else {
			return java.getBasePackage() + ".repo";
		}
	}
	
	private String getRepoPrefix(){
		if (project.hasFacet(RepositoryFacet.class)) {
			return configuration
					.getString(RepositoryFacet.REPO_REPO_CLASS_PREFIX);
		} else {
			return "Repository";
		}
	}
}
