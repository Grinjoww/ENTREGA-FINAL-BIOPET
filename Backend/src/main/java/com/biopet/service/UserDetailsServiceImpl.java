package com.biopet.service;

import com.biopet.entity.Usuario;
import com.biopet.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges BIOPET's {@link Usuario} entity to Spring Security's
 * {@link UserDetails}, used by the authentication provider to load
 * credentials and authorities during login.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Loads an active user's credentials and single authority (their
     * role) by email, for use by Spring Security's authentication
     * provider.
     *
     * @param email user's email, used as the Spring Security username
     * @return user details with username, password hash, role-based authority and enabled/disabled state
     * @throws UsernameNotFoundException if no active user exists with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
    }
}
