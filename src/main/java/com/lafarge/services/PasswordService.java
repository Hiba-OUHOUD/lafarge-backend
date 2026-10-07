package com.lafarge.services;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.ejb.Stateless;

@Stateless
public class PasswordService {

    /**
     * Hashe un mot de passe en clair avec BCrypt.
     */
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Vérifie qu'un mot de passe en clair correspond à un hash BCrypt.
     */
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
