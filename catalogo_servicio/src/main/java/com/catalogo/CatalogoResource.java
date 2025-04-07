package com.catalogo;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject; // Para inyectar el repositorio
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @POST
    @RolesAllowed("admin")
    public Response addProduct(@Valid Producto producto) {
        try {
            productoRepository.add(producto);

            return Response.status(Response.Status.CREATED)
                    .entity("Producto añadido con éxito.")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al añadir el producto: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin") // Solo accesible por administradores
    public Response updateProduct(@PathParam("id") Long id, @Valid Producto producto) {
        try {
            // Llamar al método del repositorio para actualizar el producto con los datos proporcionados
            boolean updated = productoRepository.updateProduct(
                    id,
                    producto.getNombre(),
                    producto.getDescripcion(),
                    producto.getPrecio(),
                    producto.getStock(),
                    producto.getDetalles()
            );

            if (!updated) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto con ID " + id + " no encontrado.")
                        .build();
            }

            return Response.ok("Producto actualizado con éxito.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar el producto: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    public Response deleteProduct(@PathParam("id") Long id) {
        try {
            Producto producto = productoRepository.findById(id);
            if (producto == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto con ID " + id + " no encontrado.")
                        .build();
            }

            // Eliminar el producto si existe
            productoRepository.delete(producto);

            return Response.ok("Producto eliminado con éxito.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar el producto: " + e.getMessage())
                    .build();
        }
    }

}
