package silas.yudi.applytrackmcp.support;

import org.testcontainers.containers.GenericContainer;

/**
 * Um contêiner para toda a execução dos testes; o Testcontainers o remove no final.
 */
public final class DynamoDbLocalContainer {

    private static final GenericContainer<?> CONTAINER =
            new GenericContainer<>("amazon/dynamodb-local:3.3.2").withExposedPorts(8000);

    static {
        CONTAINER.start();
    }

    private DynamoDbLocalContainer() {
    }

    public static String endpoint() {
        return "http://%s:%d".formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(8000));
    }
}
