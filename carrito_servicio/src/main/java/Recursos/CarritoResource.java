package Recursos;

import Entidades.CarritoItem;
import Servicios.CarritoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/carrito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CarritoResource {

    @Inject
    CarritoService carritoService;

    @POST
    @Path("/agregar")
    public Response agregarProducto(AgregarProductoRequest req, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();

        CarritoItem item = carritoService.agregarProducto(userId, req.productoId, req.cantidad);
        return Response.ok(item).build();
    }

    public static class AgregarProductoRequest {
        public String productoId;
        public int cantidad;
    }
}
