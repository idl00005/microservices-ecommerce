package com.catalogo;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject; // Para inyectar el repositorio
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @Inject
    RepositorioProducto productoRepository; // Inyección del repositorio de productos

    @GET
    @RolesAllowed({"admin","user"}) // Solo accesible con un token válido
    public List<Producto> getProducts() {
        // Retorna todos los productos desde la base de datos
        return productoRepository.listAll();
    }
}
