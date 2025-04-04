package com.Repositorios;

import com.Entidades.Producto;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RepositorioProducto implements PanacheRepository<Producto> {
    // Métodos específicos del repositorio
}
