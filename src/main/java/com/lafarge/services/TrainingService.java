package com.lafarge.services;

import com.lafarge.entities.Training;
import com.lafarge.entities.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class TrainingService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    // Créer une formation
    public void createTraining(Training training) {
        em.persist(training);
    }

    // Récupérer toutes les formations
    public List<Training> getAllTrainings() {
        return em.createQuery("SELECT t FROM Training t", Training.class).getResultList();
    }

    // Récupérer une formation par son ID
    public Training findById(Long id) {
        return em.find(Training.class, id);
    }

    // Assigner un formateur à une formation
    public void assignTrainer(Long trainingId, Long trainerId) {
        Training training = findById(trainingId);
        User trainer = em.find(User.class, trainerId);
        if (training != null && trainer != null) {
            training.setTrainer(trainer);
            em.merge(training);
        }
    }

    // Mettre à jour le contenu d'une formation (pour le formateur)
    public void updateContent(Long trainingId, String content) {
        Training training = findById(trainingId);
        if (training != null) {
            training.setContent(content);
            em.merge(training);
        }
    }

    // Publier ou dépublier une formation
    public void setStatus(Long trainingId, String status) {
        Training training = findById(trainingId);
        if (training != null) {
            training.setStatus(status);
            em.merge(training);
        }
    }
    public void createTraining(Training training, Long adminId) {
        User admin = em.find(User.class, adminId);
        if (admin != null) {
            training.setCreatedBy(admin);
        }
        em.persist(training);
    }
    /**
     * Supprime une formation et toutes ses dépendances.
     */
    public boolean deleteTraining(Long trainingId) {
        try {
            Training training = em.find(Training.class, trainingId);
            if (training == null) {
                return false;
            }

            // 1. Récupérer les enrollments de cette formation
            List<Long> enrollmentIds = em.createQuery(
                "SELECT e.id FROM Enrollment e WHERE e.training.id = :tid", Long.class)
                .setParameter("tid", trainingId)
                .getResultList();

            // 2. Pour chaque enrollment, supprimer les dépendances
            for (Long enrollmentId : enrollmentIds) {
                // Résultats
                em.createQuery("DELETE FROM Result r WHERE r.enrollment.id = :eid")
                  .setParameter("eid", enrollmentId)
                  .executeUpdate();

                // ChapterCompletions
                em.createQuery("DELETE FROM ChapterCompletion cc WHERE cc.enrollment.id = :eid")
                  .setParameter("eid", enrollmentId)
                  .executeUpdate();
            }

            // 3. Supprimer les enrollments
            em.createQuery("DELETE FROM Enrollment e WHERE e.training.id = :tid")
              .setParameter("tid", trainingId)
              .executeUpdate();

            // 4. Supprimer les questions du quiz
            Long quizId = null;
            try {
                quizId = em.createQuery(
                    "SELECT q.id FROM Quiz q WHERE q.training.id = :tid", Long.class)
                    .setParameter("tid", trainingId)
                    .getSingleResult();
            } catch (Exception ignored) {}

            if (quizId != null) {
                em.createQuery("DELETE FROM Question q WHERE q.quiz.id = :qid")
                  .setParameter("qid", quizId)
                  .executeUpdate();

                em.createQuery("DELETE FROM Quiz q WHERE q.id = :qid")
                  .setParameter("qid", quizId)
                  .executeUpdate();
            }

            // 5. Supprimer les chapitres
            em.createQuery("DELETE FROM TrainingChapter c WHERE c.training.id = :tid")
              .setParameter("tid", trainingId)
              .executeUpdate();

            // 6. Supprimer la formation
            em.remove(training);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}