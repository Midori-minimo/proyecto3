package com.proyecto3.proyecto3.seguridad;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    @Autowired
    public UsuarioDetallesService(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase();
        Optional<Usuario> opt = usuarioRepositorio.findByEmail(emailNormalizado);
        if (opt.isEmpty()) {
            throw new UsernameNotFoundException("No existe una cuenta con ese correo: " + emailNormalizado);
        }
        return new UsuarioDetalles(opt.get());
    }
}
