package com.lafarge.services;

import com.lafarge.entities.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class UserService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    @EJB
    private PasswordService passwordService;

    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public void addUser(User user) {
        // Hasher le mot de passe AVANT de le sauvegarder
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordService.hashPassword(user.getPassword()));
        }
        em.persist(user);
    }

    public User findByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                     .setParameter("email", email)
                     .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public User findById(Long id) {
        return em.find(User.class, id);
    }
    /**
     * Supprime un utilisateur et toutes ses dépendances.
     */
    public boolean deleteUser(Long userId) {
        try {
            User user = em.find(User.class, userId);
            if (user == null) {
                return false;
            }

            // 1. Supprimer les notifications
            em.createQuery("DELETE FROM Notification n WHERE n.user.id = :uid")
              .setParameter("uid", userId)
              .executeUpdate();

            // 2. Supprimer les intérêts
            em.createQuery("DELETE FROM UserInterest ui WHERE ui.user.id = :uid")
              .setParameter("uid", userId)
              .executeUpdate();

            // 3. Récupérer les enrollments de l'utilisateur
            List<Long> enrollmentIds = em.createQuery(
                "SELECT e.id FROM Enrollment e WHERE e.employee.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getResultList();

            // 4. Pour chaque enrollment, supprimer les dépendances
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

            // 5. Supprimer les enrollments
            em.createQuery("DELETE FROM Enrollment e WHERE e.employee.id = :uid")
              .setParameter("uid", userId)
              .executeUpdate();

            // 6. Désassigner l'utilisateur comme formateur (mettre à null)
            em.createQuery("UPDATE Training t SET t.trainer = null WHERE t.trainer.id = :uid")
              .setParameter("uid", userId)
              .executeUpdate();

            // 7. Supprimer l'utilisateur
            em.remove(user);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}