package com.lafarge.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.*;

@Stateless
public class AdminStatsService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    public Map<String, Object> getAllStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Formations par domaine
        List<Object[]> domainRows = em.createQuery(
            "SELECT t.domain, COUNT(t) FROM Training t GROUP BY t.domain", Object[].class)
            .getResultList();
        Map<String, Long> byDomain = new LinkedHashMap<>();
        for (Object[] row : domainRows) {
            byDomain.put(row[0] != null ? row[0].toString() : "GENERAL", (Long) row[1]);
        }
        stats.put("formationsByDomain", byDomain);

        // 2. Utilisateurs par rôle
        List<Object[]> roleRows = em.createQuery(
            "SELECT u.role, COUNT(u) FROM User u GROUP BY u.role", Object[].class)
            .getResultList();
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Object[] row : roleRows) {
            byRole.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("usersByRole", byRole);

        // 3. Inscriptions par statut
        List<Object[]> statusRows = em.createQuery(
            "SELECT e.status, COUNT(e) FROM Enrollment e GROUP BY e.status", Object[].class)
            .getResultList();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : statusRows) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("enrollmentsByStatus", byStatus);

        // 4. Inscriptions par mois (6 derniers mois)
        List<Object[]> monthRows = em.createQuery(
            "SELECT FUNCTION('DATE_FORMAT', e.enrollmentDate, '%Y-%m'), COUNT(e) " +
            "FROM Enrollment e GROUP BY FUNCTION('DATE_FORMAT', e.enrollmentDate, '%Y-%m') " +
            "ORDER BY FUNCTION('DATE_FORMAT', e.enrollmentDate, '%Y-%m')", Object[].class)
            .getResultList();
        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (Object[] row : monthRows) {
            byMonth.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("enrollmentsByMonth", byMonth);

        // 5. Top 5 formations les plus suivies
        List<Object[]> topRows = em.createQuery(
            "SELECT e.training.title, COUNT(e) FROM Enrollment e " +
            "GROUP BY e.training.title ORDER BY COUNT(e) DESC", Object[].class)
            .setMaxResults(5)
            .getResultList();
        List<Map<String, Object>> topTrainings = new ArrayList<>();
        for (Object[] row : topRows) {
            Map<String, Object> m = new HashMap<>();
            m.put("title", row[0]);
            m.put("count", row[1]);
            topTrainings.add(m);
        }
        stats.put("topTrainings", topTrainings);

        // 6. Taux de réussite aux quiz
        Long totalResults = em.createQuery(
            "SELECT COUNT(r) FROM Result r", Long.class).getSingleResult();
        Long passedResults = em.createQuery(
            "SELECT COUNT(r) FROM Result r WHERE r.passed = true", Long.class).getSingleResult();
        Map<String, Long> quizStats = new LinkedHashMap<>();
        quizStats.put("passed", passedResults);
        quizStats.put("failed", totalResults - passedResults);
        stats.put("quizResults", quizStats);

        // 7. Totaux globaux
        stats.put("totalUsers", em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult());
        stats.put("totalTrainings", em.createQuery("SELECT COUNT(t) FROM Training t", Long.class).getSingleResult());
        stats.put("totalEnrollments", em.createQuery("SELECT COUNT(e) FROM Enrollment e", Long.class).getSingleResult());
        stats.put("totalCertificates", passedResults);

        return stats;
    }
}