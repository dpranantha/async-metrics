package com.dpranantha.asyncmetrics.example.adapter.config

import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultResilience4JCircuitBreakerFactory.createCircuitBreakerRegistry
import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultResilience4JCircuitBreakerFactory.createDefaultCircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CircuitBreakerConfig(private val meterRegistry: MeterRegistry) {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry = createCircuitBreakerRegistry(meterRegistry)

    @Bean
    fun xkcdCircuitBreaker(
        circuitBreakerRegistry: CircuitBreakerRegistry
    ): CircuitBreaker = createDefaultCircuitBreaker(
        circuitBreakerRegistry,
        "XKCD_GET_COMIC_INFO_BY_ID",
        1000,
        5000
    )
}
