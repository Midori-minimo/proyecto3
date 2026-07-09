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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class TiendaControlador {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Map<String, String> IMAGENES_POR_MODELO = new LinkedHashMap<>();
    static {
        IMAGENES_POR_MODELO.put("huawei nova 12s",      "huawei_nova_12s.png");
        IMAGENES_POR_MODELO.put("huawei nova 12",       "huawei_nova_12s.png"); // variante
        IMAGENES_POR_MODELO.put("huawei nova 11",       "huawei_nova_11.jpg");
        IMAGENES_POR_MODELO.put("huawei nova",          "huawei_nova_11.jpg"); // variante genérica
        IMAGENES_POR_MODELO.put("xiaomi redmi note 13", "xiaomi_redmi_note_13_pro.jpg");
        IMAGENES_POR_MODELO.put("xiaomi redmi note 13 pro", "xiaomi_redmi_note_13_pro.jpg");
        IMAGENES_POR_MODELO.put("xiaomi 13t pro",       "xiaomi_13t_pro.jpg");
        IMAGENES_POR_MODELO.put("xiaomi 13t",           "xiaomi_13t_pro.jpg"); // variante
        IMAGENES_POR_MODELO.put("samsung galaxy s25",   "samsung_galaxy_s25.jpg");
        IMAGENES_POR_MODELO.put("samsung galaxy a55",   "samsung_galaxy_a55.jpeg");
        IMAGENES_POR_MODELO.put("iphone 15 pro",        "iphone_15_pro.png");
        IMAGENES_POR_MODELO.put("iphone 15",            "iphone_15_pro.png"); // variante
        IMAGENES_POR_MODELO.put("iphone 14",            "iphone_14.png");
    }

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
    public String comprarProducto(@RequestParam("productoId") String id, HttpSession sesion) {
        if (!esLogueado(sesion)) {
            return "redirect:/login?requiereLogin";
        }

        Optional<Producto> opcional = productoRepositorio.findById(id);
        boolean comprado = false;
        if (opcional.isPresent()) {
            Producto producto = opcional.get();
            if (producto.getCantidad() > 0) {
                producto.setCantidad(producto.getCantidad() - 1);
                productoRepositorio.save(producto);
                comprado = true;
            }
        }
        if (comprado) {
            return "redirect:/inicio?compra=confirmada";
        }
        return "redirect:/inicio";
    }
    @PostMapping("/agregar")
    public String agregarProducto(@ModelAttribute Producto producto,
                                  @RequestParam(value = "imagenUrl", required = false) String imagenUrl,
                                  HttpSession sesion) {
        if (!esAdmin(sesion)) {
            return "redirect:/login";
        }

        Optional<Producto> existente = productoRepositorio.findByNombre(producto.getNombre().trim());

        if (existente.isPresent()) {
            // El producto ya existía: solo sumamos stock, NO tocamos la imagen.
            Producto productoExistente = existente.get();
            productoExistente.setCantidad(productoExistente.getCantidad() + producto.getCantidad());
            productoRepositorio.save(productoExistente);
        } else {
            producto.setFecha(LocalDate.now());
            producto.setImagenUrl(resolverImagenUrl(producto.getNombre(), imagenUrl));
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

    public static String resolverImagenUrl(String nombre, String urlPersonalizada) {
        // 1) URL personalizada del admin
        if (urlPersonalizada != null) {
            String url = urlPersonalizada.trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }
        }

        // 2) Catálogo de imágenes reales
        String nombreNorm = normalizar(nombre);
        for (Map.Entry<String, String> entrada : IMAGENES_POR_MODELO.entrySet()) {
            if (nombreNorm.contains(entrada.getKey())) {
                return "/img/productos/" + entrada.getValue();
            }
        }

        // 3) Placeholder con color de la marca
        return generarPlaceholderUrl(nombre);
    }

    /** Variante sin URL personalizada — usada por la migración al arrancar. */
    public static String resolverImagenUrl(String nombre) {
        return resolverImagenUrl(nombre, null);
    }

    private static String generarPlaceholderUrl(String nombre) {
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

    /** Normaliza un nombre para comparar: minúsculas, sin tildes, sin espacios extra. */
    private static String normalizar(String texto) {
        if (texto == null) return "";
        String t = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** true si hay una sesión activa con rol ADMIN */
    private boolean esAdmin(HttpSession sesion) {
        Object rol = sesion.getAttribute("usuarioRol");
        return "ADMIN".equals(rol);
    }

    /** true si hay una sesión iniciada (cualquier rol: CLIENTE o ADMIN) */
    private boolean esLogueado(HttpSession sesion) {
        Object rol = sesion.getAttribute("usuarioRol");
        return rol != null;
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