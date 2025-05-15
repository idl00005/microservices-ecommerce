package com.Repositorios;

import com.Entidades.Valoracion;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ValoracionRepository implements PanacheRepository<Valoracion> {

    public List<Valoracion> obtenerValoracionesPorProducto(Long idProducto, int offset, int limit) {
        return find("idProducto", idProducto)
                .page(offset / limit, limit)
                .list();
    }

    public long contarValoracionesPorProducto(Long idProducto) {
        return count("idProducto", idProducto);
    }
}