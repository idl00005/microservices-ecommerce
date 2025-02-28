package uja;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.inject.Inject;

@Path("/secure")
public class SecureResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public String secureEndpoint() {
        return "Acceso permitido para " + jwt.getName();
    }
}