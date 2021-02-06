package com.dpranantha.asyncmetrics.util.webclient

import com.dpranantha.asyncmetrics.util.webclient.extension.WebClientRequest
import com.dpranantha.asyncmetrics.util.webclient.extension.getOrDefault
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
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
internal class WebClientCoroutineExtTest {
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
    fun `test a successful client call`() = runBlocking {
        targetMockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{ 'id': 1, 'name': 'John Doe' }"))

        val realResponse = webclient.getOrDefault(
            request = defaultRequest,
            defaultValue = "{}",
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        assertEquals("{ 'id': 1, 'name': 'John Doe' }", realResponse)
        assertEquals(
            "/v1/some-url",
            targetMockWebServer.takeRequest().path
        )
    }

    @Test
    fun `test a failed client call due to timeout should return default value`() = runBlocking {
        targetMockWebServer.enqueue(MockResponse()
            .setHeadersDelay(5000, TimeUnit.MILLISECONDS)
            .setResponseCode(200)
            .setBody("{ 'id': 1, 'name': 'John Doe' }"))

        val realResponse = webclient.getOrDefault(
            request = defaultRequest,
            defaultValue = "{}",
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        assertEquals("{}", realResponse)
    }

    @Test
    fun `test a failed client call due to error without fallback should return null`() = runBlocking {
        targetMockWebServer.enqueue(MockResponse().setResponseCode(400))

        var realResponse = webclient.getOrDefault(
            request = defaultRequest,
            defaultValue = null as String?,
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        assertEquals(null, realResponse)

        targetMockWebServer.enqueue(MockResponse().setResponseCode(500))

        realResponse = webclient.getOrDefault(
            request = defaultRequest,
            defaultValue = null as String?,
            circuitBreaker = circuitBreaker,
            logger = logger,
            meterRegistry = meterRegistry
        )

        assertEquals(null, realResponse)
    }

    @Test
    fun `test circuit breaker for error 4xx should NOT be open`() = runBlocking {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(400))
            webclient.getOrDefault(
                request = defaultRequest,
                defaultValue = null as String?,
                circuitBreaker = circuitBreaker,
                logger = logger,
                meterRegistry = meterRegistry
            )
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    @Test
    fun `test circuit breaker for error 5xx from target server should be open`() = runBlocking {
        repeat(100) {
            targetMockWebServer.enqueue(MockResponse().setResponseCode(500))
            webclient.getOrDefault(
                request = defaultRequest,
                defaultValue = null as String?,
                circuitBreaker = circuitBreaker,
                logger = logger,
                meterRegistry = meterRegistry
            )
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebClientCoroutineExtTest::class.java)
        @JvmStatic
        private val defaultRequest = WebClientRequest(
            uriPath = "/v1/some-url",
            serviceCommandName = "GET_MOCK_DATA"
        )
    }
}
