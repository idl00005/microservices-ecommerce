package Recursos;

import DTO.PedidoDTO;
import Entidades.Pedido;
import Servicios.PedidoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
        List<PedidoDTO> pedidos = null;
        try {
            pedidos = pedidoService.obtenerPedidosPorUsuario(usuarioId);
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener los pedidos").build();
        }
        return Response.ok(pedidos).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    public Response obtenerPedidoPorId(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        String usuarioId = securityContext.getUserPrincipal().getName();
        Pedido pedido;

        if (securityContext.isUserInRole("admin")) {
            // Lógica para admin: puede acceder a cualquier pedido
            try {
                pedido = pedidoService.obtenerPedidoPorIdParaAdmin(id);
            } catch (WebApplicationException e) {
                return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener el pedido").build();
            }

            return Response.ok(pedido).build();
        } else {
            // Lógica para usuario normal: solo puede acceder a sus propios pedidos
            try {
                pedido = pedidoService.obtenerPedidoPorId(id, usuarioId);
            } catch (WebApplicationException e) {
                return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener el pedido").build();
            }
            return Response.ok(pedido).build();
        }
    }

    @GET
    @Path("/filtro")
    @RolesAllowed("admin")
    public Response listarPedidos(@Valid FiltroPedidoRequest filtro) {
        try {
            List<PedidoDTO> pedidos = pedidoService.listarPedidos(
                    filtro.getEstado(),
                    filtro.getUsuarioId(),
                    filtro.getPagina(),
                    filtro.getTamanio()
            );
            return Response.ok(pedidos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al listar los pedidos").build();
        }
    }

    public static class FiltroPedidoRequest {
        private String estado;
        private String usuarioId;
        @Positive
        private Integer pagina;

        @Positive
        private Integer tamanio;

        // Getters y Setters
        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(String usuarioId) {
            this.usuarioId = usuarioId;
        }

        public Integer getPagina() {
            return pagina;
        }

        public void setPagina(Integer pagina) {
            this.pagina = pagina;
        }

        public Integer getTamanio() {
            return tamanio;
        }

        public void setTamanio(Integer tamanio) {
            this.tamanio = tamanio;
        }
    }
}