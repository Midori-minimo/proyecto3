package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.Usuario;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import com.proyecto3.proyecto3.contra.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/inicio";
    }
}

