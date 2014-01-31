package org.adorsys.forge.plugins.rel;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.adorsys.forge.plugins.utils.FreemarkerTemplateProcessor;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.facets.JavaSourceFacet;

public class RelationshipGenerator {

	@Inject
	FreemarkerTemplateProcessor processor;

	@Inject
	JavaSourceFacet java;

	public JavaClass generateFrom(JavaClass sourceEntityClass,JavaClass targetEntityClass) throws FileNotFoundException 
	{
	
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("sourceEntity", sourceEntityClass);
		map.put("targetEntity", targetEntityClass);
		String output = processor.processTemplate(map,"org/adorsys/forge/plugins/rel/Assoc.ftl");
		JavaClass entityAssoc = JavaParser.parse(JavaClass.class, output);
		return entityAssoc;
	}
}
