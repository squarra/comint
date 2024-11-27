package org.example;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class IntegrationTestResource implements QuarkusTestResourceLifecycleManager {

    private static final RabbitMQContainer RABBIT_MQ_CONTAINER =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));

    private static final FixedHostPortGenericContainer<?> EXTERNAL_CONTAINER =
            new FixedHostPortGenericContainer<>("comint-mock-external:latest")
                    .withFixedExposedPort(8082, 8080);

    @Override
    public Map<String, String> start() {
        RABBIT_MQ_CONTAINER.start();
        EXTERNAL_CONTAINER.start();
        return Map.of(
                "rabbitmq.host", RABBIT_MQ_CONTAINER.getHost(),
                "rabbitmq.port", String.valueOf(RABBIT_MQ_CONTAINER.getAmqpPort())
        );
    }

    @Override
    public void stop() {
        RABBIT_MQ_CONTAINER.stop();
        EXTERNAL_CONTAINER.stop();
    }
}
