package com.catalogo;

import com.Entidades.Producto;
import com.Otros.ResponseMessage;
import com.Repositorios.RepositorioProducto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
    public Response getProducts() {
        List<Producto> productos = productoRepository.listAll();

        if (productos.isEmpty()) {
            // Si no hay productos, devolver código 204 (sin contenido)
            return Response.noContent().build();
        }

        // Devolver la lista de productos con código 200 OK
        return Response.ok(productos).build();

    }

    @POST
    @RolesAllowed("admin")
    public Response addProduct(@Valid Producto producto) {
        try {
            productoRepository.add(producto);

            // Crear una respuesta estructurada como un objeto JSON
            ResponseMessage responseMessage = new ResponseMessage("Producto añadido con éxito.", producto);

            return Response.status(Response.Status.CREATED)
                    .entity(responseMessage)  // Devolvemos el objeto que será convertido a JSON
                    .build();
        } catch (Exception e) {
            // Si hay un error, devolvemos una respuesta con mensaje de error
            ResponseMessage responseMessage = new ResponseMessage("Error al añadir el producto: " + e.getMessage(), null);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseMessage)  // Devolvemos el objeto con el mensaje de error
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
