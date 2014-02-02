package org.adorsys.forge.plugins.utils;

public class RelationKey {

	private final String sourceQualifier;
	private final String targetQualifier;
	public RelationKey(String sourceQualifier, String targetQualifier) {
		super();
		this.sourceQualifier = sourceQualifier;
		this.targetQualifier = targetQualifier;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sourceQualifier == null) ? 0 : sourceQualifier.hashCode());
		result = prime * result
				+ ((targetQualifier == null) ? 0 : targetQualifier.hashCode());
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
		RelationKey other = (RelationKey) obj;
		if (sourceQualifier == null) {
			if (other.sourceQualifier != null)
				return false;
		} else if (!sourceQualifier.equals(other.sourceQualifier))
			return false;
		if (targetQualifier == null) {
			if (other.targetQualifier != null)
				return false;
		} else if (!targetQualifier.equals(other.targetQualifier))
			return false;
		return true;
	}
}
