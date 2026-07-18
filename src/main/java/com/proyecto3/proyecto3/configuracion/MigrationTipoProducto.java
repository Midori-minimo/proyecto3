package com.proyecto3.proyecto3.configuracion;

import com.proyecto3.proyecto3.modelo.Producto;
import com.proyecto3.proyecto3.modelo.TipoProducto;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(15)
public class MigrationTipoProducto implements CommandLineRunner {

    private final ProductoRepositorio productoRepositorio;

    public MigrationTipoProducto(ProductoRepositorio productoRepositorio) {
        this.productoRepositorio = productoRepositorio;
    }

    @Override
    public void run(String... args) {
        List<Producto> productos = productoRepositorio.findAll();
        int actualizados = 0;

        /* lambda stream */
        long sinTipo = productos.stream().filter(p -> p.getTipo() == null).count();
        if (sinTipo == 0) {
            System.out.println("DEBUG >> Migración tipo: todos los productos ya tienen tipo, omitiendo.");
            return;
        }

        /* lambda stream */
        for (Producto p : productos) {
            boolean cambio = false;
            if (p.getTipo() == null) {
                p.setTipo(inferirTipo(p.getNombre()));
                cambio = true;
            }
            if (p.getMarca() == null || p.getMarca().isBlank()) {
                p.setMarca(inferirMarca(p.getNombre()));
                cambio = true;
            }
            if (cambio) {
                productoRepositorio.save(p);
                actualizados++;
                System.out.println("DEBUG >> Migración tipo: '" + p.getNombre() + "' -> tipo=" + p.getTipo() + " marca=" + p.getMarca());
            }
        }
        System.out.println("DEBUG >> Migración tipo: " + actualizados + " producto(s) actualizado(s).");
    }

    private TipoProducto inferirTipo(String nombre) {
        if (nombre == null) return TipoProducto.CELULAR;
        String n = nombre.toLowerCase();
        if (n.contains("watch") || n.contains("band") || n.contains("smartwatch") || n.contains("wearable")) {
            return TipoProducto.WEARABLE;
        }
        if (n.contains("tablet") || n.contains("tab ") || n.contains("ipad")) {
            return TipoProducto.TABLET;
        }
        return TipoProducto.CELULAR;
    }

    private String inferirMarca(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Otro";
        String primera = nombre.trim().split("\\s+")[0].toLowerCase();
        return switch (primera) {
            case "iphone" -> "Apple";
            case "samsung" -> "Samsung";
            case "xiaomi" -> "Xiaomi";
            case "huawei" -> "Huawei";
            case "motorola", "moto" -> "Motorola";
            case "realme" -> "Realme";
            case "oppo" -> "Oppo";
            case "oneplus" -> "OnePlus";
            case "google", "pixel" -> "Google";
            default -> primera.substring(0, 1).toUpperCase() + primera.substring(1);
        };
    }
}
