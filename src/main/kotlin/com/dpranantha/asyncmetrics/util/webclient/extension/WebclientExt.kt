package com.dpranantha.asyncmetrics.util.webclient.extension

import com.dpranantha.asyncmetrics.util.helper.DefaultConstants.DEFAULT_TIME_BUCKETS
import com.dpranantha.asyncmetrics.util.reactor.ReactorMetric.withStatisticalMetrics
import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultResilience4JCircuitBreakerFactory.DEFAULT_RESILIENCE_TIMEOUT
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactive.awaitSingleOrDefault
import kotlinx.coroutines.reactive.awaitSingleOrNull
import org.slf4j.Logger
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

suspend inline fun <reified T : Any> WebClient
        .getOrDefault(request: WebClientRequest,
                      defaultValue: T? = null,
                      circuitBreaker: CircuitBreaker,
                      timeout: Duration = DEFAULT_RESILIENCE_TIMEOUT,
                      logger: Logger,
                      meterRegistry: MeterRegistry,
                      timeBuckets: Array<Duration> = DEFAULT_TIME_BUCKETS
): T? {
    val response = getOrDefaultAsMono(
        request, defaultValue, circuitBreaker, timeout, logger, meterRegistry, timeBuckets
    )
    return if (defaultValue != null) response.awaitSingleOrDefault(defaultValue)
    else response.awaitSingleOrNull()
}

inline fun <reified T: Any> WebClient
        .getOrDefaultAsMono(request: WebClientRequest,
                            defaultValue: T? = null,
                            circuitBreaker: CircuitBreaker,
                            timeout: Duration = DEFAULT_RESILIENCE_TIMEOUT,
                            logger: Logger,
                            meterRegistry: MeterRegistry,
                            timeBuckets: Array<Duration> = DEFAULT_TIME_BUCKETS
): Mono<T?> = this
    .get()
    .uri(request.uriPath)
    .headers { header -> request.headers.forEach { header.set(it.key, it.value) } }
    .retrieve()
    .bodyToMono(T::class.java)
    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
    .doOnSuccess {
        logger.debug("Successful get value: $it")
    }
    .doOnError {
        logger.error("Error ${request.serviceCommandName}: ${it.message} since ${it.localizedMessage}")
    }
    .onErrorResume {
        Mono.justOrEmpty(defaultValue)
    }
    .doFinally {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
    }
    .withStatisticalMetrics(
        flowName = request.serviceCommandName,
        timeBuckets = timeBuckets,
        meterRegistry = meterRegistry
    )
    .take(timeout)

data class WebClientRequest(val uriPath: String,
                            val headers: Map<String, String> = emptyMap(),
                            val body: String? = null,
                            val serviceCommandName: String)
