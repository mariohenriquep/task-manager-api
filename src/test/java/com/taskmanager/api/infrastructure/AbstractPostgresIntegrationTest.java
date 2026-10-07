package com.taskmanager.api.infrastructure;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need a real PostgreSQL instance.
 *
 * <p>The static {@code @Container} field is started before the first test of each subclass and
 * stopped after its last one, so <b>every test class gets its own fresh, empty database</b> (a
 * new container, on a new port, per class). That isolation is what lets
 * {@code TaskRepositoryAdapterTest} assert on exact table contents. The price is one container
 * start (about a second) per class; a JVM-wide singleton would be faster but would let rows leak
 * between classes, so don't switch to one without making those assertions independent of
 * leftover rows.
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
}
