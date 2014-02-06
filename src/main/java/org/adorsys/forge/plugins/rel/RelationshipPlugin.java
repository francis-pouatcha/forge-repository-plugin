package org.adorsys.forge.plugins.rel;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.OneToMany;

import org.adorsys.forge.plugins.repo.RepositoryFacet;
import org.adorsys.javaext.relation.Relationship;
import org.adorsys.javaext.relation.RelationshipEnd;
import org.adorsys.javaext.relation.RelationshipTable;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.util.Strings;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.spec.javaee.PersistenceFacet;

/**
 * Add a relationship table to help manage one to many unidirectional and many
 * to many relationships.
 * 
 * @author francis pouatcha
 * 
 */
@Alias("relationship")
@RequiresProject
@Help("Add a new relationship entity.")
@RequiresFacet({ RepositoryFacet.class, JavaSourceFacet.class })
public class RelationshipPlugin implements Plugin {

	@Inject
	private Project project;

	@Inject
	private Event<InstallFacets> request;

	@Inject
	private Event<PickupResource> pickup;

	@Inject
	private Shell shell;

	@Inject
	JavaSourceFacet java;

	@Inject
	private RelationshipGenerator relationshipGenerator;

	@Command(value = "add")
	public void setType(
			@Option(name = "sourceEntity", type = PromptType.JAVA_CLASS, required = true) final String sourceEntity,
			@Option(name = "sourceQualifier", required = true, help = "The name of reference in the source entity extent") String sourceQualifier,
			@Option(name = "targetEntity", type = PromptType.JAVA_CLASS, required = true) final String targetEntity,
			@Option(name = "targetQualifier", help = "The name of reference in the target entity extent") String targetQualifier,
			final PipeOut out) throws FileNotFoundException {

		JavaClass targetEntityClass;
		JavaClass sourceEntityClass;

		try {
			/*
			 * Synchronize source and target references if necessary
			 */
			sourceEntityClass = findEntity(sourceEntity);
			if(StringUtils.equals(sourceEntity, targetEntity)){
				targetEntityClass=sourceEntityClass;
			} else {
				targetEntityClass = findEntity(targetEntity);
				if(sourceEntityClass.getName().equals(targetEntityClass.getName())){
					targetEntityClass=sourceEntityClass;
				}
			}
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}

		JavaClass assocClass = relationshipGenerator.generateFrom(
				sourceEntityClass, targetEntityClass);
		assocClass.setPackage(sourceEntityClass.getPackage());
		assocClass.addAnnotation(RelationshipTable.class);
		if (!java.getJavaResource(assocClass).exists()) {
			java.saveJavaSource(assocClass);
		}

		if (sourceEntityClass.hasField(sourceQualifier)) {
			throw new IllegalStateException(
					"Entity already has a field named [" + sourceQualifier
							+ "]");
		}
		if (StringUtils.isNotBlank(targetQualifier)
				&& targetEntityClass.hasField(targetQualifier)) {
			throw new IllegalStateException(
					"Entity already has a field named [" + targetQualifier
							+ "]");
		}
		Field<JavaClass> sourceField = sourceEntityClass.addField("private Set<"+assocClass.getName()+"> "+ sourceQualifier+ "= new HashSet<"+assocClass.getName()+">();");
		sourceEntityClass.addImport(Set.class);
		sourceEntityClass.addImport(HashSet.class);
		
		Annotation<JavaClass> oneToManySourceAnnotation = sourceField.addAnnotation(OneToMany.class);
		oneToManySourceAnnotation.setStringValue("mappedBy", "source");
		oneToManySourceAnnotation.setLiteralValue("targetEntity", assocClass.getName()+".class");
		
		createGetterAndSetter(sourceEntityClass, sourceField);
		
		Annotation<JavaClass> sourceRelationshipAnnotation = sourceField
				.addAnnotation(Relationship.class);
		sourceRelationshipAnnotation
				.setEnumValue("end", RelationshipEnd.SOURCE);
		sourceRelationshipAnnotation.setLiteralValue("sourceEntity",
				sourceEntityClass.getName() + ".class");
		sourceRelationshipAnnotation.setLiteralValue("targetEntity",
				targetEntityClass.getName() + ".class");
		sourceRelationshipAnnotation.setStringValue("sourceQualifier",
				sourceQualifier);
		
		if (StringUtils.isNotBlank(targetQualifier)) {
			sourceRelationshipAnnotation.setStringValue("targetQualifier",
					targetQualifier);

			Field<JavaClass> targetField = targetEntityClass.addField("private Set<"+assocClass.getName()+"> "+ targetQualifier+ "= new HashSet<"+assocClass.getName()+">();");
			targetEntityClass.addImport(Set.class);
			targetEntityClass.addImport(HashSet.class);

			Annotation<JavaClass> oneToManyTargetAnnotation = targetField.addAnnotation(OneToMany.class);
			oneToManyTargetAnnotation.setStringValue("mappedBy", "target");
			oneToManyTargetAnnotation.setLiteralValue("targetEntity", assocClass.getName()+".class");
			
			createGetterAndSetter(targetEntityClass, targetField);

			Annotation<JavaClass> targetRelationshipAnnotation = targetField
					.addAnnotation(Relationship.class);
			targetRelationshipAnnotation.setEnumValue("end",
					RelationshipEnd.TARGET);
			targetRelationshipAnnotation.setLiteralValue("sourceEntity",
					sourceEntityClass.getName() + ".class");
			targetRelationshipAnnotation.setLiteralValue("targetEntity",
					targetEntityClass.getName() + ".class");
			targetRelationshipAnnotation.setStringValue("sourceQualifier",
					sourceQualifier);
			targetRelationshipAnnotation.setStringValue("targetQualifier",
					targetQualifier);
			
			if(sourceEntityClass==targetEntityClass){
				pickup.fire(new PickupResource(java.saveJavaSource(sourceEntityClass)));
			} else {
				java.saveJavaSource(targetEntityClass);
				pickup.fire(new PickupResource(java.saveJavaSource(sourceEntityClass)));
			}
		}else {
			pickup.fire(new PickupResource(java.saveJavaSource(sourceEntityClass)));
		}

	}

	private JavaClass findEntity(final String entity)
			throws FileNotFoundException {
		JavaClass result = null;

		PersistenceFacet scaffold = project.getFacet(PersistenceFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		if (entity != null) {
			result = getJavaClassFrom(java.getJavaResource(entity));
			if (result == null) {
				result = getJavaClassFrom(java.getJavaResource(scaffold
						.getEntityPackage() + "." + entity));
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

	private static void createGetterAndSetter(final JavaClass clazz,
			final Field<JavaClass> field) {
		if (!clazz.hasField(field)) {
			throw new IllegalArgumentException(
					"Entity did not contain the given field [" + field + "]");
		}

		clazz.getMethods();

		String fieldName = field.getName();
		String methodNameSuffix = Strings.capitalize(fieldName);
		clazz.addMethod().setReturnType(field.getTypeInspector().toString())
				.setName("get" + methodNameSuffix).setPublic()
				.setBody("return this." + fieldName + ";");

		if (!field.isFinal()) {
			clazz.addMethod()
					.setReturnTypeVoid()
					.setName("set" + methodNameSuffix)
					.setPublic()
					.setParameters(
							"final " + field.getTypeInspector().toString()
									+ " " + fieldName)
					.setBody("this." + fieldName + " = " + fieldName + ";");
		}
	}

}
