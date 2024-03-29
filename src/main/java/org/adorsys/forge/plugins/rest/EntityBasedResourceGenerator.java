package org.adorsys.forge.plugins.rest;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlRootElement;

import org.adorsys.forge.plugins.repo.RepositoryFacet;
import org.adorsys.forge.plugins.utils.EntityInfo;
import org.adorsys.forge.plugins.utils.FieldInfo;
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
		}

		JavaClass entitySearch = searchInputGenerator.generateFrom(entity,
				repoGeneratedResources, "org/adorsys/forge/plugins/jpa/SearchInput.jv", override, out);
		JavaClass entitySearchResult = searchInputGenerator.generateFrom(entity,
				repoGeneratedResources, "org/adorsys/forge/plugins/jpa/SearchResult.jv", override, out);

		EntityInfo entityInfo = pluginUtils.inspectEntity(entity);
		String entityTable = pluginUtils.getEntityTable(entity);
		String entityEndpointName = pluginUtils.getEntityEndpointName(entity);
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
	    map.put("entityInfo", entityInfo);

		String mergerOutput = processor.processTemplate(map,
				"org/adorsys/forge/plugins/rest/Merger.jv");
		JavaClass mergerResource = JavaParser.parse(JavaClass.class, mergerOutput);
		mergerResource.addImport(entity.getQualifiedName());
		mergerResource.addImport(repoResource.getQualifiedName());
		mergerResource.setPackage(pluginUtils.getRestPackageName());
		repoGeneratedResources.addToOthers(java.saveJavaSource(mergerResource));
	    
		String restOutput = processor.processTemplate(map,
				"org/adorsys/forge/plugins/rest/Endpoint.ftl");
		JavaClass restResource = JavaParser.parse(JavaClass.class, restOutput);
		String ejbOutput = processor.processTemplate(map,
				"org/adorsys/forge/plugins/rest/EJB.ftl");
		JavaClass ejbResource = JavaParser.parse(JavaClass.class, ejbOutput);
		restResource.addImport(entity.getQualifiedName());
		ejbResource.addImport(entity.getQualifiedName());
		/*
		 * Add the jpa modelgen of this entity.
		 */
		restResource.addImport(entity.getQualifiedName()+"_");
		restResource.addImport(entitySearch.getQualifiedName());
		restResource.addImport(entitySearchResult.getQualifiedName());
		ejbResource.addImport(repoResource.getQualifiedName());
		restResource.setPackage(pluginUtils.getRestPackageName());
		ejbResource.setPackage(pluginUtils.getRestPackageName());
		List<FieldInfo> composedCollections = entityInfo.getComposedCollections();
		if(!composedCollections.isEmpty()){
			ejbResource.addImport(Set.class.getName());
		}
		for (FieldInfo fieldInfo : composedCollections) {
			if(fieldInfo.getTargetEntity()!=null && !fieldInfo.getTargetEntity().getName().equals(entityInfo.getEntity().getQualifiedName())){
				ejbResource.addImport(fieldInfo.getTargetEntity().getQualifiedName());
			}
		}
		
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

		if(!java.getJavaResource(ejbResource).exists()){
			repoGeneratedResources.addToOthers(java
					.saveJavaSource(ejbResource));
			ShellMessages.success(out, "Generated ejb for ["
					+ entity.getQualifiedName() + "]");
		} else if (override){
			repoGeneratedResources.addToOthers(java
					.saveJavaSource(ejbResource));
			ShellMessages.success(out, "Generated ejb for ["
					+ entity.getQualifiedName() + "] overiding existing file.");
		} else {
			ShellMessages.info(out, "Aborted ejb generation for ["
					+ entity.getQualifiedName() + "] EJB File exists.");
		}
		
		return restResource;
	}
	
	public void writeMergerUtils(){
		Map<Object, Object> map = new HashMap<Object, Object>();
		String output = processor.processTemplate(map,
				"org/adorsys/forge/plugins/rest/MergerUtils.ftl");
		JavaClass mergerUtils = JavaParser.parse(JavaClass.class, output);
		mergerUtils.setPackage(pluginUtils.getRestPackageName());
		try {
			java.saveJavaSource(mergerUtils);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

}
