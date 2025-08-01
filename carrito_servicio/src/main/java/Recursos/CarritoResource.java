package Recursos;

import DTO.CarritoItemDetalleDTO;
import Entidades.CarritoItem;
import Entidades.OrdenPago;
import Servicios.CarritoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.util.List;

@Path("/carrito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CarritoResource {

    @Inject
    CarritoService carritoService;

    @POST
    @Path("/pago")
    @RolesAllowed({"user", "admin"})
    public Response iniciarPago(@Context SecurityContext ctx, @Valid IniciarPagoRequest request) {
        String userId = ctx.getUserPrincipal().getName();
        try {
            OrdenPago orden = carritoService.iniciarPago(userId, request.direccion, request.telefono);
            return Response.ok(orden).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al iniciar el pago: " + e.getMessage()).build();
        }
    }

    public record IniciarPagoRequest(
            @NotNull(message = "La dirección no puede ser nula")
            @Pattern(regexp = "\\+?[0-9]{9,15}", message = "El número de teléfono debe ser válido") String telefono,
            @NotNull(message = "El teléfono no puede ser nulo") String direccion
    ) {}

    @POST
    @Path("/")
    @RolesAllowed({"user", "admin"})
    public Response agregarProducto(@Valid AgregarProductoRequest req, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        try {
            CarritoItemDetalleDTO item = carritoService.agregarProducto(userId, req.productoId, req.cantidad);
            return Response.ok(item).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al agregar el producto al carrito").build();
        }
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
    @Retry(maxRetries = 3)
    public Response obtenerCarrito(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        try {
            List<CarritoItemDetalleDTO> carrito = carritoService.obtenerCarrito(userId);
            return Response.ok(carrito).build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        }
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
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al vaciar el carrito").build();
        }
    }

    @PUT
    @Path("/{productoId}")
    @RolesAllowed({"user","admin"})
    public Response actualizarCantidad(@PathParam("productoId") Long productoId,
                                       @Valid ActualizarCantidadRequest cantidadr,
                                       @Context SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal().getName();
        try {
            // Si la cantidad es 0, se interpreta como eliminación.
            if (cantidadr.cantidad == 0) {
                carritoService.eliminarProducto(userId, productoId);
                return Response.ok("Producto eliminado del carrito.").build();
            } else {
                CarritoItem actualizado = carritoService.actualizarCantidadProducto(userId, productoId, cantidadr.cantidad);
                return Response.ok(actualizado).build();
            }
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    public record ActualizarCantidadRequest (@Min(value = 0, message = "La cantidad debe ser al menos 0")
                                             @NotNull(message = "La cantidad no puede ser nula")
                                             int cantidad){}
}
