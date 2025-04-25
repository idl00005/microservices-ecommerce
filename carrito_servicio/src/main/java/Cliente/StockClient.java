package Cliente;

import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@ApplicationScoped
public class StockClient {

    // Cambia el puerto si tu servicio de catálogo corre en otro lado
    private final String baseUrl = "http://localhost:8081/catalogo";
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

                Response respuesta = client.target(baseUrl)
                        .path("/{id}/reservar")
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
        String url = "http://localhost:8080/auth/login";
        String usuario = "idl00005@red.ujaen.es";
        String pass = "1234";
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", usuario, pass);

        Client client = ClientBuilder.newBuilder().build();
        try {
            Response resp = client.target(url)
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

}