package Health;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class KafkaHealthClient {

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.bootstrap.servers")
    String bootstrapServers;

    public boolean isHealthy() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("request.timeout.ms", "2000");

        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(2, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
