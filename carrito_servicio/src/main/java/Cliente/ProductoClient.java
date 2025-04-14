package Cliente;

import DTO.ProductoDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
public class ProductoClient {

    private final String baseUrl = "http://localhost:8081/catalogo";

    public ProductoDTO obtenerProductoPorId(Integer id) {
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