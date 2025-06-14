package com.Recursos;

import com.DTO.ProductoDTO;
import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.Otros.ProductEvent;
import com.Otros.ResponseMessage;
import com.Otros.PaginacionResponse;
import com.Servicios.CatalogoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @Inject
    public CatalogoService catalogoService;

    private static final Logger LOG = Logger.getLogger(CatalogoResource.class);

    @GET
    public Response getProducts(@QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("nombre") String nombre,
                                @QueryParam("categoria") String categoria,
                                @QueryParam("precioMin") Double precioMin,
                                @QueryParam("precioMax") Double precioMax) {
        try {
            List<Producto> productos = catalogoService.obtenerProductos(page, size, nombre, categoria, precioMin, precioMax);
            return Response.ok(productos).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @RolesAllowed("admin")
    public Response addProduct(@Valid ProductoDTO producto) {
        try {
            Producto nuevoProducto = catalogoService.agregarProducto(producto);
            ResponseMessage responseMessage = new ResponseMessage("Producto añadido con éxito.", nuevoProducto);
            return Response.status(Response.Status.CREATED).entity(responseMessage).build();
        } catch (Exception e) {
            ResponseMessage responseMessage = new ResponseMessage("Error al añadir el producto: " + e.getMessage(), null);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseMessage).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin"})
    public Response updateProduct(@PathParam("id") Long id, @Valid ProductoDTO producto) {
        try {
            if (catalogoService.actualizarProducto(id, producto)) {
                ProductEvent event = new ProductEvent(id, "UPDATED", null);
                catalogoService.emitirEventoProducto(event);
                return Response.ok("Producto actualizado con éxito.").build();
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        } catch (Exception e) {
            return Response.serverError().entity("Error actualizando el producto: "+e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deleteProduct(@PathParam("id") Long id) {
        try {
            if (catalogoService.eliminarProducto(id)) {
                ProductEvent event = new ProductEvent(id, "DELETED", null);
                catalogoService.emitirEventoProducto(event);
                return Response.ok("Producto eliminado con éxito.").build();
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar el producto: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") Long id) {
        Producto producto = catalogoService.obtenerProductoPorId(id);
        if (producto == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        }
        return Response.ok(producto).build();
    }

    @POST
    @RolesAllowed("admin")
    @Path("/{id}/reserva")
    public Response reservarStock(@PathParam("id") Long productoId,
                                  @QueryParam("cantidad") int cantidad) {
        try {
            boolean reservado = catalogoService.reservarStock(productoId, cantidad);
            if (reservado) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity("Stock insuficiente").build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{id}/valoraciones")
    public Response obtenerValoracionesPorProducto(@PathParam("id") Long idProducto,
                                                   @QueryParam("pagina") @DefaultValue("1") int pagina,
                                                   @QueryParam("tamanio") @DefaultValue("10") int tamanio) {
        try {
            List<Valoracion> valoraciones = catalogoService.obtenerValoracionesPorProducto(idProducto, pagina, tamanio);
            long total = catalogoService.contarValoracionesPorProducto(idProducto);

            return Response.ok()
                    .entity(new PaginacionResponse<>(valoraciones, pagina, tamanio, total))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las valoraciones: " + e.getMessage())
                    .build();
        }
    }
}