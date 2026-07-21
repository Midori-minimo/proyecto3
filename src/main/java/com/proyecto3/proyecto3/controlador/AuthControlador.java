package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import com.proyecto3.proyecto3.seguridad.UsuarioDetalles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthControlador {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthControlador(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String nombre,
                                   @RequestParam String email,
                                   @RequestParam String password,
                                   @RequestParam String confirmarPassword,
                                   Model modelo) {

        String emailNormalizado = email.trim().toLowerCase();

        if (!password.equals(confirmarPassword)) {
            modelo.addAttribute("error", "Las contraseñas no coinciden.");
            modelo.addAttribute("nombreIngresado", nombre);
            modelo.addAttribute("emailIngresado", email);
            return "registro";
        }

        if (password.length() < 6) {
            modelo.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            modelo.addAttribute("nombreIngresado", nombre);
            modelo.addAttribute("emailIngresado", email);
            return "registro";
        }

        if (usuarioRepositorio.findByEmail(emailNormalizado).isPresent()) {
            modelo.addAttribute("error", "Ya existe una cuenta registrada con ese correo.");
            modelo.addAttribute("nombreIngresado", nombre);
            return "registro";
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(nombre.trim());
        nuevoUsuario.setEmail(emailNormalizado);
        nuevoUsuario.setRol("CLIENTE");

        String encoded = passwordEncoder.encode(password);
        String[] partes = encoded.split(":", 2);
        nuevoUsuario.setPasswordHash(partes[0]);
        nuevoUsuario.setSalt(partes[1]);

        usuarioRepositorio.save(nuevoUsuario);

        return "redirect:/login?registrado";
    }

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "paso", required = false) String paso,
            Model modelo) {

        if ("2".equals(paso)) {
            String emailNorm = email == null ? "" : email.trim().toLowerCase();
            if (emailNorm.isEmpty() || usuarioRepositorio.findByEmail(emailNorm).isEmpty()) {
                modelo.addAttribute("error",
                        "La cuenta ya no existe. Vuelve a iniciar el proceso de recuperación.");
                return "redirect:/recuperar-password";
            }
            modelo.addAttribute("paso", 2);
            modelo.addAttribute("email", emailNorm);
            return "recuperar-password";
        }

        modelo.addAttribute("paso", 1);
        return "recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String procesarRecuperarPassword(
            @RequestParam(value = "paso", required = false) String paso,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmarPassword", required = false) String confirmarPassword,
            Model modelo) {

        if ("2".equals(paso) && email != null && !email.isBlank()) {
            String emailNorm = email.trim().toLowerCase();
            Optional<Usuario> usuarioOpt = usuarioRepositorio.findByEmail(emailNorm);

            if (usuarioOpt.isEmpty()) {
                modelo.addAttribute("error",
                        "La cuenta ya no existe. Vuelve a iniciar el proceso de recuperación.");
                modelo.addAttribute("paso", 1);
                return "recuperar-password";
            }

            if (password == null || !password.equals(confirmarPassword)) {
                modelo.addAttribute("error", "Las contraseñas no coinciden.");
                modelo.addAttribute("paso", 2);
                modelo.addAttribute("email", emailNorm);
                return "recuperar-password";
            }

            if (password.length() < 6) {
                modelo.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
                modelo.addAttribute("paso", 2);
                modelo.addAttribute("email", emailNorm);
                return "recuperar-password";
            }

            Usuario usuario = usuarioOpt.get();
            String encoded = passwordEncoder.encode(password);
            String[] partes = encoded.split(":", 2);
            usuario.setPasswordHash(partes[0]);
            usuario.setSalt(partes[1]);
            usuarioRepositorio.save(usuario);

            return "redirect:/login?recuperada";
        }

        if (email == null || email.isBlank()) {
            modelo.addAttribute("error", "Debes ingresar un correo.");
            modelo.addAttribute("paso", 1);
            return "recuperar-password";
        }

        String emailNorm = email.trim().toLowerCase();
        if (usuarioRepositorio.findByEmail(emailNorm).isEmpty()) {
            modelo.addAttribute("error",
                    "No encontramos una cuenta registrada con ese correo.");
            modelo.addAttribute("emailIngresado", email);
            modelo.addAttribute("paso", 1);
            return "recuperar-password";
        }

        String url = "/recuperar-password?paso=2&email="
                + URLEncoder.encode(emailNorm, StandardCharsets.UTF_8);
        return "redirect:" + url;
    }
}
