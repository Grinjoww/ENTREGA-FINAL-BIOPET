package com.biopet.config;

import com.biopet.entity.Rol;
import com.biopet.entity.Usuario;
import com.biopet.repository.UsuarioRepository;
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
    CommandLineRunner seedAdmin(UsuarioRepository repo, PasswordEncoder enc, Environment env) {
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
                repo.save(Usuario.builder()
                        .nombre("Administrador BIOPET")
                        .email(email)
                        .passwordHash(enc.encode(password))
                        .rol(Rol.ROLE_ADMIN)
                        .activo(true)
                        .build());
            }
        };
    }
}