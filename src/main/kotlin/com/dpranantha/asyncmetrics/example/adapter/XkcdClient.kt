package com.dpranantha.asyncmetrics.example.adapter

import com.dpranantha.asyncmetrics.example.adapter.model.XkcdResponse
import com.dpranantha.asyncmetrics.util.webclient.extension.WebClientRequest
import com.dpranantha.asyncmetrics.util.webclient.extension.getOrDefault
import com.dpranantha.asyncmetrics.util.webclient.extension.getOrDefaultAsMono
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class XkcdClient(
    @Qualifier("xkcdCircuitBreaker") private val circuitBreaker: CircuitBreaker,
    @Qualifier("xkcdWebClient") private val webClient: WebClient,
    private val meterRegistry: MeterRegistry
    ) {

    suspend fun getComicById(comicId: String): XkcdResponse? = withContext(Dispatchers.IO) {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
        webClient.getOrDefault(
            request = WebClientRequest(uriPath = "/$comicId/info.0.json", serviceCommandName = circuitBreaker.name),
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )
    }

    fun getComicByIdAsMono(comicId: String): Mono<XkcdResponse?> = Mono.defer {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
        webClient.getOrDefaultAsMono(
            request = WebClientRequest(uriPath = "/$comicId/info.0.json", serviceCommandName = circuitBreaker.name),
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(XkcdClient::class.java)
    }
}
