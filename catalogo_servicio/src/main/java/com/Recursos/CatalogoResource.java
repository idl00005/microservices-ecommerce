package com.Recursos;

import com.DTO.ProductoDTO;
import com.DTO.ValoracionDTO;
import com.Otros.PaginacionResponse;
import com.Servicios.CatalogoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @Inject
    public CatalogoService catalogoService;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry registry;

    private Counter errorCounter;

    @PostConstruct
    public void init() {
        // Crea el contador de errores
        errorCounter = registry.counter("Aplication_CatalogoResource_primality_errors_total");
    }

    @GET
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            delay = 5,
            delayUnit = ChronoUnit.SECONDS
    )
    @Fallback(fallbackMethod = "fallbackGetProducts")
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response getProducts(@QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("size") @DefaultValue("10") int size,
                                @QueryParam("nombre") String nombre,
                                @QueryParam("categoria") String categoria,
                                @QueryParam("precioMin") Double precioMin,
                                @QueryParam("precioMax") Double precioMax) {
        try{
            if (size > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El tamaño máximo permitido por página es 100").build();
            }
            List<ProductoDTO> productos = catalogoService.obtenerProductos(page, size, nombre, categoria, precioMin, precioMax);
            return Response.ok(productos).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    public Response fallbackGetProducts(int page, int size, String nombre, String categoria, Double precioMin, Double precioMax) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Servicio de catálogo no disponible actualmente. Intente más tarde.")
                .build();
    }

    @POST
    @RolesAllowed("admin")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response addProduct(@Valid ProductoDTO producto) {
        try {
            ProductoDTO nuevoProducto = catalogoService.agregarProducto(producto);
            return Response.status(Response.Status.CREATED).entity(nuevoProducto).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin"})
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response updateProduct(@PathParam("id") Long id, @Valid ProductoDTO producto) {
        try {
            if (catalogoService.actualizarProducto(id, producto)) {
                return Response.ok("Producto actualizado con éxito.").build();
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response deleteProduct(@PathParam("id") Long id) {
        try {
            if (catalogoService.eliminarProducto(id)) {
                return Response.ok("Producto eliminado con éxito.").build();
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Producto con ID " + id + " no encontrado.").build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @GET
    @Path("/{id}")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            delay = 5,
            delayUnit = ChronoUnit.SECONDS
    )
    @Fallback(fallbackMethod = "fallbackGetProductById")
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response getProductById(@PathParam("id") Long id) {
        if (id == null || id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El ID del producto debe ser un número positivo.").build();
        }
        try {
            ProductoDTO producto = catalogoService.obtenerProductoPorId(id);
            if (producto == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto con ID " + id + " no encontrado.").build();
            }
            return Response.ok(producto).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    public Response fallbackGetProductById(Long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Producto no disponible actualmente. No es posible obtener el producto con ID " + id)
                .build();
    }

    @POST
    @RolesAllowed({"user","admin"})
    @Path("/{id}/reserva")
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response reservarStock(@PathParam("id") Long productoId,
                                  @Valid ReservaRequest request) {
        try {
            boolean reservado = catalogoService.reservarStock(productoId, request.cantidad);
            if (reservado) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity("Stock insuficiente").build();
        } catch(WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity("Error reservando stock: " + e.getMessage()).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @GET
    @Path("/{id}/valoraciones")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response listarValoracionesDeProducto(@PathParam("id") Long idProducto,
                                                 @QueryParam("pagina") @DefaultValue("1") int pagina,
                                                 @QueryParam("tamanio") @DefaultValue("10") int tamanio) {
        if (pagina <= 0 || tamanio <= 0 || tamanio > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parámetros de paginación inválidos").build();
        }
        try {
            List<ValoracionDTO> valoraciones = catalogoService.obtenerValoracionesPorProducto(idProducto, pagina, tamanio);
            long total = catalogoService.contarValoracionesPorProducto(idProducto);

            return Response.ok()
                    .entity(new PaginacionResponse<>(valoraciones, pagina, tamanio, total))
                    .build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @GET
    @Path("/{productoId}/valoracion/existe")
    @RolesAllowed("user")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
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

    public record ReservaRequest (
        @Min(1)
        @NotNull
        int cantidad
    ) {}
}