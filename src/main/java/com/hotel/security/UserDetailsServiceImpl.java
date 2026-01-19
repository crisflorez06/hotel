package com.hotel.security;

import com.hotel.models.Usuario;
import com.hotel.repositories.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuarioAndActivoTrue(username);
        Usuario usuario = usuarioOpt.orElseThrow(
                () -> new UsernameNotFoundException("Usuario no encontrado o inactivo"));

        String role = "ROLE_" + usuario.getRol();
        return new User(
                usuario.getUsuario(),
                usuario.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
