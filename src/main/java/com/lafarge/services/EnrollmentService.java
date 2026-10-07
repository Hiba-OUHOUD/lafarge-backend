package com.lafarge.services;

import com.lafarge.entities.Enrollment;
import com.lafarge.entities.Training;
import com.lafarge.entities.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import com.lafarge.entities.TrainingChapter;

import java.util.List;

import com.lafarge.entities.ChapterCompletion;

@Stateless
public class EnrollmentService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // Inscrire un employé à une formation
    public Enrollment enrollEmployee(Long trainingId, Long employeeId) {
        Training training = em.find(Training.class, trainingId);
        User employee = em.find(User.class, employeeId);

        if (training == null || employee == null) {
            return null;
        }

        // Vérifier si l'employé n'est pas déjà inscrit
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(e) FROM Enrollment e WHERE e.training.id = :trainingId AND e.employee.id = :employeeId", Long.class);
        query.setParameter("trainingId", trainingId);
        query.setParameter("employeeId", employeeId);
        if (query.getSingleResult() > 0) {
            return null; // Déjà inscrit
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setTraining(training);
        enrollment.setEmployee(employee);
        enrollment.setStatus("IN_PROGRESS");
        enrollment.setProgress(0);
        em.persist(enrollment);
        return enrollment;
    }

    // Récupérer les inscriptions d'un employé
    public List<Enrollment> getEnrollmentsByEmployee(Long employeeId) {
        return em.createQuery(
            "SELECT e FROM Enrollment e WHERE e.employee.id = :employeeId", Enrollment.class)
            .setParameter("employeeId", employeeId)
            .getResultList();
    }

    // Mettre à jour la progression
    public Enrollment updateProgress(Long enrollmentId, int progress) {
        Enrollment enrollment = em.find(Enrollment.class, enrollmentId);
        if (enrollment != null && progress >= 0 && progress <= 100) {
            enrollment.setProgress(progress);
            if (progress == 100) {
                enrollment.setStatus("COMPLETED");
            }
            em.merge(enrollment);
        }
        return enrollment;
    }

    // Récupérer une inscription par ID
    public Enrollment findById(Long id) {
        return em.find(Enrollment.class, id);
    }
 // Récupérer les chapitres d'une formation
    public List<TrainingChapter> getChaptersByTrainingId(Long trainingId) {
        return em.createQuery(
            "SELECT c FROM TrainingChapter c WHERE c.training.id = :trainingId ORDER BY c.chapterOrder ASC", 
            TrainingChapter.class)
            .setParameter("trainingId", trainingId)
            .getResultList();
    }

    // Récupérer les IDs des chapitres complétés d'une inscription
    public List<Long> getCompletedChapterIds(Long enrollmentId) {
        return em.createQuery(
            "SELECT cc.chapter.id FROM ChapterCompletion cc WHERE cc.enrollment.id = :enrollmentId", Long.class)
            .setParameter("enrollmentId", enrollmentId)
            .getResultList();
    }

    // Marquer un chapitre comme terminé et mettre à jour la progression
    public void completeChapter(Long enrollmentId, Long chapterId) {
        Enrollment enrollment = em.find(Enrollment.class, enrollmentId);
        TrainingChapter chapter = em.find(TrainingChapter.class, chapterId);

        if (enrollment == null || chapter == null) return;

        // Vérifier si déjà complété
        Long count = em.createQuery(
            "SELECT COUNT(cc) FROM ChapterCompletion cc WHERE cc.enrollment.id = :eid AND cc.chapter.id = :cid", Long.class)
            .setParameter("eid", enrollmentId)
            .setParameter("cid", chapterId)
            .getSingleResult();

        if (count == 0) {
            ChapterCompletion cc = new ChapterCompletion();
            cc.setEnrollment(enrollment);
            cc.setChapter(chapter);
            em.persist(cc);
        }

        // Recalculer la progression
        Long totalChapters = em.createQuery(
            "SELECT COUNT(c) FROM TrainingChapter c WHERE c.training.id = :tid", Long.class)
            .setParameter("tid", enrollment.getTraining().getId())
            .getSingleResult();

        Long completedChapters = em.createQuery(
            "SELECT COUNT(cc) FROM ChapterCompletion cc WHERE cc.enrollment.id = :eid", Long.class)
            .setParameter("eid", enrollmentId)
            .getSingleResult();

        if (totalChapters > 0) {
            int progress = (int) ((completedChapters * 100) / totalChapters);
            enrollment.setProgress(progress);
            
            em.merge(enrollment);
        }
    }
}
