package org.adorsys.forge.plugins.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.adorsys.javaext.relation.RelationshipTable;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.forge.parser.java.JavaClass;

public class EntityInfo {
	private static final List<FieldInfo> emptyFields = Collections.emptyList();

	private String idFieldName;
	
	private String idGenerationType;
	
	private String idGetterName;
	
	private JavaClass entity;
	
	private final List<FieldInfo> allSimpleFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleStringFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleLongFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleIntegerFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleDateFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleDoubleFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleFloatFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleBooleanFields = new ArrayList<FieldInfo>();

	private final List<FieldInfo> randomSimpleFields = new ArrayList<FieldInfo>();
	private final List<FieldInfo> simpleBigDecimalFields = new ArrayList<FieldInfo>();
	
	private final Set<String> packageImport = new HashSet<String>();
	
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
	private final List<FieldInfo> composed = new ArrayList<FieldInfo>();
	private final List<FieldInfo> composedCollections = new ArrayList<FieldInfo>();
	private final List<FieldInfo> testComposed = new ArrayList<FieldInfo>();
	
	/*
	 * Referenced entities are not cascaded. They are generally shared
	 * among many referencing entities. Before performing any 
	 * persistence operation on the current entity, we will first 
	 * reload the referenced entity if i has an id, if not set the
	 * reference to null.
	 */
	private final List<FieldInfo> aggregated = new ArrayList<FieldInfo>();
	private final List<FieldInfo> aggregatedCollections = new ArrayList<FieldInfo>();
	private final List<FieldInfo> testAggregated = new ArrayList<FieldInfo>();

	private final Set<String> composedTypes = new HashSet<String>();
	private final Set<String> composedCollectionTypes = new HashSet<String>();
	private final Set<String> composedTypesFQN = new HashSet<String>();
	private final Set<String> composedCollectionTypesFQN = new HashSet<String>();

	private final Set<String> aggregatedTypes = new HashSet<String>();
	private final Set<String> aggregatedCollectionTypes = new HashSet<String>();
	private final Set<String> aggregatedTypesFQN = new HashSet<String>();
	private final Set<String> aggregatedCollectionTypesFQN = new HashSet<String>();

	private final Random random = new Random();
    private final List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>();

	private final Map<RelationKey, Relation> relationMap = new HashMap<RelationKey, Relation>();
	
    
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

	public List<FieldInfo> getComposed() {
		return composed;
	}

	public List<FieldInfo> getAggregated() {
		return aggregated;
	}
	public List<FieldInfo> getAggregatedQualified() {
		if(isRelationship()) return aggregated;
		return emptyFields;
	}
	public List<FieldInfo> getAggregatedUnqualified() {
		if(!isRelationship()) return aggregated;
		return emptyFields;
	}
	
	public List<FieldInfo> getComposedCollections() {
		return composedCollections;
	}

	public List<FieldInfo> getAggregatedCollections() {
		return aggregatedCollections;
	}

	public List<String> getInvolvedServices(){
		Set<String> resultSet = new HashSet<String>();
		resultSet.add(getEntity().getName());
		resultSet.addAll(composedTypes);
		resultSet.addAll(aggregatedTypes);
		return new ArrayList<String>(resultSet);
	}
	
	public List<FieldInfo> getTestComposed() {
		return testComposed;
	}

	public List<FieldInfo> getTestAggregated() {
		return testAggregated;
	}

	public JavaClass getEntity() {
		return entity;
	}

	public void setEntity(JavaClass entity) {
		this.entity = entity;
	}
	
	public List<FieldInfo> getSimpleStringFields() {
		return simpleStringFields;
	}

	public List<FieldInfo> getSimpleLongFields() {
		return simpleLongFields;
	}

	public List<FieldInfo> getSimpleIntegerFields() {
		return simpleIntegerFields;
	}

	public List<FieldInfo> getSimpleDateFields() {
		return simpleDateFields;
	}

	public String randomString(int size){
		return RandomStringUtils.randomAlphabetic(size);
	}

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
	
	public BigDecimal randomDecimal(){
		return BigDecimal.valueOf(random.nextLong());
	}

	public List<FieldInfo> getSimpleBooleanFields() {
		return simpleBooleanFields;
	}

	public List<FieldInfo> getSimpleDoubleFields() {
		return simpleDoubleFields;
	}

