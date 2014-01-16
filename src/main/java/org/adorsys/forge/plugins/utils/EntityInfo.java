package org.adorsys.forge.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;

public class EntityInfo {

	private String idFieldName;
	
	private String idGenerationType;
	
	private String idGetterName;
	
	private JavaClass entity;

	private final List<Field<JavaClass>> allSimpleFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleStringFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleLongFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleIntegerFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleDateFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleDoubleFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleFloatFields = new ArrayList<Field<JavaClass>>();
	private final List<Field<JavaClass>> simpleBooleanFields = new ArrayList<Field<JavaClass>>();

	private final List<Field<JavaClass>> randomSimpleFields = new ArrayList<Field<JavaClass>>();
	
	/*
	 * Entities listed in this class are cascaded from the 
	 * containing entity. We will support only the CascadeType.ALL;
	 * 
	 * Before performing a persistence operation on the current entity, 
	 * if the cascaded entity has an id, we will first store it
	 * to attach it with the persistent context and then store the
	 * current entity.
	 * 
	 */
	private final List<Field<JavaClass>> composed = new ArrayList<Field<JavaClass>>();
	
	/*
	 * Referenced entities are not cascaded. They are generally shared
	 * among many referencing entities. Before performing any 
	 * persistence operation on the current entity, we will first 
	 * reload the referenced entity if i has an id, if not set the
	 * reference to null.
	 */
	private final List<Field<JavaClass>> aggregated = new ArrayList<Field<JavaClass>>();

	private final List<String> composedTypes = new ArrayList<String>();
	private final List<String> composedTypesFQN = new ArrayList<String>();

	private final List<String> aggregatedTypes = new ArrayList<String>();
	private final List<String> aggregatedTypesFQN = new ArrayList<String>();

	public String getIdFieldName() {
		return idFieldName;
	}

	public void setIdFieldName(String idFieldName) {
		this.idFieldName = idFieldName;
	}

	public String getIdGenerationType() {
		return idGenerationType;
	}

	public void setIdGenerationType(String idGenerationType) {
		this.idGenerationType = idGenerationType;
	}

	public String getIdGetterName() {
		return idGetterName;
	}

	public void setIdGetterName(String idGetterName) {
		this.idGetterName = idGetterName;
	}

	public List<Field<JavaClass>> getComposed() {
		return composed;
	}

	public List<Field<JavaClass>> getAggregated() {
		return aggregated;
	}

	public List<String> getComposedTypes() {
		return composedTypes;
	}

	public List<String> getComposedTypesFQN() {
		return composedTypesFQN;
	}

	public List<String> getAggregatedTypes() {
		return aggregatedTypes;
	}

	public List<String> getAggregatedTypesFQN() {
		return aggregatedTypesFQN;
	}

	public List<String> getReferencedTypes() {
		List<String> result = new ArrayList<String>();
		result.addAll(composedTypes);
		for (String aggregatedType : aggregatedTypes) {
			if(!result.contains(aggregatedType))
				result.add(aggregatedType);
		}
		return Collections.unmodifiableList(result);
	}

	public List<String> getReferencedTypesFQN() {
		List<String> result = new ArrayList<String>();
		result.addAll(composedTypesFQN);
		for (String aggregatedTypeFqn : aggregatedTypesFQN) {
			if(!result.contains(aggregatedTypeFqn))
				result.add(aggregatedTypeFqn);
		}
		return Collections.unmodifiableList(result);
	}

	public JavaClass getEntity() {
		return entity;
	}

	public void setEntity(JavaClass entity) {
		this.entity = entity;
	}
	
	public List<Field<JavaClass>> getSimpleStringFields() {
		return simpleStringFields;
	}

	public List<Field<JavaClass>> getSimpleLongFields() {
		return simpleLongFields;
	}

	public List<Field<JavaClass>> getSimpleIntegerFields() {
		return simpleIntegerFields;
	}

	public List<Field<JavaClass>> getSimpleDateFields() {
		return simpleDateFields;
	}

	public String randomString(int size){
		return RandomStringUtils.randomAlphabetic(size);
	}

	private Random random = new Random();
	public Long randomLong(){
		return random.nextLong();
	}

