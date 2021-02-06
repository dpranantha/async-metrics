package com.dpranantha.asyncmetrics.util.webclient

import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultResilience4JCircuitBreakerFactory
import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultWebClientFactory
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class TestConfig(private val environment: Environment) {
    @Bean
    fun mockMeterRegistry(): MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @Bean
    fun mockCircuitBreakerRegistry(
        @Qualifier("mockMeterRegistry") meterRegistry: MeterRegistry
    ): CircuitBreakerRegistry = DefaultResilience4JCircuitBreakerFactory.createCircuitBreakerRegistry(meterRegistry)

    @Bean
    fun mockCircuitBreaker(
        @Qualifier("mockCircuitBreakerRegistry") circuitBreakerRegistry: CircuitBreakerRegistry
    ): CircuitBreaker = DefaultResilience4JCircuitBreakerFactory.createDefaultCircuitBreaker(
        circuitBreakerRegistry,
        "MOCK_SERVICE_GET_MOCK_DATA_BY_ID",
        1000,
        5000
    )

    @Bean
    fun mockWebClient(): WebClient {
        return DefaultWebClientFactory.createWebClientJsonContentType(
            DefaultWebClientFactory.createTcpClientConfig(environment, "mock")
        )
    }
}
