package com.proyecto3.proyecto3.repositorio;

import com.proyecto3.proyecto3.modelo.Producto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepositorio extends MongoRepository<Producto, String> {
    Optional<Producto> findByNombre(String nombre);
}
