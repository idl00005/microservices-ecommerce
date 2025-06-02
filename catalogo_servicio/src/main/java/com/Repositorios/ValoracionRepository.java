package com.Repositorios;

import com.Entidades.Valoracion;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ValoracionRepository implements PanacheRepository<Valoracion> {
}