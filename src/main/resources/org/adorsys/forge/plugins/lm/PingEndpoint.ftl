package org.adorsys.adpharma.server.lm;

import java.util.Date;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Ping authenticated
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Path("/ping")
public class PingEndpoint {

	@GET
	@Produces({ "application/json", "application/xml" })
	public String authping() {
		return new Date().toString();
	}
}