package com.proyecto3.proyecto3.admin;

import com.proyecto3.proyecto3.modelo.*;
import com.proyecto3.proyecto3.repositorio.ProductoRepositorio;
import com.proyecto3.proyecto3.repositorio.UsuarioRepositorio;
import com.proyecto3.proyecto3.repositorio.VentaRepositorio;
import com.proyecto3.proyecto3.seguridad.UsuarioDetalles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
@RequestMapping("/admin")
public class AdminControlador {

    private final ProductoRepositorio productoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final VentaRepositorio ventaRepositorio;

    @Autowired
    public AdminControlador(ProductoRepositorio productoRepositorio,
                            UsuarioRepositorio usuarioRepositorio,
                            VentaRepositorio ventaRepositorio) {
        this.productoRepositorio = productoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.ventaRepositorio = ventaRepositorio;
    }

    @GetMapping({"", "/"})
    public String panel(@AuthenticationPrincipal UsuarioDetalles usuario, Model modelo) {
        modelo.addAttribute("nombreUsuario", usuario.getNombre());

        /* lambda stream */
        List<Producto> productos = productoRepositorio.findAll();
        modelo.addAttribute("totalProductos", productos.size());
        modelo.addAttribute("totalStock", productos.stream().mapToInt(Producto::getCantidad).sum());

        Map<TipoProducto, Long> conteoPorTipo = productos.stream()
                .collect(Collectors.groupingBy(Producto::getTipo, Collectors.counting()));
        modelo.addAttribute("conteoPorTipo", conteoPorTipo);

        /* lambda stream */
        List<Venta> ventas = ventaRepositorio.findAllByOrderByFechaDesc();
        modelo.addAttribute("totalVentas", ventas.size());
        modelo.addAttribute("ingresosTotales",
                ventas.stream().mapToDouble(Venta::getTotal).sum());

        LocalDateTime hace30dias = LocalDateTime.now().minusDays(30);
        modelo.addAttribute("ventas30dias",
                ventas.stream().filter(v -> v.getFecha() != null && v.getFecha().isAfter(hace30dias)).count());

        modelo.addAttribute("ventasRecientes",
                ventas.stream().limit(10).collect(Collectors.toList()));

        return "admin/panel";
    }

