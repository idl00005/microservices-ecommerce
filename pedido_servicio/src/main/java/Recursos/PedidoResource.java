package Recursos;

import DTO.PedidoDTO;
import Entidades.Pedido;
import Servicios.PedidoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    @Inject
    PedidoService pedidoService;

    @Context
    HttpHeaders headers;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry registry;

    private Counter errorCounter;

    @PostConstruct
    public void init() {
        // Crea el contador de errores
        errorCounter = registry.counter("Aplication_PedidoResource_primality_errors_total");
    }

    @POST
    @RolesAllowed({"admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response crearPedido(@Valid Pedido pedido) {
        try{
            Pedido nuevoPedido = pedidoService.crearPedido(pedido);
            return Response.status(Response.Status.CREATED).entity(nuevoPedido).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @GET
    @RolesAllowed({"user", "admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response listarPedidos(
            @Context SecurityContext securityContext,
            @BeanParam @Valid FiltroPedidoRequest filtro
    ) {
        try {
            // Si el caller no es admin, forzamos el filtro a su propio usuario
            String callerId = securityContext.getUserPrincipal().getName();
            if (!securityContext.isUserInRole("admin")) {
                filtro.setUsuarioId(callerId);
            }

            List<PedidoDTO> pedidos = pedidoService.listarPedidos(
                    filtro.getEstado(),
                    filtro.getUsuarioId(),
                    filtro.getPagina(),
                    filtro.getTamanio()
            );
            return Response.ok(pedidos).build();

        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response obtenerPedidoPorId(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        String usuarioId = securityContext.getUserPrincipal().getName();
        Pedido pedido;
        try {
            if (securityContext.isUserInRole("admin")) {
                // Lógica para admin: puede acceder a cualquier pedido
                pedido = pedidoService.obtenerPedidoPorIdParaAdmin(id);
                return Response.ok(pedido).build();
            } else {
                // Lógica para usuario normal: solo puede acceder a sus propios pedidos
                pedido = pedidoService.obtenerPedidoPorIdParaUsuario(id, usuarioId);
                return Response.ok(pedido).build();
            }
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @PATCH
    @Path("/{id}/estado")
    @RolesAllowed("admin")
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response cambiarEstadoPedido(@PathParam("id") Long id, @Valid CambioEstadoRequest request) {
        String nuevoEstado = request.estado;
        try {
            pedidoService.actualizarPedido(id,nuevoEstado);
            return Response.ok("Estado del pedido actualizado correctamente").build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @POST
    @Path("/{id}/valoracion")
    @RolesAllowed({"user","admin"})
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response crearValoracion(@PathParam("id") Long pedidoId, @Valid ValoracionRequest valoracionRequest, @Context SecurityContext securityContext) throws JsonProcessingException {
        String usuarioId = securityContext.getUserPrincipal().getName();
        String token = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        try {
            pedidoService.crearValoracion(pedidoId, usuarioId, valoracionRequest.puntuacion, valoracionRequest.comentario, token);
            return Response.status(Response.Status.CREATED).entity("Valoración creada y enviada correctamente").build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus()).entity(e.getMessage()).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    public static class FiltroPedidoRequest {
        @QueryParam("estado")
        private String estado;

        @QueryParam("usuarioId")
        private String usuarioId;

        @Positive(message = "El número de página debe ser mayor o igual a 0")
        @QueryParam("pagina")
        private Integer pagina = 1;

        @Positive(message = "El tamaño de página debe ser mayor que 0")
        @Max(value = 100, message = "El tamaño máximo permitido por página es 100")
        @QueryParam("tamanio")
        private Integer tamanio = 10;

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

    public record CambioEstadoRequest (
        @NotBlank
        @Pattern(regexp = "PENDIENTE|ENVIADO|EN REPARTO|COMPLETADO|CANCELADO", message = "El estado debe ser uno de los valores permitidos")
        String estado
    ) {}
}