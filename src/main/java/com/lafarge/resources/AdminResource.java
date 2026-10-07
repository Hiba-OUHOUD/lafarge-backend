package com.lafarge.resources;

import com.lafarge.entities.User;

import com.lafarge.services.UserService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;

@Path("/admin")
public class AdminResource {

    @EJB
    private UserService userService;

    // 1. Lister tous les utilisateurs (GET /api/admin/users)
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers() {
        List<User> users = userService.getAllUsers();
        return Response.ok(users).build();
    }

    // 2. Ajouter un employé ou un formateur (POST /api/admin/users)
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUser(User user) {
        // On vérifie si l'email existe déjà
        User existing = userService.findByEmail(user.getEmail());
        if (existing != null) {
            return Response.status(Response.Status.CONFLICT)
                           .entity("{\"error\":\"Cet email existe déjà\"}")
                           .build();
        }
        userService.addUser(user);
        return Response.status(Response.Status.CREATED)
                       .entity("{\"message\":\"Utilisateur créé avec succès\"}")
                       .build();
    }
    @DELETE
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("id") Long id) {
        try {
            boolean success = userService.deleteUser(id);
            if (!success) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Utilisateur introuvable\"}")
                        .build();
            }
            return Response.ok("{\"message\":\"Utilisateur supprimé avec succès\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