    @GetMapping("/productos")
    public String listarProductos(@AuthenticationPrincipal UsuarioDetalles usuario,
                                  @RequestParam(value = "tipo", required = false) String tipo,
                                  Model modelo) {
        modelo.addAttribute("nombreUsuario", usuario.getNombre());

        /* lambda stream */
        List<Producto> productos = productoRepositorio.findAll();
        if (tipo != null && !tipo.isBlank()) {
            TipoProducto t = TipoProducto.valueOf(tipo.toUpperCase());
            productos = productos.stream().filter(p -> t.equals(p.getTipo())).collect(Collectors.toList());
        }
        productos = productos.stream()
                .sorted(Comparator.comparing(Producto::getMarca, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Producto::getNombre, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        modelo.addAttribute("productos", productos);
        modelo.addAttribute("tipoSeleccionado", tipo);
        return "admin/productos";
    }

    @GetMapping("/productos/nuevo")
    public String nuevoProducto(@AuthenticationPrincipal UsuarioDetalles usuario, Model modelo) {
        modelo.addAttribute("nombreUsuario", usuario.getNombre());
        modelo.addAttribute("producto", new Producto());
        modelo.addAttribute("tipos", TipoProducto.values());
        modelo.addAttribute("modo", "nuevo");
        return "admin/producto-form";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(@ModelAttribute Producto producto,
                                  @RequestParam("tipoStr") String tipoStr,
                                  @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
                                  @RequestParam(value = "modo", required = false) String modo) {
        try {
            producto.setTipo(TipoProducto.valueOf(tipoStr.toUpperCase()));

            if ("nuevo".equals(modo) || producto.getId() == null || producto.getId().isBlank()) {
                producto.setFecha(LocalDate.now());
                if (producto.getImagenUrl() == null || producto.getImagenUrl().isBlank()) {
                    producto.setImagenUrl("/img/productos/placeholder.png");
                }
            } else {
                Producto existente = productoRepositorio.findById(producto.getId()).orElse(null);
                if (existente != null) {
                    producto.setFecha(existente.getFecha());
                    if (producto.getImagenUrl() == null || producto.getImagenUrl().isBlank()) {
                        producto.setImagenUrl(existente.getImagenUrl());
                    }
                }
            }

            if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
                String nombreArchivo = guardarImagen(imagenArchivo);
                if (nombreArchivo != null) {
                    producto.setImagenUrl("/img/uploads/" + nombreArchivo);
                }
            }

            productoRepositorio.save(producto);
        } catch (Exception e) {
            System.err.println("Error al guardar producto: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/admin/productos?tipo=" + tipoStr.toLowerCase();
    }

    @GetMapping("/productos/editar/{id}")
    public String editarProducto(@PathVariable String id,
                                 @AuthenticationPrincipal UsuarioDetalles usuario,
                                 Model modelo) {
        Producto producto = productoRepositorio.findById(id).orElse(null);
        if (producto == null) {
            return "redirect:/admin/productos";
        }
        modelo.addAttribute("nombreUsuario", usuario.getNombre());
        modelo.addAttribute("producto", producto);
        modelo.addAttribute("tipos", TipoProducto.values());
        modelo.addAttribute("modo", "editar");
        return "admin/producto-form";
    }

    @PostMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable String id) {
        productoRepositorio.deleteById(id);
        return "redirect:/admin/productos";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(@AuthenticationPrincipal UsuarioDetalles usuario, Model modelo) {
        modelo.addAttribute("nombreUsuario", usuario.getNombre());

        /* lambda stream */
        List<Usuario> usuarios = usuarioRepositorio.findAll();
        Map<String, List<Venta>> ventasPorEmail = ventaRepositorio.findAllByOrderByFechaDesc().stream()
                .filter(v -> v.getUsuarioEmail() != null)
                .collect(Collectors.groupingBy(Venta::getUsuarioEmail));

        /* lambda stream */
        List<Map<String, Object>> usuariosConVentas = usuarios.stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nombre", u.getNombre());
                    m.put("email", u.getEmail());
                    m.put("rol", u.getRol());
                    List<Venta> ventas = ventasPorEmail.getOrDefault(u.getEmail(), List.of());
                    m.put("cantidadCompras", ventas.size());
                    m.put("totalGastado", ventas.stream().mapToDouble(Venta::getTotal).sum());
                    m.put("ultimaCompra", ventas.isEmpty() ? null : ventas.get(0).getFecha());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("cantidadCompras"), (Integer) a.get("cantidadCompras")))
                .collect(Collectors.toList());

        modelo.addAttribute("usuarios", usuariosConVentas);
        return "admin/usuarios";
    }

    @GetMapping("/ventas")
    public String listarVentas(@AuthenticationPrincipal UsuarioDetalles usuario, Model modelo) {
        modelo.addAttribute("nombreUsuario", usuario.getNombre());

        /* lambda stream */
        List<Venta> ventas = ventaRepositorio.findAllByOrderByFechaDesc();
        modelo.addAttribute("ventas", ventas);
        modelo.addAttribute("totalVentas", ventas.size());
        modelo.addAttribute("ingresosTotales",
                ventas.stream().mapToDouble(Venta::getTotal).sum());

        Map<String, Long> ventasPorMarca = ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(i -> i.getMarca() != null)
                .collect(Collectors.groupingBy(ItemVenta::getMarca,
                        Collectors.summingLong(ItemVenta::getCantidad)));
        modelo.addAttribute("ventasPorMarca", ventasPorMarca);

        Map<TipoProducto, Long> ventasPorTipo = ventas.stream()
                .flatMap(v -> v.getItems().stream())
                .filter(i -> i.getTipo() != null)
                .collect(Collectors.groupingBy(ItemVenta::getTipo,
                        Collectors.summingLong(ItemVenta::getCantidad)));
        modelo.addAttribute("ventasPorTipo", ventasPorTipo);

        return "admin/ventas";
    }

    @GetMapping("/ventas/excel")
    public ResponseEntity<ByteArrayResource> exportarExcel() throws IOException {
        List<Venta> ventas = ventaRepositorio.findAllByOrderByFechaDesc();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet hojaVentas = workbook.createSheet("Ventas");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"N°", "Fecha", "Cliente", "Email", "Producto", "Marca", "Tipo", "Precio Unit.", "Cantidad", "Subtotal"};
            Row headerRow = hojaVentas.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int nVenta = 1;
            for (Venta venta : ventas) {
                if (venta.getItems() == null || venta.getItems().isEmpty()) continue;
                for (ItemVenta item : venta.getItems()) {
                    Row row = hojaVentas.createRow(rowNum++);
                    row.createCell(0).setCellValue(nVenta);
                    Cell fechaCell = row.createCell(1);
                    if (venta.getFecha() != null) {
                        fechaCell.setCellValue(venta.getFecha().toString());
                    }
                    row.createCell(2).setCellValue(venta.getUsuarioNombre() != null ? venta.getUsuarioNombre() : "—");
                    row.createCell(3).setCellValue(venta.getUsuarioEmail() != null ? venta.getUsuarioEmail() : "—");
                    row.createCell(4).setCellValue(item.getProductoNombre() != null ? item.getProductoNombre() : "—");
                    row.createCell(5).setCellValue(item.getMarca() != null ? item.getMarca() : "—");
                    row.createCell(6).setCellValue(item.getTipo() != null ? item.getTipo().toString() : "—");
                    row.createCell(7).setCellValue(item.getPrecioUnitario());
                    row.createCell(8).setCellValue(item.getCantidad());
                    row.createCell(9).setCellValue(item.getSubtotal());
                }
                nVenta++;
            }

            for (int i = 0; i < headers.length; i++) {
                hojaVentas.autoSizeColumn(i);
            }

            Sheet hojaStats = workbook.createSheet("Estadísticas");
            Row sH1 = hojaStats.createRow(0);
            Cell sHCell1 = sH1.createCell(0);
            sHCell1.setCellValue("Métrica");
            sHCell1.setCellStyle(headerStyle);
            Cell sHCell2 = sH1.createCell(1);
            sHCell2.setCellValue("Valor");
            sHCell2.setCellStyle(headerStyle);

            double totalGanancias = ventas.stream().flatMap(v -> v.getItems().stream())
                    .mapToDouble(ItemVenta::getSubtotal).sum();
            int totalProductosVendidos = ventas.stream().flatMap(v -> v.getItems().stream())
                    .mapToInt(ItemVenta::getCantidad).sum();

            int r = 1;
            r = addStatRow(hojaStats, r, "Total de ventas", String.valueOf(ventas.size()));
            r = addStatRow(hojaStats, r, "Productos vendidos", String.valueOf(totalProductosVendidos));
            r = addStatRow(hojaStats, r, "Ingresos totales (S/)", String.format("%.2f", totalGanancias));
            r = addStatRow(hojaStats, r, "Ticket promedio (S/)",
                    ventas.isEmpty() ? "0.00" : String.format("%.2f", totalGanancias / ventas.size()));
            r++;

            Cell marcaTitle = hojaStats.createRow(r++).createCell(0);
            marcaTitle.setCellValue("Ventas por marca");
            marcaTitle.setCellStyle(headerStyle);

            Map<String, Long> ventasMarca = ventas.stream()
                    .flatMap(v -> v.getItems().stream())
                    .filter(i -> i.getMarca() != null)
                    .collect(Collectors.groupingBy(ItemVenta::getMarca,
                            Collectors.summingLong(ItemVenta::getCantidad)));

            for (Map.Entry<String, Long> e : ventasMarca.entrySet()) {
                Row row = hojaStats.createRow(r++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue());
            }
            r++;

            Cell tipoTitle = hojaStats.createRow(r++).createCell(0);
            tipoTitle.setCellValue("Ventas por tipo");
            tipoTitle.setCellStyle(headerStyle);

            Map<TipoProducto, Long> ventasTipo = ventas.stream()
                    .flatMap(v -> v.getItems().stream())
                    .filter(i -> i.getTipo() != null)
                    .collect(Collectors.groupingBy(ItemVenta::getTipo,
                            Collectors.summingLong(ItemVenta::getCantidad)));

            for (Map.Entry<TipoProducto, Long> e : ventasTipo.entrySet()) {
                Row row = hojaStats.createRow(r++);
                row.createCell(0).setCellValue(e.getKey().toString());
                row.createCell(1).setCellValue(e.getValue());
            }

            hojaStats.autoSizeColumn(0);
            hojaStats.autoSizeColumn(1);

            workbook.write(out);

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=estadisticas_ventas.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(out.size())
                    .body(resource);
        }
    }

    private int addStatRow(Sheet hoja, int row, String metrica, String valor) {
        Row r = hoja.createRow(row);
        r.createCell(0).setCellValue(metrica);
        r.createCell(1).setCellValue(valor);
        return row + 1;
    }

    private String guardarImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) return null;
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null) return null;
        String extension = "";
        int dotIdx = nombreOriginal.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = nombreOriginal.substring(dotIdx).toLowerCase();
        }
        String nombreSeguro = "prod_" + System.currentTimeMillis() + extension;
        java.nio.file.Path destino = java.nio.file.Paths.get(
                "src/main/resources/static/img/uploads", nombreSeguro);
        if (!destino.getParent().toFile().exists()) {
            destino.getParent().toFile().mkdirs();
        }
        archivo.transferTo(destino.toFile());
        return nombreSeguro;
    }
}
