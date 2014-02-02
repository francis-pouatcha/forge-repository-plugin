package org.adorsys.forge.plugins.utils;

import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;

public class RelationEnd {
	private final String qualifier;
	private String displayFields;
	private Field<JavaClass> field;
	
	public RelationEnd(String qualifier) {
		this.qualifier = qualifier;
	}
	public String getDisplayFields() {
		return displayFields;
	}
	public void setDisplayFields(String displayFields) {
		this.displayFields = displayFields;
	}
	public String getQualifier() {
		return qualifier;
	}

	public Field<JavaClass> getField() {
		return field;
	}
	public void setField(Field<JavaClass> field) {
		this.field = field;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((qualifier == null) ? 0 : qualifier.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationEnd other = (RelationEnd) obj;
		if (qualifier == null) {
			if (other.qualifier != null)
				return false;
		} else if (!qualifier.equals(other.qualifier))
			return false;
		return true;
	}
	
}
