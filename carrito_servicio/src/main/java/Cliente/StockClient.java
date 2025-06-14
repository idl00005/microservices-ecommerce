package Cliente;

import DTO.ProductoDTO;
import io.quarkus.cache.CacheResult;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

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

    private String JwtToken = "";

    /**
     * Intenta reservar stock para todos los productos especificados.
     * Es totalmente síncrono: realiza 1 petición HTTP por cada producto.
     * Devuelve true si todos los productos se pueden reservar, false si alguno falla.
     */
    public Response reservarStock(Map<Long, Integer> productos, Long ordenId) {
        Client client = ClientBuilder.newBuilder().build();
        try {
            for (Map.Entry<Long, Integer> entrada : productos.entrySet()) {
                Long productoId = entrada.getKey();
                Integer cantidad = entrada.getValue();

                Response respuesta = client.target(urlCatalogoService)
                        .path("/{id}/reserva")
                        .resolveTemplate("id", productoId)
                        .queryParam("cantidad", cantidad)
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + JwtToken)
                        .post(Entity.json(null)); // El recurso espera POST vacío

                if (respuesta.getStatus() != 200) {
                    respuesta.close();
                    return respuesta;
                }
                respuesta.close();
            }
            return Response.ok(ordenId).build();
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
    public void obtenerJwtParaCarrito() {
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", adminUser, adminPassword);

        Client client = ClientBuilder.newBuilder().build();
        try {
            Response resp = client.target(urlAuthService)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(json));
            if (resp.getStatus() == 200) {
                JwtToken = resp.readEntity(String.class);
            } else {
                throw new RuntimeException("No se obtuvo token: status " + resp.getStatus());
            }
        } finally {
            client.close();
        }
    }

    @CacheResult(cacheName = "producto-cache")
    public ProductoDTO obtenerProductoPorId(Long id) {
        Client client = ClientBuilder.newBuilder().build();
        try {
            return client.target(urlCatalogoService)
                    .path("/{id}")
                    .resolveTemplate("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get(ProductoDTO.class);
        } finally {
            client.close();
        }
    }

}