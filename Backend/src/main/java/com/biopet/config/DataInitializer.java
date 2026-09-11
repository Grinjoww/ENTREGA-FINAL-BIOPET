package com.biopet.config;

import com.biopet.entity.Role;
import com.biopet.entity.User;
import com.biopet.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.seed-admin.enabled", havingValue = "true", matchIfMissing = false)
    CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder enc, Environment env) {
        return args -> {
            String email = env.getProperty("app.seed-admin.email");
            String password = env.getProperty("app.seed-admin.password");

            if (email == null || email.isBlank()) {
                throw new IllegalStateException("app.seed-admin.email no configurado; no se crea usuario semilla");
            }
            if (password == null || password.isBlank()) {
                throw new IllegalStateException("app.seed-admin.password no configurado; no se crea usuario semilla");
            }

            if (!repo.existsByEmail(email)) {
                repo.save(User.builder()
                        .nombre("Administrador BIOPET")
                        .email(email)
                        .passwordHash(enc.encode(password))
                        .rol(Role.ROLE_ADMIN)
                        .activo(true)
                        .build());
            }
        };
    }
}