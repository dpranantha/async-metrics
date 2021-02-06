package com.dpranantha.asyncmetrics.util.webclient.factory

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.netty.handler.timeout.ReadTimeoutException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

object DefaultResilience4JCircuitBreakerFactory {

    fun createCircuitBreakerRegistry(meterRegistry: MeterRegistry): CircuitBreakerRegistry {
        val registry = CircuitBreakerRegistry.ofDefaults()
        TaggedCircuitBreakerMetrics
            .ofCircuitBreakerRegistry(registry)
            .bindTo(meterRegistry)
        return registry
    }

    fun createDefaultCircuitBreaker(
        circuitBreakerRegistry: CircuitBreakerRegistry,
        commandName: String,
        slowCallDurationThresholdInMs: Long,
        waitDurationInOpenStateInMs: Long
    ): CircuitBreaker = circuitBreakerRegistry.circuitBreaker(
        commandName,
        CircuitBreakerConfig.custom()
            .ignoreExceptions(
                WebClientResponseException.BadRequest::class.java,
                WebClientResponseException.NotFound::class.java
            )
            .recordExceptions(
                WebClientResponseException.InternalServerError::class.java,
                WebClientResponseException.GatewayTimeout::class.java,
                ReadTimeoutException::class.java
            )
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThresholdInMs))
            .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenStateInMs))
            .build()
    )

    val DEFAULT_RESILIENCE_TIMEOUT: Duration = Duration.ofMillis(2000)
}
