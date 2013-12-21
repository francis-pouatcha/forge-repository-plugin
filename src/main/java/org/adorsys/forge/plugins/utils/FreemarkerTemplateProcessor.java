package org.adorsys.forge.plugins.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerTemplateProcessor {
	private freemarker.template.Configuration freemarkerConfig;

	public FreemarkerTemplateProcessor() {
		freemarkerConfig = new freemarker.template.Configuration();
		freemarkerConfig.setClassForTemplateLoading(getClass(), "/");
		freemarkerConfig.setObjectWrapper(new DefaultObjectWrapper());
	}

	/**
	 * Processes the provided data model with the specified Freemarker template
	 * 
	 * @param map
	 *            the data model to use for template processing.
	 * @param templateLocation
	 *            The location of the template relative to the classpath
	 * @return The text output after successfully processing the template
	 */
	public String processTemplate(Map<Object, Object> map,
			String templateLocation) {
		Writer output = new StringWriter();
		try {
			Template templateFile = freemarkerConfig
					.getTemplate(templateLocation);
			templateFile.process(map, output);
			output.flush();
		} catch (IOException ioEx) {
			throw new RuntimeException(ioEx);
		} catch (TemplateException templateEx) {
			throw new RuntimeException(templateEx);
		}
		return output.toString();
	}

}
