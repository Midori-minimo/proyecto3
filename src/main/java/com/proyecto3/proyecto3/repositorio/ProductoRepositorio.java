package com.proyecto3.proyecto3.repositorio;

import com.proyecto3.proyecto3.modelo.Producto;
import com.proyecto3.proyecto3.modelo.TipoProducto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepositorio extends MongoRepository<Producto, String> {
    Optional<Producto> findByNombre(String nombre);
    List<Producto> findByTipo(TipoProducto tipo);
    List<Producto> findByTipoOrderByFechaDesc(TipoProducto tipo);
    List<Producto> findByTipoAndMarca(TipoProducto tipo, String marca);
}
