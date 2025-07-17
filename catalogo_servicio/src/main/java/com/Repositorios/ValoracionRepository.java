package com.Repositorios;

import com.Entidades.Valoracion;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ValoracionRepository implements PanacheRepository<Valoracion> {
    public boolean existsByProductoIdAndUsuarioId(Long productoId, String usuarioId) {
        return count("producto.id = ?1 and idUsuario = ?2", productoId, usuarioId) > 0;
    }
}