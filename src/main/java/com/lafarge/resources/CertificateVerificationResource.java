package com.lafarge.resources;

import com.lafarge.entities.Enrollment;
import com.lafarge.entities.Result;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Stateless
@Path("/verify-certificate")
public class CertificateVerificationResource {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    @GET
    @Path("/{enrollmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyCertificate(@PathParam("enrollmentId") Long enrollmentId) {
        try {
            // 1. Récupérer l'inscription
            Enrollment enrollment = em.find(Enrollment.class, enrollmentId);
            if (enrollment == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"valid\":false,\"error\":\"Certificat introuvable\"}")
                        .build();
            }

            // 2. Récupérer le résultat (quiz réussi)
            Result result = null;
            try {
                result = em.createQuery(
                    "SELECT r FROM Result r WHERE r.enrollment.id = :eid AND r.passed = true ORDER BY r.completedDate DESC",
                    Result.class)
                    .setParameter("eid", enrollmentId)
                    .setMaxResults(1)
                    .getSingleResult();
            } catch (Exception e) {
                // Pas de résultat
            }

            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"valid\":false,\"error\":\"Aucun certificat valide pour cette inscription\"}")
                        .build();
            }

            // 3. Construire la réponse
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("enrollmentId", enrollmentId);
            data.put("certificateNumber", "CERT-" + java.time.Year.now().getValue()
                    + "-" + String.format("%03d", enrollment.getTraining().getId())
                    + "-" + String.format("%03d", enrollment.getId()));
            data.put("employeeName", enrollment.getEmployee().getFirstName() + " " + enrollment.getEmployee().getLastName());
            data.put("employeePosition", enrollment.getEmployee().getPosition());
            data.put("trainingTitle", enrollment.getTraining().getTitle());
            data.put("trainingDomain", enrollment.getTraining().getDomain());
            data.put("score", result.getScore());
            data.put("completedDate", result.getCompletedDate() != null ? result.getCompletedDate().toString() : null);
            data.put("trainerName", enrollment.getTraining().getTrainer() != null
                    ? enrollment.getTraining().getTrainer().getFirstName() + " " + enrollment.getTraining().getTrainer().getLastName()
                    : "Direction Pédagogique");

            return Response.ok(data).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"valid\":false,\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}