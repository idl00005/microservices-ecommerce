package Cliente;

import DTO.ProductoDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProductoClient {

    @ConfigProperty(name = "catalogo-service.url")
    String baseUrl;

    public ProductoDTO obtenerProductoPorId(Long id) {
        Client client = ClientBuilder.newBuilder().build();
        try {
            return client.target(baseUrl)
                    .path("/{id}")
                    .resolveTemplate("id", id)
                    .request(MediaType.APPLICATION_JSON)
                    .get(ProductoDTO.class);
        } finally {
            client.close();
        }
    }
}