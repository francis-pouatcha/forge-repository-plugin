package org.adorsys.forge.plugins.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Path("${resourcePath}")
public class ${entityEndpointName} {

	@Inject
	private ${entityName}EJB ejb;

	<#list entityInfo.getReferencedTypes() as referencedType>
	@Inject
   	private ${referencedType}Merger ${referencedType?uncap_first}Merger;

	</#list>
	@POST
	@Consumes({ "application/json", "application/xml" })
	@Produces({ "application/json", "application/xml" })
	public ${entityName} create(${entityName} entity) {
		return detach(ejb.create(entity));
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
   	public Response deleteById(@PathParam("id") ${idType} id){
		${entityName} deleted = ejb.deleteById(id);
		if (deleted == null)
			return Response.status(Status.NOT_FOUND).build();

		return Response.ok(detach(deleted)).build();
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Produces({ "application/json", "application/xml" })
	@Consumes({ "application/json", "application/xml" })
	public ${entityName} update(${entityName} entity) {
		return detach(ejb.update(entity));
	}

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces({ "application/json", "application/xml" })
	public Response findById(@PathParam("id") Long id) {
		${entityName} found = ejb.findById(id);
		if (found == null)
			return Response.status(Status.NOT_FOUND).build();
		return Response.ok(detach(found)).build();
	}

	@GET
	@Produces({ "application/json", "application/xml" })
	public ${entityName}SearchResult listAll(@QueryParam("start") int start,
			@QueryParam("max") int max) {
		List<${entityName}> resultList = ejb.listAll(start, max);
		${entityName}SearchInput searchInput = new ${entityName}SearchInput();
		searchInput.setStart(start);
		searchInput.setMax(max);
		return new ${entityName}SearchResult((long) resultList.size(),
				detach(resultList), detach(searchInput));
	}

	@GET
	@Path("/count")
	public Long count() {
		return ejb.count();
	}

	@POST
	@Path("/findBy")
	@Produces({ "application/json", "application/xml" })
	@Consumes({ "application/json", "application/xml" })
   	public ${entityName}SearchResult findBy(${entityName}SearchInput searchInput)
   	{
		SingularAttribute<${entityName}, ?>[] attributes = readSeachAttributes(searchInput);
		Long count = ejb.countBy(searchInput.getEntity(), attributes);
		List<${entityName}> resultList = ejb.findBy(searchInput.getEntity(),
				searchInput.getStart(), searchInput.getMax(), attributes);
		return new ${entityName}SearchResult(count, detach(resultList),
				detach(searchInput));
	}

	@POST
	@Path("/countBy")
	@Consumes({ "application/json", "application/xml" })
	public Long countBy(${entitySearchName} searchInput) {
		SingularAttribute<${entityName}, ?>[] attributes = readSeachAttributes(searchInput);
		return ejb.countBy(searchInput.getEntity(), attributes);
	}

	@POST
	@Path("/findByLike")
	@Produces({ "application/json", "application/xml" })
	@Consumes({ "application/json", "application/xml" })
   	public ${entityName}SearchResult findByLike(${entityName}SearchInput searchInput)
   	{
		SingularAttribute<${entityName}, ?>[] attributes = readSeachAttributes(searchInput);
		Long countLike = ejb.countByLike(searchInput.getEntity(), attributes);
		List<${entityName}> resultList = ejb.findByLike(searchInput.getEntity(),
				searchInput.getStart(), searchInput.getMax(), attributes);
		return new ${entityName}SearchResult(countLike, detach(resultList),
				detach(searchInput));
	}

	@POST
	@Path("/countByLike")
	@Consumes({ "application/json", "application/xml" })
	public Long countByLike(${entitySearchName} searchInput) {
		SingularAttribute<${entityName}, ?>[] attributes = readSeachAttributes(searchInput);
		return ejb.countByLike(searchInput.getEntity(), attributes);
	}

	@SuppressWarnings("unchecked")
	private SingularAttribute<${entityName}, ?>[] readSeachAttributes(
			${entitySearchName} searchInput) {
		List<String> fieldNames = searchInput.getFieldNames();
		List<SingularAttribute<${entityName}, ?>> result = new ArrayList<SingularAttribute<${entityName},?>>();
		for (String fieldName : fieldNames) {
			Field[] fields = ${entityName}_.class.getFields();
			for (Field field : fields) {
				if(field.getName().equals(fieldName)){
					try {
						result.add((SingularAttribute<${entityName}, ?>) field.get(null));
					} catch (IllegalArgumentException e) {
						throw new IllegalStateException(e);
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
		return result.toArray(new SingularAttribute[result.size()]);
	}
   	
   	private static final List<String> emptyList = Collections.emptyList();
	
	<#list entityInfo.getComposed() as field>
	<#if field.hasDisplayFields()>
	private static final List<String> ${field.getName()}Fields = Arrays.asList(${field.getDisplayedFields()});
	<#else>
	private static final List<String> ${field.getName()}Fields = emptyList;
	</#if>

	</#list>
	<#list entityInfo.getAggregatedUnqualified() as field>
	<#if field.hasDisplayFields()>
	private static final List<String> ${field.getName()}Fields = Arrays.asList(${field.getDisplayedFields()});
	<#else>
	private static final List<String> ${field.getName()}Fields = emptyList;
	</#if>

	</#list>
	<#list entityInfo.getAggregatedQualified() as field>
	<#list field.getQualifiers() as qualifier>
	<#if field.hasQualifiedDisplayFields(qualifier)>
	private static final List<String> ${qualifier}_${field.getName()}Fields = Arrays.asList(${field.getQualifiedDisplayedFields(qualifier)});
	<#else>
	private static final List<String> ${qualifier}_${field.getName()}Fields = emptyList;
	</#if>

	</#list>
	</#list>
	<#list entityInfo.composedCollections as field>
	<#if field.hasDisplayFields()>
	private static final List<String> ${field.getName()}Fields = Arrays.asList(${field.getDisplayedFields()});
	<#else>
	private static final List<String> ${field.getName()}Fields = emptyList;
	</#if>
	
	</#list>
	<#list entityInfo.aggregatedCollections as field>
	<#if field.hasDisplayFields()>
	private static final List<String> ${field.getName()}Fields = Arrays.asList(${field.getDisplayedFields()});
	<#else>
	private static final List<String> ${field.getName()}Fields = emptyList;
	</#if>
	
	</#list>
   private ${entityName} detach(${entityName} entity){
		if(entity==null) return null;
	   
	<#list entityInfo.getComposed() as field>
	   // composed
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.unbind(entity.get${field.getName()?cap_first}(), ${field.getName()}Fields));

	</#list>
	<#list entityInfo.getAggregatedUnqualified() as field>
	   // aggregated
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.unbind(entity.get${field.getName()?cap_first}(), ${field.getName()}Fields));

	</#list>
	<#list entityInfo.getAggregatedQualified() as field>
	<#list field.getQualifiers() as qualifier>
	   // aggregated
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.unbind(entity.get${field.getName()?cap_first}(), ${qualifier}_${field.getName()}Fields));

	</#list>
	</#list>
	<#list entityInfo.composedCollections as field>
	   // composed collections
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.unbind(entity.get${field.getName()?cap_first}(), ${field.getName()}Fields));

	</#list>
	<#list entityInfo.aggregatedCollections as field>
	   // aggregated collection
	   entity.set${field.getName()?cap_first}(${field.getType()?uncap_first}Merger.unbind(entity.get${field.getName()?cap_first}(), ${field.getName()}Fields));

	</#list>
	   return entity;
   }
   
	private List<${entityName}> detach(List<${entityName}> list) {
		if(list==null) return list;
		List<${entityName}> result = new ArrayList<${entityName}>();
		for (${entityName} entity : list) {
			result.add(detach(entity));
		}
		return result;
	}
	
	private ${entityName}SearchInput detach(${entityName}SearchInput searchInput){
		searchInput.setEntity(detach(searchInput.getEntity()));
		return searchInput;
	}
}