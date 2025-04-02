package com.catalogo;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @GET
    @RolesAllowed("admin") // Solo accesible con un token válido
    public List<String> getProducts() {
        return List.of("Producto 1", "Producto 2", "Producto 3");
    }
}
