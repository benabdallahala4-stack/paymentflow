package com.paymentflow.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers PostgreSQL base for all real-database tests (rule 10).
 *
 * <p>Deliberately NOT annotated with @Testcontainers/@Container: that JUnit extension
 * stops the container after each test CLASS, but this container is a single static
 * instance shared (via inheritance) across every subclass, started once here and reused
 * for the whole test run (the "singleton container" Testcontainers pattern) - the JVM
 * shutdown / Ryuk resource reaper cleans it up when the test run ends.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PostgresTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("paymentflow")
            .withUsername("paymentflow")
            .withPassword("paymentflow")
            .withReuse(false);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Kafka/Redis stay pointed at localhost (not running in CI) - both are
        // optional/non-blocking infra per rule 6/7, so tests that don't touch them
        // (all of ours) are unaffected.
    }
}
