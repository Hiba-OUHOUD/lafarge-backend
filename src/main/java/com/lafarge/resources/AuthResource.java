package com.lafarge.resources;

import com.lafarge.entities.User;
import com.lafarge.services.AuthService;
import com.lafarge.services.JwtService;
import com.lafarge.services.UserService;
import io.jsonwebtoken.Claims;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/auth")
public class AuthResource {

    @EJB
    private AuthService authService;

    @EJB
    private UserService userService;

    // ============================================================
    // LOGIN
    // ============================================================
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email et mot de passe requis\"}")
                    .build();
        }

        String token = authService.login(email, password);

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Email ou mot de passe incorrect\"}")
                    .build();
        }

        // Récupérer les infos utilisateur
        Claims claims = JwtService.validateToken(token);
        User user = authService.findById(JwtService.getUserId(claims));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", buildUserDto(user));

        return Response.ok(response).build();
    }

    // ============================================================
    // REGISTER (Inscription)
    // ============================================================
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(Map<String, String> body) {
        try {
            String firstName = body.get("firstName");
            String lastName = body.get("lastName");
            String email = body.get("email");
            String password = body.get("password");
            String domain = body.get("domain");
            String position = body.get("position");

            // Validation
            if (firstName == null || lastName == null || email == null || password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Tous les champs obligatoires doivent être remplis\"}")
                        .build();
            }

            if (password.length() < 6) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Le mot de passe doit contenir au moins 6 caractères\"}")
                        .build();
            }

            // Vérifier que l'email n'existe pas déjà
            User existing = userService.findByEmail(email);
            if (existing != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Cet email est déjà utilisé\"}")
                        .build();
            }

            // Créer le nouvel utilisateur
            User newUser = new User();
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setPassword(password);      // Sera hashé par UserService.addUser
            newUser.setRole("EMPLOYEE");        // Toujours EMPLOYEE par défaut
            newUser.setDomain(domain != null ? domain : "GENERAL");
            newUser.setPosition(position);

            userService.addUser(newUser);

            // ✅ CORRECTION : Appel STATIQUE (J majuscule)
            String token = JwtService.generateToken(
                newUser.getId(), 
                newUser.getEmail(), 
                newUser.getRole()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", buildUserDto(newUser));
            response.put("message", "Compte créé avec succès");

            return Response.status(Response.Status.CREATED).entity(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ============================================================
    // ME (utilisateur courant)
    // ============================================================
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response me(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token manquant\"}")
                    .build();
        }

        String token = authHeader.substring(7);
        Claims claims = JwtService.validateToken(token);

        if (claims == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token invalide ou expiré\"}")
                    .build();
        }

        User user = authService.findById(JwtService.getUserId(claims));
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(buildUserDto(user)).build();
    }

    // ============================================================
    // UTILITAIRE : DTO utilisateur (sans mot de passe)
    // ============================================================
    private Map<String, Object> buildUserDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole());
        dto.put("domain", user.getDomain());
        dto.put("position", user.getPosition());
        return dto;
    }
}