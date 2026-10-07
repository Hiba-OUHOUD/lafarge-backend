package com.lafarge.resources;

import com.lafarge.entities.TrainingChapter;
import com.lafarge.services.TrainerService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Stateless
@Path("/trainer")
public class TrainerResource {

    @EJB
    private TrainerService trainerService;

    // 1. Statistiques du formateur
    @GET
    @Path("/{trainerId}/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats(@PathParam("trainerId") Long trainerId) {
        try {
            return Response.ok(trainerService.getStats(trainerId)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // 2. Liste des apprenants d'une formation
    @GET
    @Path("/training/{trainingId}/learners")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLearners(@PathParam("trainingId") Long trainingId) {
        try {
            return Response.ok(trainerService.getLearners(trainingId)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // 3. Ajouter un chapitre
    @POST
    @Path("/training/{trainingId}/chapter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addChapter(@PathParam("trainingId") Long trainingId, Map<String, String> body) {
        try {
            TrainingChapter c = trainerService.addChapter(trainingId, body.get("title"), body.get("content"));
            if (c == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Formation introuvable\"}").build();
            }
            return Response.ok("{\"message\":\"Chapitre ajouté\",\"id\":" + c.getId() + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // 4. Modifier un chapitre
    @PUT
    @Path("/chapter/{chapterId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChapter(@PathParam("chapterId") Long chapterId, Map<String, String> body) {
        try {
            TrainingChapter c = trainerService.updateChapter(chapterId, body.get("title"), body.get("content"));
            if (c == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Chapitre introuvable\"}").build();
            }
            return Response.ok("{\"message\":\"Chapitre mis à jour\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // 5. Supprimer un chapitre
    @DELETE
    @Path("/chapter/{chapterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteChapter(@PathParam("chapterId") Long chapterId) {
        try {
            trainerService.deleteChapter(chapterId);
            return Response.ok("{\"message\":\"Chapitre supprimé\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    @GET
    @Path("/training/{trainingId}/chapters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChapters(@PathParam("trainingId") Long trainingId) {
        try {
            List<com.lafarge.entities.TrainingChapter> chapters = 
                trainerService.getChaptersByTrainingId(trainingId);
            
            List<java.util.Map<String, Object>> dtos = chapters.stream().map(c -> {
                java.util.Map<String, Object> dto = new java.util.HashMap<>();
                dto.put("id", c.getId());
                dto.put("title", c.getTitle());
                dto.put("content", c.getContent());
                dto.put("chapterOrder", c.getChapterOrder());
                return dto;
            }).collect(java.util.stream.Collectors.toList());
            
            return Response.ok(dtos).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
