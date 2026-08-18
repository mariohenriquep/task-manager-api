package com.taskmanager.api;

import com.taskmanager.api.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the entire Spring context - domain, application, infrastructure,
 * wired against a real Postgres instance - starts up without errors.
 */
@SpringBootTest
class TaskManagerApiApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
