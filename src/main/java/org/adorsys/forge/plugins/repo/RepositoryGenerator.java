package org.adorsys.forge.plugins.repo;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.adorsys.forge.plugins.utils.RepoGeneratedResources;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class RepositoryGenerator {
	@Inject
	private Project project;

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	@Inject
	private Configuration configuration;

	public JavaInterface generateFrom(JavaClass entity, String idType, 
			RepoGeneratedResources repoGeneratedResources, 
			boolean override,
			final PipeOut out) throws FileNotFoundException {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("entity", entity);
		map.put("idType", idType);
		map.put("repoSuffix", getRepoSuffix());
		String output = processor.processTemplate(map,
				"org/adorsys/forge/plugins/repo/Repository.jv");
		JavaInterface repoResource = JavaParser.parse(JavaInterface.class, output);
		repoResource.addImport(entity.getQualifiedName());
		repoResource.setPackage(getPackageName());

		if(!java.getJavaResource(repoResource).exists()){
			repoGeneratedResources.addToRepositories(java
					.saveJavaSource(repoResource));
			ShellMessages.success(out, "Generated repository for ["
					+ entity.getQualifiedName() + "]");
		} else if (override){
			repoGeneratedResources.addToRepositories(java
					.saveJavaSource(repoResource));
			ShellMessages.success(out, "Generated repository for ["
					+ entity.getQualifiedName() + "] ovveriding existing file.");
		} else {
			ShellMessages.info(out, "Aborted repository generation for ["
					+ entity.getQualifiedName() + "] Repository file exists.");
		}
		
		return repoResource;
	}

	private String getPackageName() {
		if (project.hasFacet(RepositoryFacet.class)) {
			String result = configuration.getString(RepositoryFacet.REPO_REPO_CLASS_PACKAGE);
			if(result!=null) return result;
		} 
			return java.getBasePackage() + ".repo";
		
	}
	
	private String getRepoSuffix(){
		if (project.hasFacet(RepositoryFacet.class)) {
			String suffix = configuration
					.getString(RepositoryFacet.REPO_REPO_CLASS_SUFFIX);
			if(suffix!=null) return suffix;
		} 
		return "Repository";
	}
}
