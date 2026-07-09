package com.proyecto3.proyecto3.configuracion;

import com.proyecto3.proyecto3.controlador.TiendaControlador;
import com.proyecto3.proyecto3.modelo.Producto;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(20) // después del AdminSeeder
public class MigrationImagenesProductos implements CommandLineRunner {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Override
    public void run(String... args) {
        List<Producto> productos = productoRepositorio.findAll();
        int actualizados = 0;

        System.out.println("DEBUG >> === Migración de imágenes iniciada ===");
        System.out.println("DEBUG >> Total de productos en BD: " + productos.size());

        for (Producto p : productos) {
            String urlActual = p.getImagenUrl();
            String nuevaUrl = TiendaControlador.resolverImagenUrl(p.getNombre());

            System.out.println("DEBUG >> Producto: '" + p.getNombre() + "'");
            System.out.println("DEBUG >>   URL actual: " + urlActual);
            System.out.println("DEBUG >>   URL nueva:   " + nuevaUrl);

            if (debeActualizar(urlActual, nuevaUrl)) {
                p.setImagenUrl(nuevaUrl);
                productoRepositorio.save(p);
                actualizados++;
                System.out.println("DEBUG >>   → ACTUALIZADA");
            } else {
                System.out.println("DEBUG >>   → respetada (no se actualiza)");
            }
        }

        System.out.println("DEBUG >> Migración de imágenes: " + actualizados
                + " producto(s) actualizado(s) de " + productos.size() + " total(es).");
        System.out.println("DEBUG >> === Migración de imágenes finalizada ===");
    }

    /**
     * Decide si una URL de imagen debe ser recalculada.
     */
    private boolean debeActualizar(String urlActual, String nuevaUrl) {
        // 1. URL vacía o null → actualizar
        if (urlActual == null || urlActual.isBlank()) {
            return true;
        }
        // 2. URL contiene "placehold.co" → actualizar
        if (urlActual.contains("placehold.co")) {
            return true;
        }
        // 3. URL externa http(s):// personalizada que no sea placeholder → respetar
        if (urlActual.startsWith("http://") || urlActual.startsWith("https://")) {
            return false;
        }
        // 4. URL local que apunta a /img/productos/<archivo> → respetar
        if (urlActual.startsWith("/img/productos/")) {
            if (nuevaUrl.startsWith("/img/productos/")) {
                return !urlActual.equals(nuevaUrl);
            }
            return false;
        }
        return true;
    }
}
