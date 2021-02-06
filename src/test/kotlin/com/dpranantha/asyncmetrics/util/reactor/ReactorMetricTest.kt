package com.dpranantha.asyncmetrics.util.reactor

import com.dpranantha.asyncmetrics.util.reactor.ReactorMetric.withStatisticalMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Duration

internal class ReactorMetricTest {
    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @BeforeEach
    fun setup() {
        meterRegistry.clear()
    }

    @Test
    fun `it should record metrics properly for single request`() {
        val dummyValue = Mono.just("{}")
            .delayElement(Duration.ofMillis(200))
            .withStatisticalMetrics(
                flowName = "dummy",
                meterRegistry = meterRegistry)
            .block()

        Assertions.assertEquals("{}", dummyValue)
        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "status", "quantile"),
            arrayOf("dummy", "complete", "0.99"))
        Assertions.assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service", "status"),
            arrayOf("dummy", "complete"))
        Assertions.assertEquals(1.0, count)
    }

    @Test
    fun `it should record metrics properly for multiple requests`() {
        IntRange(0, 99).map {
            Mono.just("{}")
                .delayElement(Duration.ofMillis(200))
                .withStatisticalMetrics(
                    flowName = "dummy",
                    meterRegistry = meterRegistry
                )}
            .parallelStream()
            .forEach { it.block() }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "status", "quantile"),
            arrayOf("dummy", "complete", "0.99"))
        Assertions.assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service", "status"),
            arrayOf("dummy", "complete"))
        Assertions.assertEquals(100.0, count)
    }

    @Test
    fun `it should record metrics properly on empty mono for single request`() {
        val dummyValue = Mono.justOrEmpty(null as String?)
            .withStatisticalMetrics(
                flowName = "dummy",
                meterRegistry = meterRegistry)
            .block()

        Assertions.assertEquals(null, dummyValue)
        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "status", "quantile"),
            arrayOf("dummy", "complete", "0.99"))
        Assertions.assertTrue(p99 > 0)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service", "status"),
            arrayOf("dummy", "complete"))
        Assertions.assertEquals(1.0, count)
    }

    @Test
    fun `it should record metrics properly  on empty mono for multiple requests`() {
        IntRange(0, 99).map {
            Mono.justOrEmpty(null as String?)
                .delayElement(Duration.ofMillis(200))
                .withStatisticalMetrics(
                    flowName = "dummy",
                    meterRegistry = meterRegistry
                )}
            .parallelStream()
            .forEach { it.block() }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "status", "quantile"),
            arrayOf("dummy", "complete", "0.99"))
        Assertions.assertTrue(p99 > 0)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service", "status"),
            arrayOf("dummy", "complete"))
        Assertions.assertEquals(100.0, count)
    }

    @Test
    fun `it should record metrics properly on error mono`() {
        Mono.error<String?>(Exception("any exception"))
            .withStatisticalMetrics(
                flowName = "dummy",
                meterRegistry = meterRegistry)
            .onErrorResume { Mono.empty<String?>()  }
            .block()

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "status", "quantile"),
            arrayOf("dummy", "error", "0.99"))
        Assertions.assertTrue(p99 > 0)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service", "status"),
            arrayOf("dummy", "error"))
        Assertions.assertEquals(1.0, count)
    }

}
