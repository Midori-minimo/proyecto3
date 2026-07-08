package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.Producto;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class TiendaControlador {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping({"", "inicio"})
    public String listarProductos(Model modelo, HttpSession sesion) {
        System.out.println("DEBUG >> Base de datos conectada: " + mongoTemplate.getDb().getName());
        System.out.println("DEBUG >> Colecciones disponibles: " + mongoTemplate.getCollectionNames());
        System.out.println("DEBUG >> Documentos en 'producto' (via driver crudo): " + mongoTemplate.getCollection("producto").countDocuments());

        List<Producto> producto = productoRepositorio.findAll();
        System.out.println("DEBUG >> Productos encontrados (via repositorio): " + producto.size());
        modelo.addAttribute("productos", producto);
        modelo.addAttribute("productoNuevo", new Producto());
        agregarDatosDeSesion(modelo, sesion);
        return "index";
    }

    @PostMapping("/comprar")
    public String comprarProducto(@RequestParam("productoId") String id) {
        Optional<Producto> opcional = productoRepositorio.findById(id);
        if (opcional.isPresent()) {
            Producto producto = opcional.get();
            if (producto.getCantidad() > 0) {
                producto.setCantidad(producto.getCantidad() - 1);
                productoRepositorio.save(producto);
            }
        }
        return "redirect:/inicio";
    }

    @PostMapping("/agregar")
    public String agregarProducto(@ModelAttribute Producto producto, HttpSession sesion) {
        if (!esAdmin(sesion)) {
            return "redirect:/login";
        }

        Optional<Producto> existente = productoRepositorio.findByNombre(producto.getNombre().trim());

        if (existente.isPresent()) {
            Producto productoExistente = existente.get();
            productoExistente.setCantidad(productoExistente.getCantidad() + producto.getCantidad());
            productoRepositorio.save(productoExistente);
        } else {
            producto.setFecha(LocalDate.now());
            // La imagen se genera automáticamente a partir del nombre; ya no se pide al admin.
            producto.setImagenUrl(generarImagenUrl(producto.getNombre()));
            productoRepositorio.save(producto);
        }

        return "redirect:/inicio";
    }

    @PostMapping("/eliminar")
    public String eliminarProducto(@RequestParam("productoId") String id, HttpSession sesion) {
        if (!esAdmin(sesion)) {
            return "redirect:/login";
        }

        productoRepositorio.deleteById(id);
        return "redirect:/inicio";
    }

    /**
     * Genera una URL de imagen "placeholder" a partir del nombre del producto,
     * usando un color distinto según la marca detectada en el nombre.
     * No requiere subir ni guardar ningún archivo.
     */
    private String generarImagenUrl(String nombre) {
        String nombreLower = nombre.toLowerCase();
        String colorFondo;

        if (nombreLower.contains("iphone")) {
            colorFondo = "1d1d1f/ffffff"; // negro Apple
        } else if (nombreLower.contains("samsung")) {
            colorFondo = "1428a0/ffffff"; // azul Samsung
        } else if (nombreLower.contains("xiaomi")) {
            colorFondo = "ff6900/ffffff"; // naranja Xiaomi
        } else if (nombreLower.contains("huawei")) {
            colorFondo = "cf0a2c/ffffff"; // rojo Huawei
        } else {
            colorFondo = "0d6efd/ffffff"; // azul genérico de la tienda
        }

        String textoCodificado = URLEncoder.encode(nombre, StandardCharsets.UTF_8);
        return "https://placehold.co/500x500/" + colorFondo + "?text=" + textoCodificado + "&font=roboto";
    }

    /** true si hay una sesión activa con rol ADMIN */
    private boolean esAdmin(HttpSession sesion) {
        Object rol = sesion.getAttribute("usuarioRol");
        return "ADMIN".equals(rol);
    }

    /** Agrega al modelo los datos de sesión que la vista necesita para mostrar/ocultar secciones */
    private void agregarDatosDeSesion(Model modelo, HttpSession sesion) {
        Object rol = sesion.getAttribute("usuarioRol");
        Object nombre = sesion.getAttribute("usuarioNombre");

        modelo.addAttribute("estaLogueado", rol != null);
        modelo.addAttribute("esAdmin", "ADMIN".equals(rol));
        modelo.addAttribute("nombreUsuario", nombre != null ? nombre : "");
    }
}
