package com.lafarge.config;

import com.lafarge.filters.JwtAuthFilter;
import com.lafarge.resources.AdminResource;
import com.lafarge.resources.AdminStatsResource;
import com.lafarge.resources.AuthResource;
import com.lafarge.resources.CertificateVerificationResource;
import com.lafarge.resources.EmployeeQuizResource;
import com.lafarge.resources.EmployeeResource;  // <-- VERIFIEZ CETTE LIGNE
import com.lafarge.resources.TrainerResource;
import com.lafarge.resources.TrainingResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(AuthResource.class);
        classes.add(AdminResource.class);
        classes.add(TrainingResource.class);
        classes.add(EmployeeQuizResource.class);
        classes.add(CertificateVerificationResource.class);
        classes.add(com.lafarge.filters.CorsFilter.class);
        classes.add(TrainerResource.class);
        classes.add(JwtAuthFilter.class);
        classes.add(AdminStatsResource.class);
        classes.add(EmployeeResource.class);  // <-- CETTE LIGNE DOIT ÊTRE PRÉSENTE
        return classes;
    }
}