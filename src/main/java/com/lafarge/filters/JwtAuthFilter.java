package com.lafarge.filters;

import com.lafarge.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.annotation.Priority;

import java.io.IOException;
import java.security.Principal;

@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Routes publiques
        if (normalizedPath.startsWith("auth/") || normalizedPath.startsWith("verify-certificate")) {
            return;
        }

        // Récupérer le token
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(requestContext, "Token manquant");
            return;
        }

        String token = authHeader.substring(7);
        Claims claims = JwtService.validateToken(token);  // ← STATIC CALL

        if (claims == null) {
            abort(requestContext, "Token invalide ou expiré");
            return;
        }

        Long userId = JwtService.getUserId(claims);      // ← STATIC CALL
        String role = JwtService.getRole(claims);        // ← STATIC CALL

        if (!isAuthorized(normalizedPath, role)) {
            abort(requestContext, "Accès refusé pour votre rôle");
            return;
        }

        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> userId.toString();
            }
            @Override
            public boolean isUserInRole(String r) {
                return role.equals(r);
            }
            @Override
            public boolean isSecure() {
                return requestContext.getUriInfo().getAbsolutePath().toString().startsWith("https");
            }
            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });
    }

    private boolean isAuthorized(String path, String role) {
        if (path.startsWith("admin/") && !"ADMIN".equals(role)) return false;
        if (path.startsWith("employee/") && !"EMPLOYEE".equals(role)) return false;
        if (path.startsWith("trainer/") && !"TRAINER".equals(role)) return false;
        return true;
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}