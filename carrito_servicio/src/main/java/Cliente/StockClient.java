package Cliente;

import DTO.ProductoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import io.quarkus.scheduler.Scheduled;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.quarkus.redis.client.RedisClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class StockClient {

    @ConfigProperty(name = "catalogo-service.url")
    String urlCatalogoService;

    @ConfigProperty(name = "auth-service.url")
    String urlAuthService;

    @ConfigProperty(name = "auth.admin.user")
    String adminUser;

    @ConfigProperty(name = "auth.admin.password")
    String adminPassword;

    @Inject
    RedisClient redisClient;

    String jwtToken = "";

    /**
     * Intenta reservar stock para todos los productos especificados.
     * Es totalmente síncrono: realiza 1 petición HTTP por cada producto.
     * Devuelve true si todos los productos se pueden reservar, false si alguno falla.
     */
    public void reservarStock(Map<Long, Integer> productos) {
        Client client = ClientBuilder.newBuilder().build();
        try {
            if (jwtToken == null || jwtToken.isBlank()) {
                throw new RuntimeException("No hay token JWT válido. Llama a obtenerJwtParaCarrito primero.");
            }

            for (Map.Entry<Long, Integer> entrada : productos.entrySet()) {
                Long productoId = entrada.getKey();
                Integer cantidad = entrada.getValue();

                // Construir body JSON que el recurso espera (ReservaRequest)
                String bodyJson = String.format("{\"cantidad\": %d}", cantidad);

                Response respuesta = client.target(urlCatalogoService)
                        .path("/{id}/reserva")
                        .resolveTemplate("id", productoId)
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .post(Entity.json(bodyJson));

                try {
                    int status = respuesta.getStatus();
                    String respBody = respuesta.readEntity(String.class); // leer antes de cerrar

                    if (status == 200) {
                        // reservado OK -> continuar con el siguiente producto
                        continue;
                    } else if (status == 409) {
                        throw new WebApplicationException("Stock insuficiente al realizar la reserva", Response.status(403).build());
                    } else if (status == 401 || status == 403) {
                        throw new WebApplicationException("No autorizado reservando stock: " + status + " -> " + respBody);
                    } else {
                        throw new WebApplicationException("Error inesperado reservando stock. Status: " + status + " Body: " + respBody);
                    }
                } finally {
                    respuesta.close();
                }
            }
        } finally {
            client.close();
        }
    }

    @PostConstruct
    public void obtenerJwtAlArrancar() {
        if (System.getProperty("test.env") != null) {
            return; // No hacer nada en el entorno de pruebas
        }
        obtenerJwtParaCarrito();
    }

    @Scheduled(every = "50m")
    @Retry(maxRetries = 4, delay = 2, delayUnit = ChronoUnit.SECONDS)
    public void obtenerJwtParaCarrito() {
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", adminUser, adminPassword);

        Client client = ClientBuilder.newBuilder().build();
        try {
            Response resp = client.target(urlAuthService)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(json));
            if (resp.getStatus() == 200) {
                jwtToken = resp.readEntity(String.class);
            } else {
                throw new RuntimeException("No se obtuvo token: status " + resp.getStatus());
            }
        } finally {
            client.close();
        }
    }

    @CacheResult(cacheName = "producto-cache")
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            delay = 5,
            delayUnit = ChronoUnit.SECONDS
    )
    @Fallback(fallbackMethod = "fallbackObtenerProductoPorId")
    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
    public ProductoDTO obtenerProductoPorId(Long id) {
        Client client = ClientBuilder.newBuilder().build();
        try {
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
        } finally {
            client.close();
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
        io.vertx.redis.client.Response response = redisClient.get(key);
        if (response != null) {
            String json = response.toString();
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