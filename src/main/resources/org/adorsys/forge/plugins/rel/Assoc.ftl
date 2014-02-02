package org.adorsys.forge.plugins.rest;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"source_id","target_id", "sourceQualifier","targetQualifier"}))
public class ${sourceEntity.name}${targetEntity.name}Assoc implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id = null;
	@Version
	@Column(name = "version")
	private int version = 0;

	@ManyToOne
	@NotNull
   	@JoinColumn(name="source_id")
	private ${sourceEntity.name} source;

	@ManyToOne
	@NotNull
   	@JoinColumn(name="target_id")
	private ${targetEntity.name} target;

	@NotNull
	@Column(name = "sourceQualifier")
	private String sourceQualifier;

	@Column(name = "targetQualifier")
	private String targetQualifier;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	public ${sourceEntity.name} getSource() {
		return source;
	}

	public void setSource(${sourceEntity.name} source) {
		this.source = source;
	}

	public ${targetEntity.name} getTarget() {
		return target;
	}

	public void setTarget(${targetEntity.name} target) {
		this.target = target;
	}

	public String getSourceQualifier() {
		return sourceQualifier;
	}

	public void setSourceQualifier(String sourceQualifier) {
		this.sourceQualifier = sourceQualifier;
	}

	public String getTargetQualifier() {
		return targetQualifier;
	}

	public void setTargetQualifier(String targetQualifier) {
		this.targetQualifier = targetQualifier;
	}

	public String getIdentifier(){
		return "s_"+(sourceQualifier==null?"":sourceQualifier.trim())+"_t_"+(targetQualifier==null?"":targetQualifier.trim());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result
				+ ((sourceQualifier == null) ? 0 : sourceQualifier.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		${sourceEntity.name}${targetEntity.name}Assoc other = (${sourceEntity.name}${targetEntity.name}Assoc) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (sourceQualifier == null) {
			if (other.sourceQualifier != null)
				return false;
		} else if (!sourceQualifier.equals(other.sourceQualifier))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (targetQualifier == null) {
			if (other.targetQualifier != null)
				return false;
		} else if (!targetQualifier.equals(other.targetQualifier))
			return false;
		return true;
	}
}
