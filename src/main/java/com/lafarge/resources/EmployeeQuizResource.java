package com.lafarge.resources;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.lafarge.entities.Enrollment;

import com.lafarge.entities.Notification;
import com.lafarge.entities.Question;
import com.lafarge.entities.Result;
import com.lafarge.services.CertificateGenerator;
import com.lafarge.services.EnrollmentService;
import com.lafarge.services.QuizService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Stateless
@Path("/employee/quiz")
public class EmployeeQuizResource {

    @EJB
    private QuizService quizService;

    @EJB
    private EnrollmentService enrollmentService;

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // 1. Récupérer les questions d'une formation
    @GET
    @Path("/questions/{trainingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuestions(@PathParam("trainingId") Long trainingId) {
        try {
            com.lafarge.entities.Quiz quiz = quizService.findByTrainingId(trainingId);
            if (quiz == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Aucun quiz pour cette formation\"}").build();
            }
            List<Question> questions = quizService.getQuestionsByQuizId(quiz.getId());
            
            // ✅ Convertir en DTO (sans le quiz)
            List<Map<String, Object>> dtos = questions.stream().map(q -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", q.getId());
                dto.put("questionText", q.getQuestionText());
                dto.put("optionA", q.getOptionA());
                dto.put("optionB", q.getOptionB());
                dto.put("optionC", q.getOptionC());
                dto.put("optionD", q.getOptionD());
                // On ne renvoie PAS correctAnswer (l'employé ne doit pas le voir)
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

    // 2. Soumettre le quiz
    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitQuiz(@QueryParam("enrollmentId") Long enrollmentId, Map<String, Map<String, String>> payload) {
        try {
            Enrollment enrollment = enrollmentService.findById(enrollmentId);
            if (enrollment == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Inscription invalide\"}").build();
            }

            Map<String, String> answers = payload.get("answers");
            if (answers == null || answers.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Aucune réponse fournie\"}").build();
            }

            Long trainingId = enrollment.getTraining().getId();
            com.lafarge.entities.Quiz quiz = quizService.findByTrainingId(trainingId);
            if (quiz == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Aucun quiz trouvé\"}").build();
            }

            List<Question> questions = quizService.getQuestionsByQuizId(quiz.getId());
            if (questions.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Aucune question trouvée\"}").build();
            }

            // Calcul du score
            int correctCount = 0;
            for (Question q : questions) {
                String userAnswer = answers.get(q.getId().toString());
                if (userAnswer != null && userAnswer.equalsIgnoreCase(q.getCorrectAnswer())) {
                    correctCount++;
                }
            }

            int total = questions.size();
            int score = (total > 0) ? (correctCount * 100 / total) : 0;
            boolean passed = score >= quiz.getPassingScore();

            // Enregistrer le résultat
            Result result = new Result();
            result.setEnrollment(enrollment);
            result.setScore(score);
            result.setPassed(passed);
            em.persist(result);
            em.flush();

            // Mettre à jour l'inscription en cas de réussite
            if (passed) {
                enrollment.setStatus("COMPLETED");
                enrollment.setProgress(100);
                em.merge(enrollment);
                em.flush();

                // ===== GÉNÉRATION DU CERTIFICAT =====
                String certPath = null;
                try {
                    int durationMinutes = enrollment.getTraining().getChapters() != null 
                        ? enrollment.getTraining().getChapters().size() * 15 
                        : 60;

                    String level = "Débutant";
                    String certificateNumber = "CERT-" + java.time.Year.now().getValue()
                        + "-" + String.format("%03d", enrollment.getTraining().getId())
                        + "-" + String.format("%03d", enrollment.getId());

                    certPath = CertificateGenerator.generateCertificate(
                        enrollment.getEmployee().getFirstName() + " " + enrollment.getEmployee().getLastName(),
                        enrollment.getTraining().getTitle(),
                        level,
                        durationMinutes + " min",
                        enrollment.getId(),
                        certificateNumber,
                        enrollment.getTraining().getTrainer() != null
                            ? enrollment.getTraining().getTrainer().getFirstName() + " "
                              + enrollment.getTraining().getTrainer().getLastName()
                            : "Direction Pédagogique"
                    );
                    System.out.println(">>> Certificat généré : " + certPath);
                } catch (Exception ce) {
                    System.err.println(">>> ERREUR certificat : " + ce.getMessage());
                    ce.printStackTrace();
                }

                if (certPath != null) {
                    result.setCertificateUrl(certPath);
                    em.merge(result);
                    em.flush();
                }

                // Créer une notification
                Notification notif = new Notification();
                notif.setUser(enrollment.getEmployee());
                notif.setMessage("Félicitations ! Vous avez réussi la formation avec un score de " + score + "%. "
                        + (certPath != null ? "Votre certificat est prêt." : ""));
                notif.setType("CERTIFICATE_READY");
                em.persist(notif);
            }

            return Response.ok("{\"message\":\"Quiz soumis avec succès\",\"score\":" + score
                    + ",\"passed\":" + passed + "}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}