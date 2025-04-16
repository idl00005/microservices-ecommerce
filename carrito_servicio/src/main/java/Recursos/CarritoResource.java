package Recursos;

import Entidades.CarritoItem;
import Servicios.CarritoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/carrito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CarritoResource {

    @Inject
    CarritoService carritoService;

    @POST
    @Path("/agregar")
    @RolesAllowed({"user", "admin"})
    public Response agregarProducto(@Valid AgregarProductoRequest req, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();

        CarritoItem item = carritoService.agregarProducto(userId, req.productoId, req.cantidad);
        return Response.ok(item).build();
    }

    public static class AgregarProductoRequest {
        @NotNull(message = "El productoId no puede ser nulo")
        public Long productoId;
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @NotNull(message = "La cantidad no puede ser nula")
        public int cantidad;
    }

    @GET
    @Path("/")
    @RolesAllowed({"user", "admin"})
    public Response obtenerCarrito(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();

        List<CarritoItem> carrito = carritoService.obtenerCarrito(userId);
        return Response.ok(carrito).build();
    }

    @DELETE
    @Path("/{productoId}")
    @RolesAllowed({"user", "admin"})
    public Response eliminarProducto(@PathParam("productoId") Long productoId, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        try {
            carritoService.eliminarProducto(userId, productoId);
            return Response.noContent().build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/")
    @RolesAllowed({"user", "admin"})
    public Response vaciarCarrito(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        try {
            carritoService.vaciarCarrito(userId);
            return Response.noContent().build(); // Retorna 204 si es exitoso
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al vaciar el carrito").build(); // Maneja errores genéricos
        }
    }

    @PUT
    @Path("/{productoId}")
    @RolesAllowed({"user","admin"})
    public Response actualizarCantidad(@PathParam("productoId") Long productoId,
                                       @Valid ActualizarCantidadRequest cantidad,
                                       @Context SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal().getName();
        try {
            // Si la cantidad es 0, se interpreta como eliminación.
            if (cantidad.getCantidad() == 0) {
                carritoService.eliminarProducto(userId, productoId);
                return Response.ok("Producto eliminado del carrito.").build();
            } else {
                CarritoItem actualizado = carritoService.actualizarCantidadProducto(userId, productoId, cantidad.getCantidad());
                return Response.ok(actualizado).build();
            }
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    public static class ActualizarCantidadRequest {
        @Min(value = 1, message = "La cantidad debe ser al menos 0")
        @NotNull(message = "La cantidad no puede ser nula")
        private int cantidad;

        public int getCantidad() {
            return cantidad;
        }
        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }
    }
}
