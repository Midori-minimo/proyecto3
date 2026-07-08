package com.proyecto3.proyecto3.configuracion;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import com.proyecto3.proyecto3.contra.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminSeeder(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public void run(String... args) {
        String emailNormalizado = adminEmail.trim().toLowerCase();

        if (usuarioRepositorio.findByEmail(emailNormalizado).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail(emailNormalizado);

            String salt = PasswordUtil.generarSalt();
            admin.setSalt(salt);
            admin.setPasswordHash(PasswordUtil.hashPassword(adminPassword, salt));
            admin.setRol("ADMIN");

            usuarioRepositorio.save(admin);
            System.out.println("DEBUG >> Usuario ADMIN creado: " + emailNormalizado);
        }
    }
}

