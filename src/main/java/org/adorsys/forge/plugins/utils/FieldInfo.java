package org.adorsys.forge.plugins.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;

public class FieldInfo {

	private Field<JavaClass> field;
	
	private String fieldType;
	
	private String fieldTypeFQN;
	
	private JavaClass targetEntity;
	
	private boolean collection;

	private boolean simpleField;
	
	private String sipmleFieldType;
	
	private boolean associationManager;
	
	private String displayedFields;
	
	private String mappedBy;
	
	private Map<String, String> qualifiedDisplayFields = new HashMap<String, String>();
	
	public Field<JavaClass> getField() {
		return field;
	}

	public void setField(Field<JavaClass> field) {
		this.field = field;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public String getFieldTypeFQN() {
		return fieldTypeFQN;
	}

	public void setFieldTypeFQN(String fieldTypeFQN) {
		this.fieldTypeFQN = fieldTypeFQN;
	}

	public JavaClass getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(JavaClass targetEntity) {
		this.targetEntity = targetEntity;
	}

	public boolean isCollection() {
		return collection;
	}

	public void setCollection(boolean collection) {
		this.collection = collection;
	}

	public boolean isSimpleField() {
		return simpleField;
	}

	public void setSimpleField(boolean simpleField) {
		this.simpleField = simpleField;
	}

	public String getSipmleFieldType() {
		return sipmleFieldType;
	}

	public void setSipmleFieldType(String sipmleFieldType) {
		this.sipmleFieldType = sipmleFieldType;
	}

	public String getName(){
		return field.getName();
	}
	
	public String getType(){
		if(targetEntity!=null) return targetEntity.getName();
		return field.getType();
	}
	
	public String getQualifiedType(){
		if(targetEntity!=null) return targetEntity.getQualifiedName();
		return field.getQualifiedType();
	}

	public boolean isAssociationManager() {
		return associationManager;
	}

	public void setAssociationManager(boolean associationManager) {
		this.associationManager = associationManager;
	}

	public String getDisplayedFields() {
		return displayedFields;
	}

	public void setDisplayedFields(String displayedFields) {
		this.displayedFields = displayedFields;
	}

	public String getQualifiedDisplayedFields(String qualifier){
		if(qualifiedDisplayFields.containsKey(qualifier)) return qualifiedDisplayFields.get(qualifier);
		return displayedFields; 
	}
	
	public boolean hasDisplayFields(){
		return StringUtils.isNotBlank(displayedFields);
	}
		
	public boolean hasQualifiedDisplayFields(String qualifier){
		if(qualifiedDisplayFields.containsKey(qualifier)) return StringUtils.isNotBlank(qualifiedDisplayFields.get(qualifier));
		return StringUtils.isNotBlank(displayedFields);
	}
	
	public void putDisplayField(String qualifier, String displayFields){
		qualifiedDisplayFields.put(qualifier, displayFields);
	}
	public Collection<String> getQualifiers(){
		return qualifiedDisplayFields.keySet();
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}
	
	public boolean hasMappedBy(){
		return StringUtils.isNotBlank(this.mappedBy);
	}
}
