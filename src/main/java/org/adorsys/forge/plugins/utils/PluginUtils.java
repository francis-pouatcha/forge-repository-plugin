package org.adorsys.forge.plugins.utils;

import static org.jboss.forge.spec.javaee.RestApplicationFacet.REST_APPLICATIONCLASS_PACKAGE;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
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
import org.adorsys.javaext.display.ToStringField;
import org.adorsys.javaext.list.ListField;
import org.adorsys.javaext.relation.Relationship;
import org.adorsys.javaext.relation.RelationshipEnd;
import org.adorsys.javaext.relation.RelationshipTable;
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

		if(entityInfo.isRelationship()){
			try {
				processRelationship(entity, entityInfo);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		return entityInfo;
	}

	private void processToOne(Annotation<JavaClass> toOneAnnotation,
			FieldInfo fieldInfo, EntityInfo entityInfo) {
		Field<JavaClass> field = fieldInfo.getField();
		Annotation<JavaClass> associationAnnotation = field
				.getAnnotation(Association.class);
		if (associationAnnotation == null){
			if(entityInfo.getEntity().hasAnnotation(RelationshipTable.class)){
				try {
					processToOneRelationship(toOneAnnotation, fieldInfo, entityInfo);
				} catch (FileNotFoundException e) {
					throw new IllegalStateException(e);
				}
				return;
			} else {
				throw new IllegalStateException("Missing association annotation.");
			}
		}
		
		String selectionModeAnnotation = associationAnnotation.getStringValue("selectionMode");
		if(StringUtils.isNotBlank(selectionModeAnnotation))fieldInfo.setAssociationManager(true);
		
		JavaClass targetEntity = null;
		String targetEntityAnnotationValue = toOneAnnotation.getStringValue("targetEntity");
		if(StringUtils.isBlank(targetEntityAnnotationValue))
			targetEntityAnnotationValue = associationAnnotation
				.getStringValue("targetEntity");

		if (StringUtils.isBlank(targetEntityAnnotationValue))
			targetEntityAnnotationValue = field.getType();

		targetEntityAnnotationValue = targetEntityAnnotationValue.replaceAll(
				".class", ".java");
		try {
			targetEntity = findEntity(targetEntityAnnotationValue, entityInfo.getEntity());
		} catch (FileNotFoundException e) {
			// noop.
		}
		fieldInfo.setTargetEntity(targetEntity);
		
		String displayedFields = readDisplayFields(associationAnnotation);
		if(StringUtils.isBlank(displayedFields) && targetEntity!=null){
			displayedFields = readListFields(targetEntity);
		}
		if(StringUtils.isBlank(displayedFields) && targetEntity!=null){
			displayedFields = readToStringFields(targetEntity);
		}
		fieldInfo.setDisplayedFields(displayedFields);
		
		String mappedByStringValue = toOneAnnotation.getStringValue("mappedBy");
		if(StringUtils.isNotBlank(mappedByStringValue)){
			fieldInfo.setMappedBy(mappedByStringValue);
		}

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
	
	private void processRelationship(JavaClass relationshipClass, EntityInfo relEntityInfo) throws FileNotFoundException{
		
		Field<JavaClass> sourceRelationField = relationshipClass.getField("source");
		Field<JavaClass> targetRelationField = relationshipClass.getField("target");
		
		/*
		 * Find the JavaClass of the field and process all field of this type
		 * departing from there.
		 */
		String sourceEndFQN = sourceRelationField.getQualifiedType();
		JavaClass sourceEndClass = findEntity(sourceEndFQN, relationshipClass);
		/*
		 * Keep track of each field of the source entity that references 
		 * this relation as a source.
		 */
		List<Field<JavaClass>> sourceFields = sourceEndClass.getFields();
		for (Field<JavaClass> sourceField : sourceFields) {
			// The field must carry a Relationship annotation.
			if(!sourceField.hasAnnotation(Relationship.class)) continue;
			
			Annotation<JavaClass> sourceRelationshipAnnotation = sourceField.getAnnotation(Relationship.class);
			// Association End must be source.
			RelationshipEnd relationshipEnd = sourceRelationshipAnnotation.getEnumValue(RelationshipEnd.class, "end");
			if(!RelationshipEnd.SOURCE.equals(relationshipEnd)) continue;

			// The field class muss be of the same type as 
			if(!sourceField.hasAnnotation(OneToMany.class)) throw new IllegalStateException("Relationship must have the annotation oneToMany");
			Annotation<JavaClass> oneToManyAnnotation = sourceField.getAnnotation(OneToMany.class);
			String targetEntityAnnotationValue = oneToManyAnnotation.getStringValue("targetEntity");
			if(StringUtils.isBlank(targetEntityAnnotationValue))throw new IllegalStateException("Missing targetEntity");
			if(targetEntityAnnotationValue.endsWith(".class"))targetEntityAnnotationValue=StringUtils.substringBeforeLast(targetEntityAnnotationValue, ".");
			if(targetEntityAnnotationValue.contains(".")){
				if(!targetEntityAnnotationValue.equals(relationshipClass.getQualifiedName())) continue;
			} else {
				if(!targetEntityAnnotationValue.equals(relationshipClass.getName())) continue;
			}			
			
			// Source qualifier is the name of the association in the source end.
			// This also equivalent to the name of the field.
			String sourceQualifier = sourceField.getName();
			String targetQualifier = sourceRelationshipAnnotation.getStringValue("targetQualifier");
			if(StringUtils.isBlank(targetQualifier))targetQualifier="source";
			RelationKey relationKey = new RelationKey(sourceQualifier, targetQualifier);
			Relation relation = relEntityInfo.getRelationMap().get(relationKey);
			if(relation==null) {
				relation=new Relation(new SourceEnd(sourceQualifier), new TargetEnd(targetQualifier));
				relEntityInfo.getRelationMap().put(relationKey, relation);
			}
			relation.getSourceEnd().setField(sourceField);
		} 
		
		/*
		 * Find the JavaClass of the field and process all field of this type
		 * departing from there.
		 */
		String targetEndFQN = targetRelationField.getQualifiedType();
		JavaClass targetEndClass = findEntity(targetEndFQN, relationshipClass);
		/*
		 * Keep track of each field of the target entity that references 
		 * this relation as a target.
		 */
		List<Field<JavaClass>> targetFields = targetEndClass.getFields();
		for (Field<JavaClass> targetField : targetFields) {

			// The field must carry a Relationship annotation.
			if(!targetField.hasAnnotation(Relationship.class)) continue;
			Annotation<JavaClass> targetRelationshipAnnotation = targetField.getAnnotation(Relationship.class);
			// Association End must be target.
			RelationshipEnd relationshipEnd = targetRelationshipAnnotation.getEnumValue(RelationshipEnd.class, "end");
			if(!RelationshipEnd.TARGET.equals(relationshipEnd)) continue;

			// The field class muss be of the same type as 
			if(!targetField.hasAnnotation(OneToMany.class)) throw new IllegalStateException("Relationship must have the annotation oneToMany");
			Annotation<JavaClass> oneToManyAnnotation = targetField.getAnnotation(OneToMany.class);
			String targetEntityAnnotationValue = oneToManyAnnotation.getStringValue("targetEntity");
			if(StringUtils.isBlank(targetEntityAnnotationValue))throw new IllegalStateException("Missing targetEntity");
			if(targetEntityAnnotationValue.endsWith(".class"))targetEntityAnnotationValue=StringUtils.substringBeforeLast(targetEntityAnnotationValue, ".");
			if(targetEntityAnnotationValue.contains(".")){
				if(!targetEntityAnnotationValue.equals(relationshipClass.getQualifiedName())) continue;
			} else {
				if(!targetEntityAnnotationValue.equals(relationshipClass.getName())) continue;
			}
			
			// Target qualifier is the name of the association in the target end.
			// This also equivalent to the name of the field.
			String targetQualifier = targetField.getName();
			String sourceQualifier = targetRelationshipAnnotation.getStringValue("sourceQualifier");
			if(StringUtils.isBlank(sourceQualifier))throw new IllegalStateException("Source qualifier cn not be null");
			RelationKey relationKey = new RelationKey(sourceQualifier,targetQualifier);
			Relation relation = relEntityInfo.getRelationMap().get(relationKey);
			if(relation==null) {
				relation=new Relation(new SourceEnd(sourceQualifier), new TargetEnd(targetQualifier));
				relEntityInfo.getRelationMap().put(relationKey, relation);
			}
			relation.getTargetEnd().setField(targetField);
		} 
		
		/*
		 * For now each relationship that defined display field on the oder end has it set.
		 * 
		 * Now we will compute the display field define at the class level on each end
		 * and use it when nothing is available.
		 */
		String sourceClassDisplayedFields = readListFields(sourceEndClass);
		if(StringUtils.isBlank(sourceClassDisplayedFields))
			sourceClassDisplayedFields=readToStringFields(sourceEndClass);
		
		String targetClassDisplayedFields = readListFields(targetEndClass);
		if(StringUtils.isBlank(targetClassDisplayedFields))
			targetClassDisplayedFields=readToStringFields(targetEndClass);

		FieldInfo sourceFieldInfo = null;
		FieldInfo targetFieldInfo = null;
		List<FieldInfo> fieldInfos = relEntityInfo.getFieldInfos();
		for (FieldInfo fi : fieldInfos) {
			if(fi.getName().equals("source")){
				sourceFieldInfo=fi;
			} else if (fi.getName().equals("target")){
				targetFieldInfo=fi;
			}
		}
		sourceFieldInfo.setDisplayedFields(sourceClassDisplayedFields);
		targetFieldInfo.setDisplayedFields(targetClassDisplayedFields);
		
		Collection<Relation> relations = relEntityInfo.getRelationMap().values();
		for (Relation relation : relations) {
			String identifier = relation.getIdentifier();
			
			Field<JavaClass> sourceField = relation.getSourceEnd().getField();
			/*
			 * Attention, the display fields listed here is for the inverse side of the relation
			 */
			Annotation<JavaClass> sourceAssociationAnnotation = sourceField.getAnnotation(Association.class);
			String targetDisplayedFields = readDisplayFields(sourceAssociationAnnotation);
			if(StringUtils.isNotBlank(targetDisplayedFields)){
				relation.getTargetEnd().setDisplayFields(targetDisplayedFields);
				targetFieldInfo.putDisplayField(identifier, targetDisplayedFields);
			} else {
				relation.getTargetEnd().setDisplayFields(targetClassDisplayedFields);
				targetFieldInfo.putDisplayField(identifier, targetClassDisplayedFields);
			}

			Field<JavaClass> targetField = relation.getTargetEnd().getField();
			/*
			 * Target field is optional.
			 */
			if(targetField!=null){ 
				/*
				 * Attention, the display fields listed here is for the inverse side of the relation
				 */
				Annotation<JavaClass> targetAssociationAnnotation = targetField.getAnnotation(Association.class);
				String sourceDisplayedFields = readDisplayFields(targetAssociationAnnotation);
				if(StringUtils.isNotBlank(sourceDisplayedFields)){
					relation.getSourceEnd().setDisplayFields(sourceDisplayedFields);
					sourceFieldInfo.putDisplayField(identifier, sourceDisplayedFields);
				} else {
					relation.getSourceEnd().setDisplayFields(sourceClassDisplayedFields);
					sourceFieldInfo.putDisplayField(identifier, sourceClassDisplayedFields);
				}
			} else {
				sourceFieldInfo.putDisplayField(identifier, sourceClassDisplayedFields);
			}
		}
	}

	/*
	 * A toOne relationship is simple. It has a source and a target. So this field 
	 * either the source or the target of the toOne relationship.
	 */
	private void processToOneRelationship(Annotation<JavaClass> toOneAnnotation,
			FieldInfo fieldInfo, EntityInfo entityInfo) throws FileNotFoundException {
		Field<JavaClass> field = fieldInfo.getField();

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

		JavaClass targetEntity = null;
		String targetEntityAnnotationValue = toManyAnnotation.getStringValue("targetEntity");
		if(StringUtils.isBlank(targetEntityAnnotationValue))
			targetEntityAnnotationValue = associationAnnotation
				.getStringValue("targetEntity");

		if (StringUtils.isBlank(targetEntityAnnotationValue))
			throw new IllegalStateException(
					"Missing targetEntity annotation value annotation.");

		targetEntityAnnotationValue = targetEntityAnnotationValue.replaceAll(
				".class", ".java");
		try {
			targetEntity = findEntity(targetEntityAnnotationValue, entityInfo.getEntity());
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("target entity "
					+ targetEntityAnnotationValue + " not found");
		}
		fieldInfo.setTargetEntity(targetEntity);
		
		String mappedByStringValue = toManyAnnotation.getStringValue("mappedBy");
		if(StringUtils.isNotBlank(mappedByStringValue)){
			fieldInfo.setMappedBy(mappedByStringValue);
		}
		
		String displayedFields = readDisplayFields(associationAnnotation);
		if(StringUtils.isBlank(displayedFields) && targetEntity!=null){
			displayedFields = readListFields(targetEntity);
		}
		if(StringUtils.isBlank(displayedFields) && targetEntity!=null){
			displayedFields = readToStringFields(targetEntity);
		}
		fieldInfo.setDisplayedFields(displayedFields);
		
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
			if(StringUtils.isBlank(fieldInfo.getMappedBy())) throw new IllegalStateException("Missing mapped by in a to many annotation value.");
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
	
	private String readDisplayFields(Annotation<JavaClass> associationAnnotation){
		String displayedFields = associationAnnotation.getLiteralValue("fields");
		if(displayedFields==null) return null; 
		displayedFields=displayedFields.trim();
		if(displayedFields.startsWith("{"))
			displayedFields=StringUtils.substringAfter(displayedFields, "{");
		if(displayedFields.endsWith("}"))
			displayedFields = StringUtils.substringBefore(displayedFields, "}");
		return displayedFields;
		
	}
	
	private String readListFields(JavaClass targetEntity) {
		Annotation<JavaClass> listFieldAnnotation = targetEntity.getAnnotation(ListField.class);
		if(listFieldAnnotation==null)return null;
		String listFields = listFieldAnnotation.getLiteralValue();
		listFields=listFields.trim();
		if(listFields.startsWith("{"))
			listFields=StringUtils.substringAfter(listFields, "{");
		if(listFields.endsWith("}"))
			listFields = StringUtils.substringBefore(listFields, "}");
		return listFields;
	}

	private String readToStringFields(JavaClass targetEntity) {
		Annotation<JavaClass> toStringFieldAnnotation = targetEntity.getAnnotation(ToStringField.class);
		if(toStringFieldAnnotation==null)return null;
		String toStringFields = toStringFieldAnnotation.getLiteralValue();
		toStringFields=toStringFields.trim();
		if(toStringFields.startsWith("{"))
			toStringFields=StringUtils.substringAfter(toStringFields, "{");
		if(toStringFields.endsWith("}"))
			toStringFields = StringUtils.substringBefore(toStringFields, "}");
		return toStringFields;
	}
}
