package com.proyecto3.proyecto3.repositorio;

import com.proyecto3.proyecto3.modelo.Venta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaRepositorio extends MongoRepository<Venta, String> {
    List<Venta> findByUsuarioEmailOrderByFechaDesc(String email);
    List<Venta> findAllByOrderByFechaDesc();
}
