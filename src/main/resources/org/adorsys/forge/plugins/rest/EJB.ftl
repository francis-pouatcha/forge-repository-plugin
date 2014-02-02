package org.adorsys.forge.plugins.rest;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;

@Stateless
public class ${entityName}EJB {

	@Inject
	private ${entityRepoName} repository;

	<#list entityInfo.getReferencedTypes() as referencedType>
	@Inject
   	private ${referencedType}Merger ${referencedType?uncap_first}Merger;

	</#list>

	public ${entityName} create(${entityName} entity) {
		return repository.save(attach(entity));
	}

	public ${entityName} deleteById(Long id) {
		${entityName} entity = repository.findBy(id);
		if (entity != null) {
			repository.remove(entity);
		}
		return entity;
	}

	public ${entityName} update(${entityName} entity) {
		return repository.save(attach(entity));
	}

	public ${entityName} findById(Long id) {
		return repository.findBy(id);
	}

	public List<${entityName}> listAll(int start, int max) {
		return repository.findAll(start, max);
	}

	public Long count() {
		return repository.count();
	}

	public List<${entityName}> findBy(${entityName} entity, int start, int max, SingularAttribute<${entityName}, ?>[] attributes) {
		return repository.findBy(entity,start, max, attributes);
	}

	public Long countBy(${entityName} entity, SingularAttribute<${entityName}, ?>[] attributes) {
		return repository.count(entity, attributes);
	}

	public List<${entityName}> findByLike(${entityName} entity,int start,int max,SingularAttribute<${entityName}, ?>[] attributes) {
		return repository.findByLike(entity, start, max, attributes);
	}

	public Long countByLike(${entityName} entity,SingularAttribute<${entityName}, ?>[] attributes) {
		return repository.countLike(entity, attributes);
	}
	
   private ${entityName} attach(${entityName} entity){
		if(entity==null) return null;
	   
	<#list entityInfo.getComposed() as field>
	   // composed
	   <#if field.hasMappedBy()>
	   entity.get${field.getName()?cap_first}().set${field.mappedBy?cap_first}(entity);
	   </#if>

	</#list>
	<#list entityInfo.getAggregated() as field>
	   // aggregated
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.bindAggregated(entity.get${field.getName()?cap_first}()));

	</#list>
	<#list entityInfo.composedCollections as field>
	   // composed collections
       Set<${field.getType()}> ${field.getName()} = entity.get${field.getName()?cap_first}();
       for (${field.getType()} ${field.getType()?uncap_first} : ${field.getName()}) {
    	  	${field.getType()?uncap_first}.set${field.mappedBy?cap_first}(entity);
       }

	</#list>
	<#list entityInfo.aggregatedCollections as field>
	   // aggregated collection
	   ${field.getType()?uncap_first}Merger.bindAggregated(entity.get${field.getName()?cap_first}());

	</#list>
	   return entity;
   }
}
