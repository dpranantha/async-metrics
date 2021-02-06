package com.dpranantha.asyncmetrics.util.webclient

import com.dpranantha.asyncmetrics.util.reactor.ReactorMetric.withStatisticalMetrics
import com.dpranantha.asyncmetrics.util.webclient.extension.WebClientRequest
import com.dpranantha.asyncmetrics.util.webclient.extension.getOrDefaultAsMono
import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultResilience4JCircuitBreakerFactory.DEFAULT_RESILIENCE_TIMEOUT
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.micrometer.core.instrument.MeterRegistry
import io.netty.handler.timeout.ReadTimeoutException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestConfig::class]
)
internal class WebClientWithResilienceTest {
    private val targetMockWebServer = MockWebServer()

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    @Qualifier("mockCircuitBreaker")
    private lateinit var circuitBreaker: CircuitBreaker

    @Autowired
    @Qualifier("mockWebClient")
    private lateinit var webclient: WebClient

    @BeforeEach
    fun setup() {
        targetMockWebServer.start(9999)

        meterRegistry.clear()
        circuitBreaker.reset()
    }

    @AfterEach
    fun tearDown() {
        targetMockWebServer.shutdown()
    }

    @Test
    fun `test a successful client call`() {
        targetMockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{ 'id': 1, 'name': 'John Doe' }"))

        val realResponse = webclient.get()
            .uri("/v1/some-url")
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .withStatisticalMetrics(
                flowName = "mockCommand",
                meterRegistry = meterRegistry
            )
            .take(DEFAULT_RESILIENCE_TIMEOUT)

        StepVerifier.create(realResponse)
            .expectNextMatches{ it == "{ 'id': 1, 'name': 'John Doe' }" }
            .expectComplete()
            .verify()

        assertEquals(
            "/v1/some-url",
            targetMockWebServer.takeRequest().path
        )
    }

    @Test
    fun `test a failed client call due to timeout should throw ReadTimeoutException`() {
        targetMockWebServer.enqueue(MockResponse()
            .setHeadersDelay(5000, TimeUnit.MILLISECONDS)
            .setResponseCode(200)
            .setBody("{ 'id': 1, 'name': 'John Doe' }"))

        val realResponse = webclient.get()
            .uri("/v1/some-url")
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .withStatisticalMetrics(
                flowName = "mockCommand",
                meterRegistry = meterRegistry
            )
            .take(DEFAULT_RESILIENCE_TIMEOUT)

        StepVerifier.create(realResponse)
            .expectErrorMatches{ it.cause is ReadTimeoutException }
            .verify()
    }

    @Test
    fun `test a failed client call due to error should throw exceptions`() {
        targetMockWebServer.enqueue(MockResponse().setResponseCode(400))

        var realResponse = webclient.get()
            .uri("/v1/some-url")
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .withStatisticalMetrics(
                flowName = "mockCommand",
                meterRegistry = meterRegistry
            )
            .take(DEFAULT_RESILIENCE_TIMEOUT)

        StepVerifier.create(realResponse)
            .expectErrorMatches{ it is WebClientResponseException.BadRequest }
            .verify()

        targetMockWebServer.enqueue(MockResponse().setResponseCode(500))

        realResponse = webclient.get()
            .uri("/v1/some-url")
            .retrieve()
            .bodyToMono(String::class.java)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .withStatisticalMetrics(
                flowName = "mockCommand",
                meterRegistry = meterRegistry
            )
            .take(DEFAULT_RESILIENCE_TIMEOUT)

        StepVerifier.create(realResponse)
            .expectErrorMatches{ it is WebClientResponseException.InternalServerError }
            .verify()
    }

    @Test
    fun `test circuit breaker for error 4xx should NOT be open`() {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(400))
            webclient.get()
                .uri("/v1/some-url")
                .retrieve()
                .bodyToMono(String::class.java)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .withStatisticalMetrics(
                    flowName = "mockCommand",
                    meterRegistry = meterRegistry
                )
                .take(DEFAULT_RESILIENCE_TIMEOUT)
                .onErrorResume{ Mono.empty() }
                .block()
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `test circuit breaker for error 5xx from target server should be open`() {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(500))
            webclient.get()
                .uri("/v1/some-url")
                .retrieve()
                .bodyToMono(String::class.java)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .withStatisticalMetrics(
                    flowName = "mockCommand",
                    meterRegistry = meterRegistry
                )
                .take(DEFAULT_RESILIENCE_TIMEOUT)
                .onErrorResume{ Mono.empty() }
                .block()
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }
}
