package Clientes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CatalogoClient {
    @ConfigProperty(name = "catalogo-service.url")
    String urlCatalogoService;

    public Response comprobarValoracionExsistente(long productoId, String jwtToken) {
        Client client = ClientBuilder.newClient();
        try {
            String targetUrl = urlCatalogoService + "/pedidos/" + productoId + "/valoracion";

            Response response = client
                    .target(targetUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, jwtToken)
                    .get();

            return response;
        } finally {
            client.close();
        }
    }
}