	public Integer randomInteger(){
		return random.nextInt();
	}

	public Double randomDouble(){
		return random.nextDouble();
	}

	public Float randomFloat(){
		return random.nextFloat();
	}

	public Boolean randomBoolean(){
		return random.nextBoolean();
	}
	
	public Date randomDate(){
		return new Date();
	}

	public List<Field<JavaClass>> getSimpleBooleanFields() {
		return simpleBooleanFields;
	}

	public List<Field<JavaClass>> getSimpleDoubleFields() {
		return simpleDoubleFields;
	}

	public List<Field<JavaClass>> getSimpleFloatFields() {
		return simpleFloatFields;
	}
	
	private final Map<String, List<String>> aggregatedFieldsByType = new HashMap<String, List<String>>();
	public int getCountForAggregatedFieldsOfType(String fieldType){
		List<String> list = aggregatedFieldsByType.get(fieldType);
		return list==null?0:list.size();
	}
	
	private final Map<String, List<String>> composedFieldsByType = new HashMap<String, List<String>>();
	public int getCountForComposeFieldsOfType(String fieldType){
		List<String> list = composedFieldsByType.get(fieldType);
		return list==null?0:list.size();
	}

	public int getCountForReferencedFieldsOfType(String fieldType){
		return getCountForAggregatedFieldsOfType(fieldType) + getCountForComposeFieldsOfType(fieldType);
	}

	public Map<String, List<String>> getAggregatedFieldsByType() {
		return aggregatedFieldsByType;
	}

	public Map<String, List<String>> getComposedFieldsByType() {
		return composedFieldsByType;
	}
	
	public String randomFields(){
		if(allSimpleFields.size()<=3) {
			randomSimpleFields.addAll(allSimpleFields);
			return randomSimpleFields.size() + " Fields.";
		}
		Collections.shuffle(allSimpleFields);
		int size = allSimpleFields.size()/2;
		for (int i = 0; i < size; i++) {
			randomSimpleFields.add(allSimpleFields.get(i));
		}
		return randomSimpleFields.size() + " Fields.";
	}
	
	public boolean containsField(Field<JavaClass> candidate){
		boolean b = randomSimpleFields.contains(candidate);
		return b;
	}

	public List<Field<JavaClass>> getAllSimpleFields() {
		return allSimpleFields;
	}

	public List<Field<JavaClass>> getRandomSimpleFields() {
		return randomSimpleFields;
	}
	
	public Field<JavaClass> getRandomSimpleField(){
		if(allSimpleFields.isEmpty()){
			if(!composed.isEmpty()) return composed.iterator().next();
			if(!aggregated.isEmpty()) return aggregated.iterator().next();
			throw new IllegalStateException("Entity "+ entity.getQualifiedName() + " has no fields.");
		}
		Collections.shuffle(allSimpleFields);
		return allSimpleFields.iterator().next();
	}
	
	public Field<JavaClass> getSimpleField(String fieldName){
		for (Field<JavaClass> field : allSimpleFields) {
			if(field.getName().equals(fieldName)) return field;
		}
		return null;
	}
	
	/**
	 * Will be call to add this field to the list of imports if we cast the field in the test class.
	 * @param fieldName
	 */
	private final List<String> simpleFieldTypeImport = new ArrayList<String>();
	public void importIfRequired(String fieldName){
		Field<JavaClass> simpleField = getSimpleField(fieldName);
		String qualifiedType = simpleField.getQualifiedType();
		if(simpleFieldTypeImport.contains(qualifiedType)) return;
		if(qualifiedType.startsWith("java.lang"))return;
		simpleFieldTypeImport.add(qualifiedType);
	}

	public List<String> getSimpleFieldTypeImport() {
		return simpleFieldTypeImport;
	}
	
	private final List<String> endPointDeployementPackages = new ArrayList<String>();
	public List<String> getEndPointDeployementPackages(){
		return endPointDeployementPackages;
	}
	public void addToEndpointDeploymentPackage(Collection<String> packages){
		for (String endpointPackage : packages) {
			if(!endPointDeployementPackages.contains(endpointPackage))
				endPointDeployementPackages.add(endpointPackage);
		}
	}
	
}
