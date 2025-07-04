package Health;

import Repositorios.PedidoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ReadinessCheck implements HealthCheck {

    @Inject
    PedidoRepository repositorioPedido;

    @Inject
    KafkaHealthClient kafkaHealthClient;

    @Override
    public HealthCheckResponse call() {
        boolean dbOk = checkDatabaseConnection();
        boolean kafkaOk = kafkaHealthClient.isHealthy();
        boolean allOk = dbOk && kafkaOk;

        return HealthCheckResponse.named("Readiness check")
                .status(allOk)
                .withData("database", dbOk)
                .withData("kafka", kafkaOk)
                .build();
    }

    private boolean checkDatabaseConnection() {
        return repositorioPedido.checkDatabaseConnection();
    }
}


