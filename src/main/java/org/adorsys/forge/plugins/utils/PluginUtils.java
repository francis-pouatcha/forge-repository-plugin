package org.adorsys.forge.plugins.utils;

import static org.jboss.forge.spec.javaee.RestApplicationFacet.REST_APPLICATIONCLASS_PACKAGE;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Member;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.java.util.Strings;
import org.jboss.forge.parser.java.util.Types;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.project.ProjectScoped;
import org.jboss.forge.spec.javaee.RestApplicationFacet;

public class PluginUtils {

	@Inject
	JavaSourceFacet java;
	@Inject
	private Project project;

	@Inject
	@Current
	private Resource<?> currentResource;

	@Inject
	private RestResourceTypeVisitor resourceTypeVisitor;

	@Inject
	private ShellPrintWriter writer;

	@Inject
	private ShellPrompt prompt;

	@Inject
	@ProjectScoped
	private Configuration configuration;

	public List<JavaResource> calculateTargetToProcess(JavaResource jpaClass,
			Resource<?> jpaPackage, final PipeOut out)
			throws FileNotFoundException {

		List<JavaResource> targetsToProcess = new ArrayList<JavaResource>();

		if (jpaClass != null) {
			targetsToProcess.add(jpaClass);
		}

		if (jpaPackage != null) {
			if (jpaPackage instanceof JavaResource) {
				JavaResource jr = (JavaResource) jpaPackage;
				selectTargets(out, targetsToProcess, jr);
			} else if (jpaPackage instanceof DirectoryResource) {
				DirectoryResource dr = (DirectoryResource) jpaPackage;
				selectTargets(out, targetsToProcess, dr);
			} else {
				ShellMessages
						.warn(out,
								"Provided class is neiter a java package nor a java file");
			}
		}
		if (jpaClass == null && jpaPackage == null && currentResource != null) {
			if (currentResource instanceof JavaResource) {
				JavaResource jr = (JavaResource) currentResource;
				selectTargets(out, targetsToProcess, jr);
			} else if (currentResource instanceof DirectoryResource) {
				DirectoryResource dr = (DirectoryResource) currentResource;
				selectTargets(out, targetsToProcess, dr);
			} else {
				ShellMessages
						.warn(out,
								"Current resource is neiter a java package nor a java file");
			}
		}
		return targetsToProcess;
	}

	public String resolveIdType(JavaClass entity) {
		for (Member<JavaClass, ?> member : entity.getMembers()) {
			if (member.hasAnnotation(Id.class)) {
				if (member instanceof Method) {
					return ((Method<?>) member).getReturnType();
				}
				if (member instanceof Field) {
					return ((Field<?>) member).getType();
				}
			}
		}
		return "Object";
	}

	public void selectTargets(final PipeOut out,
			List<JavaResource> ressourcesToProcess, JavaResource javaResource)
			throws FileNotFoundException {

		if (ressourcesToProcess.contains(javaResource))
			return;

		JavaSource<?> entity = javaResource.getJavaSource();
		if (entity instanceof JavaClass) {
			if (entity.hasAnnotation(Entity.class)) {
				ressourcesToProcess.add(javaResource);
			} else {
				displaySkippingResourceMsg(out, entity);
			}
		} else {
			displaySkippingResourceMsg(out, entity);
		}
	}

