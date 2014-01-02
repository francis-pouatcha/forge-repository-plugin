package org.adorsys.forge.plugins.rest;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlRootElement;

import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.adorsys.forge.plugins.utils.PluginUtils;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class EntityBasedResourceGenerator {

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	@Inject
	private PluginUtils pluginUtils;

	@Inject
	private EntitySearchInputGenerator searchInputGenerator;

	public JavaClass generateFrom(JavaClass entity, String idType,
			String contentType, JavaInterface repoResource,
			RepoGeneratedResources repoGeneratedResources, boolean override,
			final PipeOut out) throws FileNotFoundException {
		if (!entity.hasAnnotation(XmlRootElement.class)) {
			entity.addAnnotation(XmlRootElement.class);
			java.saveJavaSource(entity);
		}

		JavaClass entitySearch = searchInputGenerator.generateFrom(entity,
				repoGeneratedResources, override, out);

		// String persistenceUnitName = utility.getPersistenceUnitName();
		String entityTable = pluginUtils.getEntityTable(entity);
		String entityEndpointName = getEntityEndpointName(entityTable);
		String resourcePath = pluginUtils.getResourcePath(entityTable);
	    String idGetterName = pluginUtils.resolveIdGetterName(entity);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("resourcePath", resourcePath);
		map.put("entityEndpointName", entityEndpointName);
		map.put("entityRepoName", repoResource.getName());
		map.put("contentType", contentType);
		map.put("entityName", entity.getName());
		map.put("entity", entity);
		map.put("entitySearchName", entitySearch.getName());
		map.put("idType", idType);
	    map.put("getIdStatement", idGetterName);

		String output = processor.processTemplate(map,
				"org/adorsys/forge/plugins/rest/Endpoint.jv");
		JavaClass restResource = JavaParser.parse(JavaClass.class, output);
		restResource.addImport(entity.getQualifiedName());
		/*
		 * Add the jpa modelgen of this entity.
		 */
		restResource.addImport(entity.getQualifiedName()+"_");
		restResource.addImport(entitySearch.getQualifiedName());
		restResource.addImport(repoResource.getQualifiedName());
		restResource.setPackage(pluginUtils.getRestPackageName());

		if(!java.getJavaResource(restResource).exists()){
			repoGeneratedResources.addToEndpoints(java
					.saveJavaSource(restResource));
			ShellMessages.success(out, "Generated rest endpoint for ["
					+ entity.getQualifiedName() + "]");
		} else if (override){
			repoGeneratedResources.addToEndpoints(java
					.saveJavaSource(restResource));
			ShellMessages.success(out, "Generated rest endpoint for ["
					+ entity.getQualifiedName() + "] overiding existing file.");
		} else {
			ShellMessages.info(out, "Aborted rest endpoint generation for ["
					+ entity.getQualifiedName() + "] Rest endpoint File exists.");
		}
		
		return restResource;
	}

	private String getEntityEndpointName(String entityTable) {
		return entityTable + "Endpoint";
	}

}
