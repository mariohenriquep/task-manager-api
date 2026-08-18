package com.taskmanager.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes the system clock as a bean so application-layer classes depend on an
 * injectable {@link Clock} instead of calling {@code Instant.now()} directly -
 * this is what lets use case tests use a fixed clock deterministically.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
