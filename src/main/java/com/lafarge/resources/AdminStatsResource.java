package com.lafarge.resources;

import com.lafarge.services.AdminStatsService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("/admin/stats")
public class AdminStatsResource {

    @EJB
    private AdminStatsService statsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        try {
            return Response.ok(statsService.getAllStats()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
