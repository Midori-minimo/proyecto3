package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import com.proyecto3.proyecto3.contra.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthControlador {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email,
                                @RequestParam String password,
                                HttpSession sesion,
                                Model modelo) {

        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByEmail(email.trim().toLowerCase());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (PasswordUtil.verificarPassword(password, usuario.getSalt(), usuario.getPasswordHash())) {
                sesion.setAttribute("usuarioId", usuario.getId());
                sesion.setAttribute("usuarioNombre", usuario.getNombre());
                sesion.setAttribute("usuarioRol", usuario.getRol());
                return "redirect:/inicio";
            }
        }

        modelo.addAttribute("error", "Correo o contraseña incorrectos.");
        modelo.addAttribute("emailIngresado", email);
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

        String salt = PasswordUtil.generarSalt();
        nuevoUsuario.setSalt(salt);
        nuevoUsuario.setPasswordHash(PasswordUtil.hashPassword(password, salt));
        nuevoUsuario.setRol("CLIENTE");

        usuarioRepositorio.save(nuevoUsuario);

        return "redirect:/login?registrado";
    }

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "paso", required = false) String paso,
            Model modelo) {

        // Si llegamos al paso 2 pero el email no existe en la BD, volvemos al paso 1.
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

        // Paso 1 por defecto
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

        // ── Paso 2: cambiar la contraseña ──
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
            String saltNuevo = PasswordUtil.generarSalt();
            usuario.setSalt(saltNuevo);
            usuario.setPasswordHash(PasswordUtil.hashPassword(password, saltNuevo));
            usuarioRepositorio.save(usuario);

            return "redirect:/login?recuperada";
        }

        // ── Paso 1: validar el correo ──
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

        // El correo existe → ir al paso 2.
        // Usamos redirect para evitar reenvíos del formulario al refrescar.
        String url = "/recuperar-password?paso=2&email="
                + URLEncoder.encode(emailNorm, StandardCharsets.UTF_8);
        return "redirect:" + url;
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/inicio";
    }
}
