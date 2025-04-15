package com.Recursos;

import com.Entidades.Producto;
import com.Otros.ProductEvent;
import com.Otros.ResponseMessage;
import com.Repositorios.RepositorioProducto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.util.List;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @Inject
    RepositorioProducto productoRepository; // Inyección del repositorio de productos

    @Inject
    @Channel("product-events")
    Emitter<ProductEvent> productEventEmitter;

    private static final Logger LOG = Logger.getLogger(CatalogoResource.class);

    @GET
    public Response getProducts(@QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("nombre") String nombre,
                                @QueryParam("categoria") String categoria,
                                @QueryParam("precioMin") Double precioMin,
                                @QueryParam("precioMax") Double precioMax) {

        // Validate page and size parameters
        if (page < 1 || size < 1) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Los parámetros 'page' y 'size' deben ser mayores o iguales a 1.")
                    .build();
        }

        // Filter products
        List<Producto> productos = productoRepository.listAll().stream()
                .filter(p -> nombre == null || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(p -> categoria == null || (p.getCategoria() != null && p.getCategoria().equalsIgnoreCase(categoria)))
                .filter(p -> precioMin == null || p.getPrecio().compareTo(BigDecimal.valueOf(precioMin)) >= 0)
                .filter(p -> precioMax == null || p.getPrecio().compareTo(BigDecimal.valueOf(precioMax)) <= 0)
                .toList();

        if (productos.isEmpty()) {
            return Response.noContent().build();
        }

        // Pagination
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, productos.size());

        if (startIndex >= productos.size()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Página fuera de rango.")
                    .build();
        }

        List<Producto> paginated = productos.subList(startIndex, endIndex);
        return Response.ok(paginated).build();
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
    @RolesAllowed("admin")
    public Response updateProduct(@PathParam("id") Long id, @Valid Producto producto) {
        boolean updated;

        // Ejecutar la transacción en un método separado
        try {
            updated = updateProductInDB(id, producto);
        } catch (Exception e) {
            LOG.error("Error actualizando producto", e);
            return Response.serverError().entity("Error actualizando el producto.").build();
        }

        if (updated) {
            ProductEvent event = new ProductEvent(id, "UPDATED", producto);
            LOG.info("Publicando evento de actualización para producto " + id);
            productEventEmitter.send(event); // Fuera del contexto transaccional
            return Response.ok("Producto actualizado con éxito.").build();
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity("Producto con ID " + id + " no encontrado.")
                .build();
    }

    @Transactional
    public boolean updateProductInDB(Long id, Producto producto) {
        return productoRepository.updateProduct(
                id, producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.getStock(), producto.getDetalles()
        );
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deleteProduct(@PathParam("id") Long id) {
        if (deleteProductInDB(id)) {
            ProductEvent event = new ProductEvent(id, "DELETED", null);
            productEventEmitter.send(event);

            return Response.ok("Producto eliminado con éxito.").build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Producto con ID " + id + " no encontrado.")
                .build();
    }

    @Transactional
    public boolean deleteProductInDB(Long id) {
        Producto producto = productoRepository.findById(id);
        if (producto != null) {
            productoRepository.delete(producto);
            return true;
        }
        return false;
    }
    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") Long id) {
        Producto producto = productoRepository.findById(id);
        if (producto == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Producto con ID " + id + " no encontrado.")
                    .build();
        }
        return Response.ok(producto).build();
    }

}
