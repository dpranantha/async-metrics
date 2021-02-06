package com.dpranantha.asyncmetrics.util.webclient

import com.dpranantha.asyncmetrics.util.webclient.extension.WebClientRequest
import com.dpranantha.asyncmetrics.util.webclient.extension.getOrDefaultAsMono
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
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
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestConfig::class]
)
internal class WebClientMonoExtTest {
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

        val realResponse = webclient.getOrDefaultAsMono(
            request = defaultRequest,
            defaultValue = "{}",
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

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
    fun `test a failed client call due to timeout should return default value`() {
        targetMockWebServer.enqueue(MockResponse()
            .setHeadersDelay(5000, TimeUnit.MILLISECONDS)
            .setResponseCode(200)
            .setBody("{ 'id': 1, 'name': 'John Doe' }"))

        val realResponse = webclient.getOrDefaultAsMono(
            request = defaultRequest,
            defaultValue = "{}",
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        StepVerifier.create(realResponse)
            .expectNextMatches{ it == "{}" }
            .expectComplete()
            .verify()
    }

    @Test
    fun `test a failed client call due to error without fallback should just complete`() {
        targetMockWebServer.enqueue(MockResponse().setResponseCode(400))

        var realResponse = webclient.getOrDefaultAsMono(
            request = defaultRequest,
            defaultValue = null as String?,
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        StepVerifier.create(realResponse)
            .expectComplete()
            .verify()

        targetMockWebServer.enqueue(MockResponse().setResponseCode(500))

        realResponse = webclient.getOrDefaultAsMono(
            request = defaultRequest,
            defaultValue = null as String?,
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        StepVerifier.create(realResponse)
            .expectComplete()
            .verify()
    }

    @Test
    fun `test circuit breaker for error 4xx should NOT be open`() {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(400))
            webclient.getOrDefaultAsMono(
                request = defaultRequest,
                defaultValue = null as String?,
                circuitBreaker = circuitBreaker,
                logger = logger,
                meterRegistry = meterRegistry
            )
            .onErrorResume { Mono.empty() }
            .block()
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `test circuit breaker for error 5xx from target server should be open`() {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(500))
            webclient.getOrDefaultAsMono(
                request = defaultRequest,
                defaultValue = null as String?,
                circuitBreaker = circuitBreaker,
                logger = logger,
                meterRegistry = meterRegistry
            )
            .onErrorResume { Mono.empty() }
            .block()
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebClientMonoExtTest::class.java)
        @JvmStatic
        private val defaultRequest = WebClientRequest(
            uriPath = "/v1/some-url",
            serviceCommandName = "GET_MOCK_DATA"
        )
    }
}
