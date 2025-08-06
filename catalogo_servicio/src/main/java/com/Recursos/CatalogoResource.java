package com.Recursos;

import com.DTO.ProductoDTO;
import com.DTO.ValoracionDTO;
import com.Otros.PaginacionResponse;
import com.Servicios.CatalogoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @Inject
    public CatalogoService catalogoService;

    @GET
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response getProducts(@QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("nombre") String nombre,
                                @QueryParam("categoria") String categoria,
                                @QueryParam("precioMin") Double precioMin,
                                @QueryParam("precioMax") Double precioMax) {
        if (size > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El tamaño máximo permitido por página es 100").build();
        }
        try {
            List<ProductoDTO> productos = catalogoService.obtenerProductos(page, size, nombre, categoria, precioMin, precioMax);
            return Response.ok(productos).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @RolesAllowed("admin")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response addProduct(@Valid ProductoDTO producto) {
        try {
            ProductoDTO nuevoProducto = catalogoService.agregarProducto(producto);
            return Response.status(Response.Status.CREATED)
                    .entity(nuevoProducto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al agregar el producto: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response updateProduct(@PathParam("id") Long id, @Valid ProductoDTO producto) {
        try {
            if (catalogoService.actualizarProducto(id, producto)) {
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
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response deleteProduct(@PathParam("id") Long id) {
        try {
            if (catalogoService.eliminarProducto(id)) {
                return Response.ok("Producto eliminado con éxito.").build();
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar el producto: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response getProductById(@PathParam("id") Long id) {
        if (id == null || id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El ID del producto debe ser un número positivo.").build();
        }
        try {
            ProductoDTO producto = catalogoService.obtenerProductoPorId(id);
            return Response.ok(producto).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error obteniendo el producto con id "+id+": "+e.getMessage()).build();
        }
    }

    @POST
    @RolesAllowed("admin")
    @Path("/{id}/reserva")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
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
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response obtenerValoracionesPorProducto(@PathParam("id") Long idProducto,
                                                   @QueryParam("pagina") @DefaultValue("1") int pagina,
                                                   @QueryParam("tamanio") @DefaultValue("10") int tamanio) {
        try {
            List<ValoracionDTO> valoraciones = catalogoService.obtenerValoracionesPorProducto(idProducto, pagina, tamanio);
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

    @GET
    @Path("/{productoId}/valoracion")
    @RolesAllowed("user")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response existeValoracion(
            @PathParam("productoId") Long productoId, @Context SecurityContext sctx) {
        try{
            String userId = sctx.getUserPrincipal().getName();
            boolean existe = catalogoService.existeValoracionPorPedido(productoId, userId);
            return Response.ok(existe).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al comprobar si existe la valoracion: " + e.getMessage())
                    .build();
        }
    }
}