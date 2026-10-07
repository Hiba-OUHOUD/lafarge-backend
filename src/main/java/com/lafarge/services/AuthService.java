package com.lafarge.services;

import com.lafarge.entities.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class AuthService {

    @PersistenceContext(unitName = "LafargePU")
    private EntityManager em;

    @EJB
    private PasswordService passwordService;


    /**
     * Vérifie les identifiants et retourne un token JWT (ou null si échec).
     */
    public String login(String email, String password) {
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            User user = query.getSingleResult();

            // Vérifier le mot de passe
            boolean valid = false;
            
            // Cas 1 : mot de passe hashé (commence par $2a$, $2b$ ou $2y$)
            if (user.getPassword().startsWith("$2")) {
                valid = passwordService.checkPassword(password, user.getPassword());
            } else {
                // Cas 2 : ancien mot de passe en clair (pour la migration)
                valid = user.getPassword().equals(password);
                // Si valide, hasher le mot de passe pour la prochaine fois
                if (valid) {
                    user.setPassword(passwordService.hashPassword(password));
                    em.merge(user);
                }
            }

            if (!valid) {
                return null;
            }

            // Générer le JWT
            return JwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Récupère un utilisateur par ID (utile pour l'API "me").
     */
    public User findById(Long id) {
        return em.find(User.class, id);
    }
}