package com.dpranantha.asyncmetrics.util.coroutine

import com.dpranantha.asyncmetrics.util.coroutine.CoroutineMetric.coroutineMetrics
import com.dpranantha.asyncmetrics.util.coroutine.CoroutineMetric.coroutineMetricsWithNullable
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CoroutineMetricTest {
    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @BeforeEach
    fun setup() {
        meterRegistry.clear()
    }

    @Test
    fun `it should record metrics properly for single request`() {
        runBlocking {
            val dummyValue = coroutineMetrics(
                suspendFunc = suspend {
                    coroutineScope {
                        delay(200)
                        "{}"
                    }
                },
                functionName = "dummy",
                meterRegistry = meterRegistry
            )

            assertEquals("{}", dummyValue)
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1.0, count)
    }

    @Test
    fun `it should record metrics properly for multiple requests`() {
        runBlocking {
            repeat(1000) {
                launch {
                    coroutineMetrics(
                        suspendFunc = suspend {
                            coroutineScope {
                                delay(200)
                                "{}"
                            }
                        },
                        functionName = "dummy",
                        meterRegistry = meterRegistry
                    )
                }
            }
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1000.0, count)
    }

    @Test
    fun `it should record metrics with nullable func properly for single request`() {
        runBlocking {
            val dummyValue: String? = coroutineMetricsWithNullable(
                suspendFunc = suspend {
                    coroutineScope {
                        delay(200)
                        null
                    }
                },
                functionName = "dummy",
                meterRegistry = meterRegistry
            )

            assertEquals(null, dummyValue)
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1.0, count)
    }

    @Test
    fun `it should record metrics with nullable func properly for multiple requests`() {
        runBlocking {
            repeat(1000) {
                launch {
                    coroutineMetricsWithNullable(
                        suspendFunc = suspend {
                            coroutineScope {
                                delay(200)
                                null
                            }
                        },
                        functionName = "dummy",
                        meterRegistry = meterRegistry
                    )
                }
            }
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1000.0, count)
    }

    @Test
    fun `it should record metrics properly for single request on failure`() {
        runBlocking {
            try {
                val dummyValue: String? = coroutineMetricsWithNullable(
                    suspendFunc = suspend {
                        coroutineScope {
                            delay(200)
                            throw Exception("any exception")
                        }
                    },
                    functionName = "dummy",
                    meterRegistry = meterRegistry
                )
            } catch (e: Exception) {
                //Do nothing
            }
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1.0, count)
    }

    @Test
    fun `it should record metrics properly for multiple requests on failure`() {
        runBlocking {
            repeat(1000) {
                launch {
                    supervisorScope {
                        try {
                            coroutineMetricsWithNullable(
                                suspendFunc = suspend {
                                    coroutineScope {
                                        delay(200)
                                        throw Exception("any exception")
                                    }
                                },
                                functionName = "dummy",
                                meterRegistry = meterRegistry
                            )
                        } catch (e: Exception) {
                            //Do nothing
                        }
                    }

                }
            }
        }

        val p99 = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds",
            arrayOf("service", "quantile"),
            arrayOf("dummy", "0.99"))
        assertTrue(p99 >= 0.2)

        val count = meterRegistry.prometheusRegistry.getSampleValue("dummy_statistic_seconds_count",
            arrayOf("service"),
            arrayOf("dummy"))
        assertEquals(1000.0, count)
    }
}
