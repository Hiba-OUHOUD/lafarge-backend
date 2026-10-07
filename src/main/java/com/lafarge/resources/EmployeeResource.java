package com.lafarge.resources;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.lafarge.services.MlRecommendationClient;
import com.lafarge.services.RecommendationService;
import java.util.stream.Collectors;
import com.lafarge.entities.Enrollment;
import com.lafarge.entities.Result;
import com.lafarge.entities.Notification;
import com.lafarge.entities.Training;
import com.lafarge.services.EnrollmentService;
import com.lafarge.services.TrainingService;
import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.ejb.Stateless;
import java.io.File;
import java.nio.file.Files;
import com.lafarge.entities.UserInterest;

@Stateless
@Path("/employee")

public class EmployeeResource {
	@EJB
	private RecommendationService recommendationService;

	@EJB
	private MlRecommendationClient mlClient;
    @EJB
    private EnrollmentService enrollmentService;

    @EJB
    private TrainingService trainingService;

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // ============================================================
    // 1. LISTE DES FORMATIONS DISPONIBLES POUR UN EMPLOYÉ
    // ============================================================
    @GET
    @Path("/trainings/available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableTrainings(@QueryParam("employeeId") Long employeeId) {
        try {
            com.lafarge.entities.User employee = em.find(com.lafarge.entities.User.class, employeeId);
            if (employee == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // 1. Formations publiées non suivies
            List<Training> allTrainings = trainingService.getAllTrainings();
            List<Training> published = allTrainings.stream()
                    .filter(t -> "PUBLISHED".equals(t.getStatus()))
                    .collect(Collectors.toList());

            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByEmployee(employeeId);
            List<Long> enrolledIds = enrollments.stream()
                    .map(e -> e.getTraining().getId())
                    .collect(Collectors.toList());

            List<Training> available = published.stream()
                    .filter(t -> !enrolledIds.contains(t.getId()))
                    .collect(Collectors.toList());

            // 2. Récupérer les domaines d'intérêt de l'employé (via user_interests)
            List<String> interestDomains = em.createQuery(
                    "SELECT ui.domain FROM UserInterest ui WHERE ui.user.id = :uid ORDER BY ui.clickCount DESC",
                    String.class)
                    .setParameter("uid", employeeId)
                    .getResultList();

            // Ajouter le domaine principal de l'employé
            if (employee.getDomain() != null && !interestDomains.contains(employee.getDomain())) {
                interestDomains.add(0, employee.getDomain()); // Priorité maximale
            }

            System.out.println(">>> Domaines d'intérêt pour user " + employeeId + " : " + interestDomains);

            // 3. Séparer : formations des domaines d'intérêt vs autres
            List<Training> recommended = available.stream()
                    .filter(t -> t.getDomain() != null && interestDomains.contains(t.getDomain()))
                    .collect(Collectors.toList());

            List<Training> others = available.stream()
                    .filter(t -> t.getDomain() == null || !interestDomains.contains(t.getDomain()))
                    .collect(Collectors.toList());

            // 4. Trier les recommandations par score ML (le ML trie en priorité)
            List<Long> mlRecommendedIds = mlClient.getRecommendations(employeeId, 10);

            if (!mlRecommendedIds.isEmpty()) {
                // Trier "recommended" selon l'ordre ML
                recommended.sort((t1, t2) -> {
                    int idx1 = mlRecommendedIds.indexOf(t1.getId());
                    int idx2 = mlRecommendedIds.indexOf(t2.getId());
                    if (idx1 == -1 && idx2 == -1) return 0;
                    if (idx1 == -1) return 1;
                    if (idx2 == -1) return -1;
                    return Integer.compare(idx1, idx2);
                });
                // Trier "others" aussi
                others.sort((t1, t2) -> {
                    int idx1 = mlRecommendedIds.indexOf(t1.getId());
                    int idx2 = mlRecommendedIds.indexOf(t2.getId());
                    if (idx1 == -1 && idx2 == -1) return 0;
                    if (idx1 == -1) return 1;
                    if (idx2 == -1) return -1;
                    return Integer.compare(idx1, idx2);
                });
            }

            // 5. Convertir en DTO
            Map<String, Object> response = new HashMap<>();
            response.put("recommended", recommended.stream().map(this::toTrainingDto).collect(Collectors.toList()));
            response.put("others", others.stream().map(this::toTrainingDto).collect(Collectors.toList()));

            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // 2. INSCRIPTION D'UN EMPLOYÉ À UNE FORMATION
    // ============================================================
    @POST
    @Path("/enroll/{trainingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enroll(@PathParam("trainingId") Long trainingId, 
                           @QueryParam("employeeId") Long employeeId) {
        try {
            Enrollment enrollment = enrollmentService.enrollEmployee(trainingId, employeeId);
            if (enrollment == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Inscription impossible (déjà inscrit ou formation/employé invalide)\"}")
                        .build();
            }
            return Response.ok("{\"message\":\"Inscription réussie\",\"enrollmentId\":" + enrollment.getId() + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // 3. MES FORMATIONS (avec progression)
    // ============================================================
    @GET
    @Path("/enrollments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyEnrollments(@QueryParam("employeeId") Long employeeId) {
        try {
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByEmployee(employeeId);
            List<Map<String, Object>> dtos = enrollments.stream()
                    .map(this::toEnrollmentDto)
                    .collect(Collectors.toList());
            return Response.ok(dtos).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
    // ============================================================
    // 4. METTRE À JOUR LA PROGRESSION
    // ============================================================
    @PUT
    @Path("/progress/{enrollmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProgress(@PathParam("enrollmentId") Long enrollmentId, 
                                   @QueryParam("progress") int progress) {
        try {
            Enrollment updated = enrollmentService.updateProgress(enrollmentId, progress);
            if (updated == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Progression invalide\"}")
                        .build();
            }
            return Response.ok("{\"message\":\"Progression mise à jour\",\"progress\":" + updated.getProgress() + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // 5. RÉCUPÉRER UNE INSCRIPTION PAR ID (pour la page d'apprentissage)
    // ============================================================
    @GET
    @Path("/enrollment/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnrollment(@PathParam("id") Long id) {
        try {
            Enrollment enrollment = enrollmentService.findById(id);
            if (enrollment == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(toEnrollmentDto(enrollment)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // 6. NOTIFICATIONS
    // ============================================================
    @GET
    @Path("/notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotifications(@QueryParam("employeeId") Long employeeId) {
        try {
            List<Notification> notifs = em.createQuery(
                "SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC", Notification.class)
                .setParameter("userId", employeeId)
                .getResultList();
            return Response.ok(notifs).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // 7. MARQUER UNE NOTIFICATION COMME LUE
    // ============================================================
    @PUT
    @Path("/notifications/{id}/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response markAsRead(@PathParam("id") Long notificationId) {
        try {
            Notification notif = em.find(Notification.class, notificationId);
            if (notif == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Notification non trouvée\"}")
                        .build();
            }
            notif.setRead(true);
            em.merge(notif);
            return Response.ok("{\"message\":\"Notification marquée comme lue\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
 // 8. Récupérer les chapitres d'une formation (converti en DTO)
    @GET
    @Path("/training/{trainingId}/chapters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChapters(@PathParam("trainingId") Long trainingId) {
        try {
            List<com.lafarge.entities.TrainingChapter> chapters = 
                enrollmentService.getChaptersByTrainingId(trainingId);
            
            // ✅ Convertir en DTO pour éviter les problèmes de sérialisation
            List<Map<String, Object>> dtos = chapters.stream().map(c -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", c.getId());
                dto.put("title", c.getTitle());
                dto.put("content", c.getContent());
                dto.put("chapterOrder", c.getChapterOrder());
                return dto;
            }).collect(Collectors.toList());
            
            return Response.ok(dtos).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // 9. Récupérer les chapitres déjà complétés d'une inscription
    @GET
    @Path("/enrollment/{enrollmentId}/chapters-completed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompletedChapters(@PathParam("enrollmentId") Long enrollmentId) {
        try {
            List<Long> completed = enrollmentService.getCompletedChapterIds(enrollmentId);
            return Response.ok(completed).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    // 10. Marquer un chapitre comme terminé
    @POST
    @Path("/enrollment/{enrollmentId}/chapter/{chapterId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeChapter(@PathParam("enrollmentId") Long enrollmentId,
                                    @PathParam("chapterId") Long chapterId) {
        try {
            enrollmentService.completeChapter(enrollmentId, chapterId);
            Enrollment enrollment = enrollmentService.findById(enrollmentId);
            return Response.ok("{\"message\":\"Chapitre terminé\",\"progress\":" 
                    + enrollment.getProgress() + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
 // ============================================================
 // 11. TÉLÉCHARGER LE CERTIFICAT
 // ============================================================
 @GET
 @Path("/certificate/{enrollmentId}/download")
 @Produces("application/pdf")
 public Response downloadCertificate(@PathParam("enrollmentId") Long enrollmentId) {
     try {
         // 1. Récupérer le certificat lié à cette inscription
         Result result = em.createQuery(
             "SELECT r FROM Result r WHERE r.enrollment.id = :eid AND r.passed = true ORDER BY r.completedDate DESC", 
             Result.class)
             .setParameter("eid", enrollmentId)
             .setMaxResults(1)
             .getSingleResult();

         if (result == null || result.getCertificateUrl() == null) {
             return Response.status(Response.Status.NOT_FOUND)
                     .entity("{\"error\":\"Aucun certificat trouvé pour cette inscription\"}")
                     .build();
         }

         // 2. Lire le fichier PDF
         java.io.File pdfFile = new java.io.File(result.getCertificateUrl());
         if (!pdfFile.exists()) {
             return Response.status(Response.Status.NOT_FOUND)
                     .entity("{\"error\":\"Fichier PDF introuvable sur le serveur\"}")
                     .build();
         }

         // 3. Renvoyer le fichier
         return Response.ok(pdfFile)
                 .header("Content-Disposition", "attachment; filename=\"certificate_" + enrollmentId + ".pdf\"")
                 .build();

     } catch (jakarta.persistence.NoResultException e) {
         return Response.status(Response.Status.NOT_FOUND)
                 .entity("{\"error\":\"Aucun certificat trouvé\"}").build();
     } catch (Exception e) {
         e.printStackTrace();
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                 .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
     }
 }
 private Map<String, Object> toTrainingDto(Training t) {
	    Map<String, Object> dto = new HashMap<>();
	    dto.put("id", t.getId());
	    dto.put("title", t.getTitle());
	    dto.put("description", t.getDescription());
	    dto.put("content", t.getContent());
	    dto.put("status", t.getStatus());
	    dto.put("domain", t.getDomain());
	    dto.put("createdAt", t.getCreatedAt());

	    if (t.getTrainer() != null) {
	        Map<String, Object> trainerDto = new HashMap<>();
	        trainerDto.put("id", t.getTrainer().getId());
	        trainerDto.put("firstName", t.getTrainer().getFirstName());
	        trainerDto.put("lastName", t.getTrainer().getLastName());
	        dto.put("trainer", trainerDto);
	    } else {
	        dto.put("trainer", null);
	    }
	    return dto;
	}
 private Map<String, Object> toEnrollmentDto(Enrollment e) {
	    Map<String, Object> dto = new HashMap<>();
	    dto.put("id", e.getId());
	    dto.put("progress", e.getProgress());
	    dto.put("status", e.getStatus());
	    dto.put("enrollmentDate", e.getEnrollmentDate());
	    dto.put("training", toTrainingDto(e.getTraining()));
	    return dto;
	}
 @POST
 @Path("/track-click")
 @Consumes(MediaType.APPLICATION_JSON)
 @Produces(MediaType.APPLICATION_JSON)
 public Response trackClick(Map<String, Object> body) {
     try {
         Long userId = Long.parseLong(body.get("userId").toString());
         String domain = (String) body.get("domain");
         
         if (domain != null) {
             recommendationService.trackClick(userId, domain);
         }
         
         return Response.ok("{\"message\":\"Click enregistré\"}").build();
     } catch (Exception e) {
         e.printStackTrace();
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                 .entity("{\"error\":\"" + e.getMessage() + "\"}")
                 .build();
     }
 }
    
}