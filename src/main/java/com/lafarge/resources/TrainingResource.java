package com.lafarge.resources;

import com.lafarge.entities.Training;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import com.lafarge.services.TrainingService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Path("/trainings")
public class TrainingResource {

    @EJB
    private TrainingService trainingService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTrainings() {
        try {
            List<Training> trainings = trainingService.getAllTrainings();

            List<Map<String, Object>> dtos = trainings.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            return Response.ok(dtos).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTraining(Map<String, Object> body) {
        try {
            Training training = new Training();
            training.setTitle((String) body.get("title"));
            training.setDescription((String) body.get("description"));
            training.setContent((String) body.get("content"));
            training.setStatus("DRAFT");
            training.setDomain((String) body.get("domain"));

            // createdBy : récupérer l'utilisateur ID 1 (Admin) par défaut
            // (plus tard, on le lira depuis le token JWT)
            trainingService.createTraining(training, 1L);

            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\":\"Formation créée avec succès\"}")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}/publish")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(@PathParam("id") Long id) {
        try {
            trainingService.setStatus(id, "PUBLISHED");
            return Response.ok("{\"message\":\"Formation publiée\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}/assign/{trainerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignTrainer(@PathParam("id") Long id, @PathParam("trainerId") Long trainerId) {
        try {
            trainingService.assignTrainer(id, trainerId);
            return Response.ok("{\"message\":\"Formateur assigné\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // MÉTHODE DTO : Transforme une entité Training en Map JSON
    // (évite les boucles infinies et les LazyInitializationException)
    // ============================================================
    private Map<String, Object> toDto(Training t) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", t.getId());
        dto.put("title", t.getTitle());
        dto.put("description", t.getDescription());
        dto.put("content", t.getContent());
        dto.put("status", t.getStatus());
        dto.put("domain", t.getDomain());
        dto.put("createdAt", t.getCreatedAt());

        // Formateur (sans password)
        if (t.getTrainer() != null) {
            Map<String, Object> trainerDto = new HashMap<>();
            trainerDto.put("id", t.getTrainer().getId());
            trainerDto.put("firstName", t.getTrainer().getFirstName());
            trainerDto.put("lastName", t.getTrainer().getLastName());
            trainerDto.put("email", t.getTrainer().getEmail());
            trainerDto.put("position", t.getTrainer().getPosition());
            dto.put("trainer", trainerDto);
        } else {
            dto.put("trainer", null);
        }

        // Créateur (sans password)
        if (t.getCreatedBy() != null) {
            Map<String, Object> creatorDto = new HashMap<>();
            creatorDto.put("id", t.getCreatedBy().getId());
            creatorDto.put("firstName", t.getCreatedBy().getFirstName());
            creatorDto.put("lastName", t.getCreatedBy().getLastName());
            dto.put("createdBy", creatorDto);
        }

        return dto;
    }
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTraining(@PathParam("id") Long id) {
        try {
            boolean success = trainingService.deleteTraining(id);
            if (!success) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Formation introuvable\"}")
                        .build();
            }
            return Response.ok("{\"message\":\"Formation supprimée avec succès\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}