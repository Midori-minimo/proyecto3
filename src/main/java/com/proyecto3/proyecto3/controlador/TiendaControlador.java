package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.Producto;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String listarProductos(Model modelo) {
        System.out.println("DEBUG >> Base de datos conectada: " + mongoTemplate.getDb().getName());
        System.out.println("DEBUG >> Colecciones disponibles: " + mongoTemplate.getCollectionNames());
        System.out.println("DEBUG >> Documentos en 'producto' (via driver crudo): " + mongoTemplate.getCollection("producto").countDocuments());

        List<Producto> producto = productoRepositorio.findAll();
        System.out.println("DEBUG >> Productos encontrados (via repositorio): " + producto.size());
        modelo.addAttribute("productos", producto);
        modelo.addAttribute("productoNuevo", new Producto());
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
    public String agregarProducto(@ModelAttribute Producto producto) {
        Optional<Producto> existente = productoRepositorio.findByNombre(producto.getNombre().trim());

        if (existente.isPresent()) {
            Producto productoExistente = existente.get();
            productoExistente.setCantidad(productoExistente.getCantidad() + producto.getCantidad());
            productoRepositorio.save(productoExistente);
        } else {
            producto.setFecha(LocalDate.now());
            if (producto.getImagenUrl() == null || producto.getImagenUrl().isBlank()) {
                producto.setImagenUrl("phone1.jpg");
            }
            productoRepositorio.save(producto);
        }

        return "redirect:/inicio";
    }

    @PostMapping("/eliminar")
    public String eliminarProducto(@RequestParam("productoId") String id) {
        productoRepositorio.deleteById(id);
        return "redirect:/inicio";
    }
}
