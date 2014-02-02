package org.adorsys.forge.plugins.utils;

public class Relation {

	private final SourceEnd sourceEnd;
	private final TargetEnd targetEnd;
	
	public Relation(SourceEnd sourceEnd, TargetEnd targetEnd) {
		this.sourceEnd = sourceEnd;
		this.targetEnd = targetEnd;
	}
	public SourceEnd getSourceEnd() {
		return sourceEnd;
	}
	public TargetEnd getTargetEnd() {
		return targetEnd;
	}

	public String getIdentifier(){
		return "s_"+(sourceEnd.getQualifier()==null?"":sourceEnd.getQualifier().trim())+"_t_"+(targetEnd.getQualifier()==null?"":targetEnd.getQualifier().trim());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sourceEnd == null) ? 0 : sourceEnd.hashCode());
		result = prime * result
				+ ((targetEnd == null) ? 0 : targetEnd.hashCode());
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
		Relation other = (Relation) obj;
		if (sourceEnd == null) {
			if (other.sourceEnd != null)
				return false;
		} else if (!sourceEnd.equals(other.sourceEnd))
			return false;
		if (targetEnd == null) {
			if (other.targetEnd != null)
				return false;
		} else if (!targetEnd.equals(other.targetEnd))
			return false;
		return true;
	}
	
}