	public List<FieldInfo> getSimpleFloatFields() {
		return simpleFloatFields;
	}
	public List<FieldInfo> getSimpleBigDecimalFields() {
		return simpleBigDecimalFields;
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
	
	public boolean containsField(FieldInfo candidate){
		boolean b = randomSimpleFields.contains(candidate);
		return b;
	}

	public List<FieldInfo> getAllSimpleFields() {
		return allSimpleFields;
	}

	public List<FieldInfo> getRandomSimpleFields() {
		return randomSimpleFields;
	}
	
	public FieldInfo getRandomSimpleField(){
		if(allSimpleFields.isEmpty()){
			if(!composed.isEmpty()) return composed.iterator().next();
			if(!aggregated.isEmpty()) return aggregated.iterator().next();
			throw new IllegalStateException("Entity "+ entity.getQualifiedName() + " has no fields.");
		}
		Collections.shuffle(allSimpleFields);
		return allSimpleFields.iterator().next();
	}
	
	public String getFieldType(String fieldName){
		FieldInfo simpleField = getSimpleField(fieldName);
		if(simpleField!=null) return simpleField.getType();
		for (FieldInfo fieldInfo : aggregated) {
			if(fieldInfo.getName().equals(fieldName)) return fieldInfo.getType();
		}
		for (FieldInfo fieldInfo : composed) {
			if(fieldInfo.getName().equals(fieldName)) return fieldInfo.getType();
		}
		return null;
	}

	public FieldInfo getSimpleField(String fieldName){
		for (FieldInfo field : allSimpleFields) {
			if(field.getField().getName().equals(fieldName)) return field;
		}
		return null;
	}
	
	/**
	 * Will be call to add this field to the list of imports if we cast the field in the test class.
	 * @param fieldName
	 */
	private final List<String> simpleFieldTypeImport = new ArrayList<String>();
	public void importIfRequired(String fieldName){
		FieldInfo simpleField = getSimpleField(fieldName);
		if(simpleField==null){
			return;
		}
		String qualifiedType = simpleField.getQualifiedType();
		if(simpleFieldTypeImport.contains(qualifiedType)) return;
		if(qualifiedType.startsWith("java.lang"))return;
		simpleFieldTypeImport.add(qualifiedType);
		return;
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

	public Set<String> getPackageImport() {
		return packageImport;
	}

	public Set<String> getComposedTypes() {
		return composedTypes;
	}

	public Set<String> getComposedCollectionTypes() {
		return composedCollectionTypes;
	}

	public Set<String> getComposedTypesFQN() {
		return composedTypesFQN;
	}

	public Set<String> getComposedCollectionTypesFQN() {
		return composedCollectionTypesFQN;
	}

	public Set<String> getAggregatedTypes() {
		return aggregatedTypes;
	}

	public Set<String> getAggregatedCollectionTypes() {
		return aggregatedCollectionTypes;
	}

	public Set<String> getAggregatedTypesFQN() {
		return aggregatedTypesFQN;
	}

	public Set<String> getAggregatedCollectionTypesFQN() {
		return aggregatedCollectionTypesFQN;
	}

	public Random getRandom() {
		return random;
	}
	
	public Set<String> getReferencedTypes(){
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.addAll(aggregatedTypes);
		hashSet.addAll(aggregatedCollectionTypes);
		hashSet.addAll(composedTypes);
		hashSet.addAll(composedCollectionTypes);
		return hashSet;
	}

	public List<String> getReferencedTypesFQN() {
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.addAll(aggregatedTypesFQN);
		hashSet.addAll(aggregatedCollectionTypesFQN);
		hashSet.addAll(composedTypesFQN);
		hashSet.addAll(composedCollectionTypesFQN);
		return new ArrayList<String>(hashSet);
	}

	public List<FieldInfo> getFieldInfos() {
		return fieldInfos;
	}
	
	public List<String> getManagedCompositions(){
		HashSet<String> hashSet = new HashSet<String>();
		for (FieldInfo fieldInfo : composed) {
			if(fieldInfo.isAssociationManager()) hashSet.add(fieldInfo.getType());
		}
		return new ArrayList<String>(hashSet);
	}

	public Map<RelationKey, Relation> getRelationMap() {
		return relationMap;
	}
	
	public boolean isRelationship(){
		return entity.hasAnnotation(RelationshipTable.class);
	}
	
	public Collection<Relation> getRelations(){
		return relationMap.values();
	}
	
	

}
