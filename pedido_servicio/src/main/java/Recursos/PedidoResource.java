package Recursos;

import DTO.PedidoDTO;
import Entidades.Pedido;
import Servicios.PedidoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/pedido")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    @Inject
    PedidoService pedidoService;

    @Context
    HttpHeaders headers;

    @POST
    @RolesAllowed({"admin"})
    public Response crearPedido(@Valid Pedido pedido) {
        Pedido nuevoPedido = pedidoService.crearPedido(pedido);
        return Response.status(Response.Status.CREATED).entity(nuevoPedido).build();
    }

    @GET
    @RolesAllowed({"user", "admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response obtenerPedidosPorUsuario(@Context SecurityContext securityContext) {
        String usuarioId = securityContext.getUserPrincipal().getName();
        List<PedidoDTO> pedidos;
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
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
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
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al listar los pedidos: "+e.getMessage()).build();
        }
    }

    @PATCH
    @Path("/{id}/estado")
    @RolesAllowed("admin")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response cambiarEstadoPedido(@PathParam("id") Long id, @QueryParam("estado") String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El parámetro 'estado' es obligatorio y no puede estar vacío").build();
        }
        try {
            pedidoService.actualizarPedido(id,nuevoEstado);
            return Response.ok("Estado del pedido actualizado correctamente").build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cambiar el estado del pedido").build();
        }
    }

    @POST
    @Path("/{id}/valoracion")
    @RolesAllowed({"user","admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response crearValoracion(@PathParam("id") Long pedidoId, @Valid ValoracionRequest valoracionRequest, @Context SecurityContext securityContext) {
        String usuarioId = securityContext.getUserPrincipal().getName();
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        try {
            pedidoService.crearValoracion(pedidoId, usuarioId, valoracionRequest.puntuacion, valoracionRequest.comentario, token);
            return Response.status(Response.Status.CREATED).entity("Valoración creada y enviada correctamente").build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear la valoración: "+e.getMessage()).build();
        }
    }

    public static class FiltroPedidoRequest {
        private String estado;
        private String usuarioId;
        @Positive(message = "El número de página debe ser mayor o igual a 0")
        private Integer pagina;

        @Positive(message = "El tamaño de página debe ser mayor que 0")
        @Max(value = 100, message = "El tamaño máximo permitido por página es 100")
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

    public record ValoracionRequest(
            @Positive(message = "La puntuación debe ser mayor que 0")
            @Max(value = 5, message = "La puntuación no puede ser mayor que 5")
            @NotNull
            int puntuacion,
            @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
            String comentario
    ) {}
}