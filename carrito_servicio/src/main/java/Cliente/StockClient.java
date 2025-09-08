package Cliente;

import DTO.ProductoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class StockClient {

    @ConfigProperty(name = "catalogo-service.url")
    String urlCatalogoService;

    @Inject
    RedisDataSource redisDataSource;

    /**
     * Intenta reservar stock para todos los productos especificados.
     * Es totalmente síncrono: realiza 1 petición HTTP por cada producto.
     * Devuelve true si todos los productos se pueden reservar, false si alguno falla.
     */
    public void reservarStock(Map<Long, Integer> productos, String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new RuntimeException("No hay token JWT válido. Llama a obtenerJwtParaCarrito primero.");
        }

        // Construir payload batch según ReservaBatchRequest
        Map<String, Object> bodyJson = Map.of(
                "items", productos.entrySet()
                        .stream()
                        .map(e -> Map.of(
                                "productoId", e.getKey(),
                                "cantidad", e.getValue()
                        ))
                        .collect(Collectors.toList())
        );

        try (Client client = ClientBuilder.newBuilder().build();
             Response respuesta = client.target(urlCatalogoService)
                     .path("/reservas")  // nuevo endpoint batch
                     .request(MediaType.APPLICATION_JSON)
                     .header(HttpHeaders.AUTHORIZATION, jwt)
                     .post(Entity.json(bodyJson))) {

            int status = respuesta.getStatus();
            String respBody = respuesta.readEntity(String.class); // leer antes de cerrar

            if (status == 200) {
                // Todos los productos reservados correctamente
                return;
            } else if (status == 409) {
                // Algunos productos no pudieron reservarse
                throw new WebApplicationException("Stock insuficiente al realizar la reserva: " + respBody, Response.status(409).build());
            } else if (status == 401 || status == 403) {
                throw new WebApplicationException("No autorizado reservando stock: " + status + " -> " + respBody);
            } else {
                throw new WebApplicationException("Error inesperado reservando stock. Status: " + status + " Body: " + respBody
                        + " Token empleado: "+ jwt);
            }

        }
    }

    @CacheResult(cacheName = "producto-cache")
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            delay = 5,
            delayUnit = ChronoUnit.SECONDS
    )
    @Fallback(fallbackMethod = "fallbackObtenerProductoPorId")
    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    public ProductoDTO obtenerProductoPorId(Long id) {
        try (Client client = ClientBuilder.newBuilder().build()) {
            Response response = client.target(urlCatalogoService)
                    .path("/{id}")
                    .resolveTemplate("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() == 404) {
                return null;
            }

            if (response.getStatus() != 200) {
                throw new RuntimeException("Error consultando producto" +
                        " con ID " + id + ": status " + response.getStatus() +
                        " -> " + response.readEntity(String.class));
            }

            return response.readEntity(ProductoDTO.class);
        }
    }

    public ProductoDTO fallbackObtenerProductoPorId(Long id) {
        // Intentar obtener desde caché
        ProductoDTO cachedProducto = getProductoFromRedis(id);
        if (cachedProducto != null) {
            return cachedProducto;
        } else {
            throw new WebApplicationException("Producto no disponible actualmente. No es posible obtener el producto con ID " + id,
                    Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private ProductoDTO getProductoFromRedis(Long id) {
        String key = "producto-cache:" + id;

        ValueCommands<String, String> valueCommands =
                redisDataSource.value(String.class, String.class);

        String json = valueCommands.get(key);

        if (json != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, ProductoDTO.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

}