package com.proyecto3.proyecto3.controlador;

import com.proyecto3.proyecto3.modelo.*;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import com.proyecto3.proyecto3.repositorio.VentaRepositorio;
import com.proyecto3.proyecto3.seguridad.UsuarioDetalles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class TiendaControlador {

    private final ProductoRepositorio productoRepositorio;
    private final VentaRepositorio ventaRepositorio;

    @Autowired
    public TiendaControlador(ProductoRepositorio productoRepositorio,
                             VentaRepositorio ventaRepositorio) {
        this.productoRepositorio = productoRepositorio;
        this.ventaRepositorio = ventaRepositorio;
    }

    @GetMapping({"", "inicio"})
    public String inicio(Model modelo, @AuthenticationPrincipal UsuarioDetalles usuario) {
        agregarDatosUsuario(modelo, usuario);

        List<Producto> todos = productoRepositorio.findAll();
        modelo.addAttribute("totalProductos", todos.size());

        /* lambda stream */
        Map<TipoProducto, List<Producto>> porTipo = todos.stream()
                .filter(p -> p.getTipo() != null)
                .sorted(Comparator.comparing(Producto::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.groupingBy(Producto::getTipo,
                        LinkedHashMap::new, Collectors.toList()));
        modelo.addAttribute("productosPorTipo", porTipo);

        /* lambda stream */
        List<Producto> loMasNuevo = todos.stream()
                .filter(p -> p.getMarca() != null && !p.getMarca().isBlank())
                .collect(Collectors.groupingBy(Producto::getMarca,
                        LinkedHashMap::new,
                        Collectors.maxBy(Comparator.comparing(
                                Producto::getFecha,
                                Comparator.nullsFirst(Comparator.naturalOrder())))))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(Producto::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .collect(Collectors.toList());
        modelo.addAttribute("loMasNuevo", loMasNuevo);

        return "index";
    }

    @PostMapping("/comprar")
    public String comprarProducto(@RequestParam("productoId") String id,
                                  @AuthenticationPrincipal UsuarioDetalles usuario) {
        if (usuario == null) {
            return "redirect:/login?requiereLogin";
        }

        Optional<Producto> opcional = productoRepositorio.findById(id);
        if (opcional.isPresent()) {
            Producto producto = opcional.get();
            if (producto.getCantidad() > 0) {
                producto.setCantidad(producto.getCantidad() - 1);
                productoRepositorio.save(producto);

                ItemVenta item = new ItemVenta();
                item.setProductoId(producto.getId());
                item.setProductoNombre(producto.getNombre());
                item.setMarca(producto.getMarca());
                item.setTipo(producto.getTipo());
                item.setPrecioUnitario(producto.getPrecio());
                item.setCantidad(1);
                item.setSubtotal(producto.getPrecio());

                Venta venta = new Venta();
                venta.setUsuarioEmail(usuario.getUsername());
                venta.setUsuarioNombre(usuario.getNombre());
                venta.setFecha(LocalDateTime.now());
                venta.setItems(List.of(item));
                venta.setTotal(producto.getPrecio());
                ventaRepositorio.save(venta);
            }
        }
        return "redirect:/inicio?compra=confirmada";
    }

    @GetMapping("/celulares")
    public String celulares(Model modelo, @AuthenticationPrincipal UsuarioDetalles usuario) {
        cargarCategoria(modelo, TipoProducto.CELULAR, usuario);
        modelo.addAttribute("tipoSlug", "celulares");
        modelo.addAttribute("tipoNombre", "Celulares");
        return "categoria";
    }

    @GetMapping("/tablets")
    public String tablets(Model modelo, @AuthenticationPrincipal UsuarioDetalles usuario) {
        cargarCategoria(modelo, TipoProducto.TABLET, usuario);
        modelo.addAttribute("tipoSlug", "tablets");
        modelo.addAttribute("tipoNombre", "Tablets");
        return "categoria";
    }

    @GetMapping("/wearables")
    public String wearables(Model modelo, @AuthenticationPrincipal UsuarioDetalles usuario) {
        cargarCategoria(modelo, TipoProducto.WEARABLE, usuario);
        modelo.addAttribute("tipoSlug", "wearables");
        modelo.addAttribute("tipoNombre", "Wearables");
        return "categoria";
    }

    /* lambda stream */
    private void cargarCategoria(Model modelo, TipoProducto tipo, UsuarioDetalles usuario) {
        agregarDatosUsuario(modelo, usuario);

        List<Producto> productos = productoRepositorio.findByTipoOrderByFechaDesc(tipo);
        modelo.addAttribute("productos", productos);
        modelo.addAttribute("tipo", tipo);

        /* lambda stream */
        Map<String, List<Producto>> porMarca = productos.stream()
                .filter(p -> p.getMarca() != null && !p.getMarca().isBlank())
                .collect(Collectors.groupingBy(
                        p -> p.getMarca().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        modelo.addAttribute("productosPorMarca", porMarca);

        /* lambda stream */
        List<String> marcas = porMarca.keySet().stream().sorted().collect(Collectors.toList());
        modelo.addAttribute("marcas", marcas);

        /* lambda stream */
        DoubleSummaryStatistics stats = productos.stream()
                .mapToDouble(Producto::getPrecio)
                .summaryStatistics();
        modelo.addAttribute("precioMin", stats.getMin());
        modelo.addAttribute("precioMax", stats.getMax());
    }

    private void agregarDatosUsuario(Model modelo, UsuarioDetalles usuario) {
        if (usuario != null) {
            modelo.addAttribute("estaLogueado", true);
            modelo.addAttribute("nombreUsuario", usuario.getNombre());
            modelo.addAttribute("esAdmin", "ADMIN".equalsIgnoreCase(usuario.getRol()));
        } else {
            modelo.addAttribute("estaLogueado", false);
            modelo.addAttribute("nombreUsuario", "");
            modelo.addAttribute("esAdmin", false);
        }
    }
}
