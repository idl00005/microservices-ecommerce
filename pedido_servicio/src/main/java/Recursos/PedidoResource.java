package Recursos;

import Entidades.Pedido;
import Servicios.PedidoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/pedido")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    @Inject
    PedidoService pedidoService;

    @POST
    public Response crearPedido(@Valid Pedido pedido) {
        Pedido nuevoPedido = pedidoService.crearPedido(pedido);
        return Response.status(Response.Status.CREATED).entity(nuevoPedido).build();
    }

    @GET
    @RolesAllowed({"user", "admin"})
    public Response obtenerPedidosPorUsuario(@Context SecurityContext securityContext) {
        String usuarioId = securityContext.getUserPrincipal().getName();
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorUsuario(usuarioId);
        return Response.ok(pedidos).build();
    }
}