package org.adorsys.forge.plugins.rest;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class EntitySearchInputGenerator {

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	public JavaClass generateFrom(JavaClass entity,
			RepoGeneratedResources repoGeneratedResources, 
			String fileName,
			boolean override,
			final PipeOut out) throws FileNotFoundException 
	{
	
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("entityName", entity.getName());
		String output = processor.processTemplate(map,fileName);
		JavaClass entitySearch = JavaParser.parse(JavaClass.class, output);
		entitySearch.setPackage(entity.getPackage());

		if(!java.getJavaResource(entitySearch).exists()){
			repoGeneratedResources.addToOthers(java
					.saveJavaSource(entitySearch));
			ShellMessages.success(out, "Generated search input class for ["
					+ entity.getQualifiedName() + "]");
		} else if (override){
			repoGeneratedResources.addToEndpoints(java
					.saveJavaSource(entitySearch));
			ShellMessages.success(out, "Generated search input class for ["
					+ entity.getQualifiedName() + "] overiding existing file.");
		} else {
			ShellMessages.info(out, "Aborted rest endpoint generation for ["
					+ entity.getQualifiedName() + "] Rest endpoint File exists.");
		}
		return entitySearch;
	}
}
