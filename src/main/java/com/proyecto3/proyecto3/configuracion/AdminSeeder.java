package com.proyecto3.proyecto3.configuracion;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminSeeder(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String emailNormalizado = adminEmail.trim().toLowerCase();
        Optional<Usuario> existente = usuarioRepositorio.findByEmail(emailNormalizado);

        String encoded = passwordEncoder.encode(adminPassword);
        String[] partes = encoded.split(":", 2);
        String hashNuevo = partes[0];
        String saltNuevo = partes[1];

        if (existente.isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail(emailNormalizado);
            admin.setRol("ADMIN");
            admin.setPasswordHash(hashNuevo);
            admin.setSalt(saltNuevo);
            usuarioRepositorio.save(admin);
            System.out.println("DEBUG >> Usuario ADMIN creado: " + emailNormalizado);
        } else {
            Usuario admin = existente.get();
            boolean coincide = passwordEncoder.matches(adminPassword, admin.getPasswordHash() + ":" + admin.getSalt());
            if (!coincide) {
                admin.setPasswordHash(hashNuevo);
                admin.setSalt(saltNuevo);
                usuarioRepositorio.save(admin);
                System.out.println("DEBUG >> Usuario ADMIN actualizado (password reseteada): " + emailNormalizado);
            } else {
                System.out.println("DEBUG >> Usuario ADMIN OK, password sin cambios: " + emailNormalizado);
            }
        }
    }
}
