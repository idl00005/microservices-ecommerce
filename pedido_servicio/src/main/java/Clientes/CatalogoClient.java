package Clientes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class CatalogoClient {
    @ConfigProperty(name = "catalogo-service.url")
    String urlCatalogoService;

    @Retry(delay = 200, delayUnit = ChronoUnit.MILLIS)
    public boolean comprobarValoracionExistente(long productoId, String jwtToken) {
        try (Client client = ClientBuilder.newClient()) {
            String targetUrl = urlCatalogoService + "/pedidos/" + productoId + "/valoracion";

            try (Response response = client
                    .target(targetUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, jwtToken)
                    .get()) {

                if (response.getStatus() != 200) {
                    throw new WebApplicationException("Respuesta no OK: " + response.getStatus(), response.getStatus());
                }

                return response.readEntity(Boolean.class);
            }
        }
    }

}