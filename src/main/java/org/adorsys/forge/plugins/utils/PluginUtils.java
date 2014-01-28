package org.adorsys.forge.plugins.utils;

import static org.jboss.forge.spec.javaee.RestApplicationFacet.REST_APPLICATIONCLASS_PACKAGE;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.adorsys.javaext.display.Association;
import org.adorsys.javaext.display.AssociationType;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.Import;
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
			restPackage = configuration
					.getString(REST_APPLICATIONCLASS_PACKAGE);
		}

		if (restPackage == null)
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
		return String.valueOf(text.charAt(0)).toUpperCase()
				.concat(text.substring(1, text.length()));
	}

	public EntityInfo inspectEntity(JavaClass entity) {
		assert entity != null : "entity can not be null";
		EntityInfo entityInfo = new EntityInfo();
		entityInfo.setEntity(entity);

		Annotation<JavaClass> entityAnnotation = entity
				.getAnnotation(Entity.class);
		assert entityAnnotation != null : "Object does not have the @Entity annotation.";

		List<Field<JavaClass>> fields = entity.getFields();
		for (Field<JavaClass> field : fields) {

			if (field.hasAnnotation(Id.class)) {
				processId(field, entityInfo);
				continue;
			}

			if (field.hasAnnotation(Version.class))
				continue;

			FieldInfo fieldInfo = new FieldInfo();
			fieldInfo.setField(field);
			entityInfo.getFieldInfos().add(fieldInfo);

			Annotation<JavaClass> manyToOneAnnotation = field
					.getAnnotation(ManyToOne.class);
			if (manyToOneAnnotation != null) {
				processToOne(manyToOneAnnotation, fieldInfo, entityInfo);
				continue;
			}

			Annotation<JavaClass> oneToOneAnnotation = field
					.getAnnotation(OneToOne.class);
			if (oneToOneAnnotation != null) {
				processToOne(oneToOneAnnotation, fieldInfo, entityInfo);
				continue;
			}

			Annotation<JavaClass> manyToManyAnnotation = field
					.getAnnotation(ManyToMany.class);
			if (manyToManyAnnotation != null) {
				processToMany(manyToManyAnnotation, fieldInfo, entityInfo);
				continue;
			}

			Annotation<JavaClass> oneToManyAnnotation = field
					.getAnnotation(OneToMany.class);
			if (oneToManyAnnotation != null) {
				processToMany(oneToManyAnnotation, fieldInfo, entityInfo);
				continue;
			}

			if (field.getType().equals(String.class.getSimpleName())) {
				entityInfo.getSimpleStringFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Long.class.getSimpleName())) {
				entityInfo.getSimpleLongFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Integer.class.getSimpleName())) {
				entityInfo.getSimpleIntegerFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Double.class.getSimpleName())) {
				entityInfo.getSimpleDoubleFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Float.class.getSimpleName())) {
				entityInfo.getSimpleFloatFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Boolean.class.getSimpleName())) {
				entityInfo.getSimpleBooleanFields().add(fieldInfo);
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(Date.class.getSimpleName())) {
				entityInfo.getSimpleDateFields().add(fieldInfo);
				entityInfo.getPackageImport().add(Date.class.getName());
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			} else if (field.getType().equals(BigDecimal.class.getSimpleName())) {
				entityInfo.getSimpleBigDecimalFields().add(fieldInfo);
				entityInfo.getPackageImport().add(BigDecimal.class.getName());
				entityInfo.getAllSimpleFields().add(fieldInfo);
				fieldInfo.setSimpleField(true);
				fieldInfo.setFieldType(field.getType());
			}
		}

		return entityInfo;
	}

	private void processToOne(Annotation<JavaClass> toOneAnnotation,
			FieldInfo fieldInfo, EntityInfo entityInfo) {
		Field<JavaClass> field = fieldInfo.getField();
		Annotation<JavaClass> associationAnnotation = field
				.getAnnotation(Association.class);
		if (associationAnnotation == null)
			throw new IllegalStateException("Missing association annotation.");
		
		String selectionModeAnnotation = associationAnnotation.getStringValue("selectionMode");
		if(StringUtils.isNotBlank(selectionModeAnnotation))fieldInfo.setAssociationManager(true);
		
		AssociationType associationType = associationAnnotation.getEnumValue(
				AssociationType.class, "associationType");
		if (associationType == null)
			throw new IllegalStateException("Missing association type.");
		switch (associationType) {
		case AGGREGATION:
			entityInfo.getAggregated().add(fieldInfo);
			if (!entityInfo.getAggregatedFieldsByType().containsKey(
					field.getType()))
				entityInfo.getAggregatedFieldsByType().put(field.getType(),
						new ArrayList<String>());
			entityInfo.getAggregatedFieldsByType().get(field.getType())
					.add(field.getName());
			if (!entityInfo.getAggregatedTypes().contains(field.getType())) {
				entityInfo.getAggregatedTypes().add(field.getType());
				entityInfo.getAggregatedTypesFQN()
						.add(field.getQualifiedType());
			}
			entityInfo.getAggregatedTypes().add(field.getType());
			entityInfo.getAggregatedTypesFQN().add(field.getQualifiedType());
			if (!field.getType().equals(entityInfo.getEntity().getName())) {
				entityInfo.getTestAggregated().add(fieldInfo);
			}
			break;

		case COMPOSITION:
			entityInfo.getComposed().add(fieldInfo);
			if (!entityInfo.getComposedFieldsByType().containsKey(
					field.getType()))
				entityInfo.getComposedFieldsByType().put(field.getType(),
						new ArrayList<String>());
			entityInfo.getComposedFieldsByType().get(field.getType())
					.add(field.getName());
			if (!entityInfo.getComposedTypes().contains(field.getType())) {
				entityInfo.getComposedTypes().add(field.getType());
				entityInfo.getComposedTypesFQN().add(field.getQualifiedType());
			}
			entityInfo.getComposedTypes().add(field.getType());
			entityInfo.getComposedTypesFQN().add(field.getQualifiedType());
			if (!field.getType().equals(entityInfo.getEntity().getName())) {
				entityInfo.getTestComposed().add(fieldInfo);
			}
			break;
		default:
			throw new IllegalStateException("Unknown association type");
		}
	}

	private void processToMany(Annotation<JavaClass> toManyAnnotation,
			FieldInfo fieldInfo, EntityInfo entityInfo) {
		Field<JavaClass> field = fieldInfo.getField();
		Annotation<JavaClass> associationAnnotation = field
				.getAnnotation(Association.class);
		if (associationAnnotation == null)
			throw new IllegalStateException("Missing association annotation.");
		
		String selectionModeAnnotation = associationAnnotation.getStringValue("selectionMode");
		if(StringUtils.isNotBlank(selectionModeAnnotation))fieldInfo.setAssociationManager(true);
		
		AssociationType associationType = associationAnnotation.getEnumValue(
				AssociationType.class, "associationType");
		if (associationType == null)
			throw new IllegalStateException("Missing association type.");
		String targetEntityAnnotationValue = associationAnnotation
				.getStringValue("targetEntity");
		if (StringUtils.isBlank(targetEntityAnnotationValue))
			throw new IllegalStateException(
					"Missing targetEntity annotation value annotation.");
		targetEntityAnnotationValue = targetEntityAnnotationValue.replaceAll(
				".class", ".java");
		JavaClass targetEntity = null;
		try {
			targetEntity = findEntity(targetEntityAnnotationValue, entityInfo.getEntity());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("target entity "
					+ targetEntityAnnotationValue + " not found");
		}
		fieldInfo.setTargetEntity(targetEntity);
		switch (associationType) {
		case AGGREGATION:
			entityInfo.getAggregatedCollections().add(fieldInfo);
			if (!entityInfo.getAggregatedFieldsByType().containsKey(
					targetEntity.getName()))
				entityInfo.getAggregatedFieldsByType().put(
						targetEntity.getName(), new ArrayList<String>());
			entityInfo.getAggregatedFieldsByType().get(targetEntity.getName())
					.add(field.getName());
			if (!entityInfo.getAggregatedTypes().contains(
					targetEntity.getName())) {
				entityInfo.getAggregatedTypes().add(targetEntity.getName());
				entityInfo.getAggregatedTypesFQN().add(
						targetEntity.getQualifiedName());
			}
			break;

		case COMPOSITION:
			entityInfo.getComposedCollections().add(fieldInfo);
			if (!entityInfo.getComposedFieldsByType().containsKey(
					targetEntity.getName()))
				entityInfo.getComposedFieldsByType().put(
						targetEntity.getName(), new ArrayList<String>());
			entityInfo.getComposedFieldsByType().get(targetEntity.getName())
					.add(field.getName());
			if (!entityInfo.getComposedTypes().contains(targetEntity.getName())) {
				entityInfo.getComposedTypes().add(targetEntity.getName());
				entityInfo.getComposedTypesFQN().add(
						targetEntity.getQualifiedName());
			}
			break;
		default:
			throw new IllegalStateException("Unknown association type");
		}
	}

	private void processId(Field<JavaClass> field, EntityInfo entityInfo) {
		String name = field.getName();
		entityInfo.setIdFieldName(name);
		entityInfo.setIdGetterName("get" + firstLetterCaps(name));
		Annotation<JavaClass> genValuennotation = field
				.getAnnotation(GeneratedValue.class);
		if (genValuennotation != null) {
			String generationStrategy = genValuennotation
					.getStringValue("strategy");
			entityInfo.setIdGenerationType(generationStrategy);
		}
	}

	public String getEntityEndpointName(JavaClass entity) {
		return getEntityTable(entity) + "Endpoint";
	}

	public JavaResource findEndPoint(JavaClass entity) {
		String entityEndpointName = getEntityEndpointName(entity);
		String restPackageName = getRestPackageName();
		// String restEndpointFQN = restPackageName + "."+entityEndpointName;
		String restFilePath = StringUtils.replace(restPackageName, ".",
				File.separator) + File.separator + entityEndpointName + ".java";
		try {
			return java.getJavaResource(restFilePath);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(
					"Rest endpoint not found for entity "
							+ entity.getQualifiedName());
		}
	}

	private JavaClass findEntity(final String entity, JavaClass mainEntity) throws FileNotFoundException {
		JavaClass result = null;

		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		if (entity != null) {
			try {
				result = getJavaClassFrom(java.getJavaResource(entity));
			} catch (FileNotFoundException e) {
				// noop. Keep looking
			}
			if (result == null) {
				String mainPackage = mainEntity.getPackage();
				try {
					result = getJavaClassFrom(java.getJavaResource(mainPackage + "." + entity));
				} catch (FileNotFoundException e) {
					// noop. Keep looking
				}
				if(result==null){
					List<Import> imports = mainEntity.getImports();
					String entityImportDeclaration = entity;
					if(StringUtils.endsWith(entity, ".java")){
						entityImportDeclaration = StringUtils.substringBeforeLast(entity, ".");
					}
					for (Import import1 : imports) {
						if(StringUtils.endsWith(import1.getQualifiedName(), entityImportDeclaration)){
							try {
								result = getJavaClassFrom(java.getJavaResource(import1.getPackage() + "." + entity));
							} catch (FileNotFoundException e) {
								// noop. Keep looking
							}
							if(result!=null) break;
						}
					}
				}
			}
		}

		if (result == null) {
			throw new FileNotFoundException(
					"Could not locate JavaClass on which to operate.");
		}

		return result;
	}

	private JavaClass getJavaClassFrom(final Resource<?> resource)
			throws FileNotFoundException {
		JavaSource<?> source = ((JavaResource) resource).getJavaSource();
		if (!source.isClass()) {
			throw new IllegalStateException(
					"Current resource is not a JavaClass!");
		}
		return (JavaClass) source;
	}
}
