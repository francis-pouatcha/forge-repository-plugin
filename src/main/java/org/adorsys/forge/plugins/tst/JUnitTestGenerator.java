package org.adorsys.forge.plugins.tst;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.apache.deltaspike.data.api.Repository;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Import;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.PipeOut;

public class JUnitTestGenerator {

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	public JavaClass generateFrom(JavaSource<?> sourceOfClassToTest, final PipeOut out) {
//		String repositoryAnnotation = "org.apache.deltaspike.data.api.Repository";
		if(!(sourceOfClassToTest.hasAnnotation(Repository.class) || sourceOfClassToTest.hasAnnotation(Stateless.class))){
			ShellMessages.info(out,
					"Specified package " + sourceOfClassToTest.getQualifiedName()
					+ " does not have the annotation "+Repository.class.getName()+". No test will be generated for this class.");
			return null;
		}

		Set<String> dependencyPackages = new HashSet<String>();

		List<JavaSource<?>> sourcesToProcess = new ArrayList<JavaSource<?>>();

		JavaResource mainJavaResource = getJavaResource(sourceOfClassToTest);
		Resource<?> parent = mainJavaResource.getParent();
		List<Resource<?>> listResources = parent.listResources(new ResourceFilter() {
			@Override
			public boolean accept(Resource<?> resource) {
				return resource instanceof JavaResource;
			}
		});
		dependencyPackages.add(sourceOfClassToTest.getPackage());
		for (Resource<?> resource : listResources) {
			JavaResource jr = (JavaResource) resource;
			sourcesToProcess.add(getJavaSource(jr));
		}

		processJavaSources(dependencyPackages, sourcesToProcess);

		Map<Object, Object> map = new HashMap<Object, Object>();
		
		map.put("packagesToImport", dependencyPackages);
		map.put("ClassToTest", sourceOfClassToTest.getName());
		map.put("classToTest", sourceOfClassToTest.getName().toLowerCase());
		String output = processor.processTemplate(map,
				"org/adorsys/forge/plugins/tst/JunitTest.jv");
		JavaClass resource = JavaParser.parse(JavaClass.class, output);
		resource.setPackage(sourceOfClassToTest.getPackage()+".test");
		return resource;
	}

	private void processJavaSources(Set<String> dependencyPackages,
			List<JavaSource<?>> sourcesToProcess) 
	{
		List<JavaSource<?>> newSourcesToProcess = new ArrayList<JavaSource<?>>();
		for (JavaSource<?> javaSource : sourcesToProcess) {
			List<Import> imports = javaSource.getImports();
			for (Import import1 : imports) {
				Resource<?> packageResource = processImport(dependencyPackages, import1);
				if(packageResource==null) continue;
				dependencyPackages.add(import1.getPackage());
				List<Resource<?>> listResources = packageResource.listResources(new ResourceFilter() {
					@Override
					public boolean accept(Resource<?> resource) {
						return resource instanceof JavaResource;
					}
				});
				for (Resource<?> resource : listResources) {
					JavaResource jr = (JavaResource) resource;
					newSourcesToProcess.add(getJavaSource(jr));
				}
			}
		}
		if(!newSourcesToProcess.isEmpty()){
			processJavaSources(dependencyPackages, newSourcesToProcess);;
		}
	}

	/*
	 * Returns the file resource associated with this import if the package of the 
	 * resource is not yet processed. 
	 */
	private Resource<?> processImport(final Set<String> dependencyPackages,Import import1) {
		DirectoryResource sourceFolder = java.getSourceFolder();		
		
		// Package is already processed. This class is already in the list.
		String package1 = import1.getPackage();
		if(dependencyPackages.contains(package1)) return null;

		// Package is not in this project. Will be included through maven dependencies.
		String packageResourceName = package1.replace(".", File.separator);
		Resource<?> packageResource = sourceFolder.getChild(packageResourceName);
		if(!packageResource.exists()){
			return null;
		}
		return packageResource;

	}
	
	private JavaSource<?> getJavaSource(JavaResource javaResource){
		try {
			return javaResource.getJavaSource();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private JavaResource getJavaResource(JavaSource<?> javaClass) {
		try {
			return java.getJavaResource(javaClass);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}
	
}
