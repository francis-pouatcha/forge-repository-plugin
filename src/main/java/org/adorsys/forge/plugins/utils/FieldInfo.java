package org.adorsys.forge.plugins.utils;

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
}
