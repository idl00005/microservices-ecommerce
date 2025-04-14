package Cliente;

import DTO.ProductoDTO;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/catalogo")
@RegisterRestClient(configKey = "catalogo-api")
public interface ProductoClient {

    @GET
    @Path("/{id}")
    ProductoDTO obtenerProductoPorId(@PathParam("id") String id);
}
