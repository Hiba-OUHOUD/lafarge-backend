package com.lafarge.services;

import com.lafarge.entities.User;
import com.lafarge.entities.UserInterest;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class RecommendationService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    /**
     * Enregistre un clic sur une formation → augmente le compteur d'intérêt pour son domaine.
     */
    public void trackClick(Long userId, String domain) {
        if (domain == null || domain.isEmpty()) return;

        User user = em.find(User.class, userId);
        if (user == null) return;

        List<UserInterest> existing = em.createQuery(
                "SELECT ui FROM UserInterest ui WHERE ui.user.id = :uid AND ui.domain = :domain",
                UserInterest.class)
                .setParameter("uid", userId)
                .setParameter("domain", domain)
                .getResultList();

        if (!existing.isEmpty()) {
            UserInterest ui = existing.get(0);
            ui.setClickCount(ui.getClickCount() + 1);
            ui.setLastClickAt(LocalDateTime.now());
            em.merge(ui);
            System.out.println(">>> Intérêt accru pour user " + userId + " sur domaine " + domain 
                    + " (total: " + ui.getClickCount() + " clics)");
        } else {
            UserInterest ui = new UserInterest();
            ui.setUser(user);
            ui.setDomain(domain);
            ui.setClickCount(1);
            ui.setLastClickAt(LocalDateTime.now());
            em.persist(ui);
            System.out.println(">>> Nouvel intérêt pour user " + userId + " sur domaine " + domain);
        }
    }
}
