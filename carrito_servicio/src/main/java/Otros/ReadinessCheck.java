package Otros;

import Repositorios.CarritoItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ReadinessCheck implements HealthCheck {

    @Inject
    CarritoItemRepository repositorioCarrito;

    @Override
    public HealthCheckResponse call() {
        boolean dbOk = checkDatabaseConnection();

        if (dbOk) {
            return HealthCheckResponse.up("Readiness check: todas las dependencias están operativas");
        } else {
            return HealthCheckResponse.down("Readiness check: dependencias no operativas");
        }
    }

    private boolean checkDatabaseConnection() {
        return repositorioCarrito.checkDatabaseConnection();
    }
}