	public void selectTargets(final PipeOut out,
			List<JavaResource> ressourcesToProcess,
			DirectoryResource directoryResource) throws FileNotFoundException {

		DirectoryResource sourceFolder = java.getSourceFolder();
		if (!directoryResource.getFullyQualifiedName().startsWith(
				sourceFolder.getFullyQualifiedName())) {
			ShellMessages.warn(out,
					"selected directory is not in the source folder. Will not be processed. ["
							+ directoryResource.getFullyQualifiedName() + "]");
			return;
		}
		List<Resource<?>> javaResources = directoryResource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof JavaResource;
					}
				});
		if (javaResources != null) {
			for (Resource<?> resource2 : javaResources) {
				JavaResource jr = (JavaResource) resource2;
				selectTargets(out, ressourcesToProcess, jr);
			}
		}
		List<Resource<?>> dirResources = directoryResource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof DirectoryResource;
					}
				});
		for (Resource<?> resource2 : dirResources) {
			DirectoryResource dr = (DirectoryResource) resource2;
			selectTargets(out, ressourcesToProcess, dr);
		}
	}

	private static void displaySkippingResourceMsg(final PipeOut out,
			final JavaSource<?> entity) {
		if (!out.isPiped()) {
			ShellMessages.info(out, "Skipped non-@Entity Java resource ["
					+ entity.getQualifiedName() + "]");
		}
	}

	public String getBasicType(JavaClass entity, final PipeOut out) {
		String idType = resolveIdType(entity);
		if (!Types.isBasicType(idType)) {
			ShellMessages.error(out,
					"Skipped class [" + entity.getQualifiedName()
							+ "] because @Id type [" + idType
							+ "] is not supported by repository generation.");
			return null;
		}
		return idType;
	}

	public String getEntityTable(final JavaClass entity) {
		String table = entity.getName();
		if (entity.hasAnnotation(Entity.class)) {
			Annotation<JavaClass> a = entity.getAnnotation(Entity.class);
			if (!Strings.isNullOrEmpty(a.getStringValue("name"))) {
				table = a.getStringValue("name");
			} else if (!Strings.isNullOrEmpty(a.getStringValue())) {
				table = a.getStringValue();
			}
		}
		return table;
	}

	public String getResourcePath(String entityTable) {
		String proposedQualifiedClassName = getRestPackageName() + "."
				+ entityTable + "Endpoint";
		String proposedResourcePath = "/" + entityTable.toLowerCase() + "s";
		resourceTypeVisitor.setFound(false);
		resourceTypeVisitor.setProposedPath(proposedResourcePath);
		while (true) {
			java.visitJavaSources(resourceTypeVisitor);
			if (resourceTypeVisitor.isFound()) {
				if (proposedQualifiedClassName.equals(resourceTypeVisitor
						.getQualifiedClassNameForMatch())) {
					// The class might be overwritten later, so break out
					break;
				}
				ShellMessages.warn(writer, "The @Path " + proposedResourcePath
						+ " conflicts with an existing @Path.");
				String computedPath = proposedResourcePath.startsWith("/") ? "forge"
						+ proposedResourcePath
						: "forge/" + proposedResourcePath;
				proposedResourcePath = prompt
						.prompt("Provide a different URI path value for the generated resource.",
								computedPath);
				resourceTypeVisitor.setProposedPath(proposedResourcePath);
				resourceTypeVisitor.setFound(false);
			} else {
				break;
			}
		}
		return proposedResourcePath;
	}

	public String getRestPackageName() {
		String restPackage = null;
		if (project.hasFacet(RestApplicationFacet.class)) {
			restPackage = configuration.getString(REST_APPLICATIONCLASS_PACKAGE);
		}
		
		if(restPackage==null) 
			restPackage = java.getBasePackage() + ".rest";
		
		return restPackage;
	}

	public String resolveIdGetterName(JavaClass entity) {
		String result = null;

		for (Member<JavaClass, ?> member : entity.getMembers()) {
			if (member.hasAnnotation(Id.class)) {
				String name = member.getName();
				String type = null;
				if (member instanceof Method) {
					type = ((Method<?>) member).getReturnType();
					if (name.startsWith("get")) {
						name = name.substring(2);
					}
				} else if (member instanceof Field) {
					type = ((Field<?>) member).getType();
				}

				if (type != null) {
					for (Method<JavaClass> method : entity.getMethods()) {
						// It's a getter
						if (method.getParameters().size() == 0
								&& type.equals(method.getReturnType())) {
							if (method.getName().toLowerCase()
									.contains(name.toLowerCase())) {
								result = method.getName() + "()";
								break;
							}
						}
					}
				}

				if (result != null) {
					break;
				} else if (type != null && member.isPublic()) {
					String memberName = member.getName();
					// Cheat a little if the member is public
					if (member instanceof Method
							&& memberName.startsWith("get")) {
						memberName = memberName.substring(3);
						memberName = Strings.uncapitalize(memberName);
					}
					result = memberName;
				}
			}
		}

		if (result == null) {
			throw new RuntimeException(
					"Could not determine @Id field and getter method for @Entity ["
							+ entity.getQualifiedName() + "]. Aborting.");
		}

		return result;
	}
	
    public static String firstLetterCaps(String text) {
        return String.valueOf(text.charAt(0)).toUpperCase().concat(text.substring(1, text.length()));
    }
	
}
