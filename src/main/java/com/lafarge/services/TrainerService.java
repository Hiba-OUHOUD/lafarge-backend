package com.lafarge.services;

import com.lafarge.entities.Enrollment;
import com.lafarge.entities.Result;
import com.lafarge.entities.Training;
import com.lafarge.entities.TrainingChapter;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;

@Stateless
public class TrainerService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // ============================================================
    // STATISTIQUES GLOBALES DU FORMATEUR
    // ============================================================
    public Map<String, Object> getStats(Long trainerId) {
        Map<String, Object> stats = new HashMap<>();

        Long totalTrainings = em.createQuery(
            "SELECT COUNT(t) FROM Training t WHERE t.trainer.id = :tid", Long.class)
            .setParameter("tid", trainerId).getSingleResult();

        Long totalLearners = em.createQuery(
            "SELECT COUNT(DISTINCT e.employee.id) FROM Enrollment e WHERE e.training.trainer.id = :tid", Long.class)
            .setParameter("tid", trainerId).getSingleResult();

        Double avgProgress = em.createQuery(
            "SELECT AVG(e.progress) FROM Enrollment e WHERE e.training.trainer.id = :tid", Double.class)
            .setParameter("tid", trainerId).getSingleResult();

        Long totalResults = em.createQuery(
            "SELECT COUNT(r) FROM Result r WHERE r.enrollment.training.trainer.id = :tid", Long.class)
            .setParameter("tid", trainerId).getSingleResult();

        Long passedResults = em.createQuery(
            "SELECT COUNT(r) FROM Result r WHERE r.enrollment.training.trainer.id = :tid AND r.passed = true", Long.class)
            .setParameter("tid", trainerId).getSingleResult();

        stats.put("totalTrainings", totalTrainings);
        stats.put("totalLearners", totalLearners);
        stats.put("avgProgress", avgProgress != null ? Math.round(avgProgress) : 0);
        stats.put("successRate", totalResults > 0 ? (passedResults * 100 / totalResults) : 0);
        stats.put("totalQuizzesTaken", totalResults);
        stats.put("totalQuizzesPassed", passedResults);
        return stats;
    }

    // ============================================================
    // LISTE DES APPRENANTS POUR UNE FORMATION
    // ============================================================
    public List<Map<String, Object>> getLearners(Long trainingId) {
        List<Enrollment> enrollments = em.createQuery(
            "SELECT e FROM Enrollment e WHERE e.training.id = :tid ORDER BY e.enrollmentDate DESC", Enrollment.class)
            .setParameter("tid", trainingId)
            .getResultList();

        List<Map<String, Object>> learners = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Map<String, Object> l = new HashMap<>();
            l.put("enrollmentId", e.getId());
            l.put("firstName", e.getEmployee().getFirstName());
            l.put("lastName", e.getEmployee().getLastName());
            l.put("email", e.getEmployee().getEmail());
            l.put("position", e.getEmployee().getPosition());
            l.put("progress", e.getProgress());
            l.put("status", e.getStatus());
            l.put("enrollmentDate", e.getEnrollmentDate());

            // Chercher le résultat du quiz
            try {
                Result r = em.createQuery(
                    "SELECT r FROM Result r WHERE r.enrollment.id = :eid ORDER BY r.completedDate DESC", Result.class)
                    .setParameter("eid", e.getId())
                    .setMaxResults(1)
                    .getSingleResult();
                l.put("score", r.getScore());
                l.put("passed", r.isPassed());
            } catch (Exception ex) {
                l.put("score", null);
                l.put("passed", null);
            }
            learners.add(l);
        }
        return learners;
    }

    // ============================================================
    // GESTION DES CHAPITRES
    // ============================================================
    public TrainingChapter addChapter(Long trainingId, String title, String content) {
        Training training = em.find(Training.class, trainingId);
        if (training == null) return null;

        // Calculer le prochain ordre
        Long count = em.createQuery(
            "SELECT COUNT(c) FROM TrainingChapter c WHERE c.training.id = :tid", Long.class)
            .setParameter("tid", trainingId)
            .getSingleResult();

        TrainingChapter chapter = new TrainingChapter();
        chapter.setTraining(training);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setChapterOrder(count.intValue() + 1);
        em.persist(chapter);
        return chapter;
    }

    public TrainingChapter updateChapter(Long chapterId, String title, String content) {
        TrainingChapter chapter = em.find(TrainingChapter.class, chapterId);
        if (chapter != null) {
            chapter.setTitle(title);
            chapter.setContent(content);
            em.merge(chapter);
        }
        return chapter;
    }

    public void deleteChapter(Long chapterId) {
        TrainingChapter chapter = em.find(TrainingChapter.class, chapterId);
        if (chapter != null) {
            em.remove(chapter);
        }
    }
    public List<com.lafarge.entities.TrainingChapter> getChaptersByTrainingId(Long trainingId) {
        return em.createQuery(
            "SELECT c FROM TrainingChapter c WHERE c.training.id = :tid ORDER BY c.chapterOrder ASC",
            com.lafarge.entities.TrainingChapter.class)
            .setParameter("tid", trainingId)
            .getResultList();
    }
}
