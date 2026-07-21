package com.proyecto3.proyecto3.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "venta")
public class Venta {

    @Id
    private String id;

    private String usuarioEmail;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private List<ItemVenta> items;
    private double total;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public List<ItemVenta> getItems() { return items; }
    public void setItems(List<ItemVenta> items) { this.items = items; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
