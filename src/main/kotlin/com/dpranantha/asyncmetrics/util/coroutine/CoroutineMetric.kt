package com.dpranantha.asyncmetrics.util.coroutine

import com.dpranantha.asyncmetrics.util.helper.DefaultConstants.DEFAULT_TIME_BUCKETS
import com.dpranantha.asyncmetrics.util.helper.TimerBuilder.statisticTimerBuilder
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import java.util.concurrent.TimeUnit

object CoroutineMetric {

    suspend fun <T: Any> coroutineMetrics(
        suspendFunc: suspend () -> T,
        functionName: String,
        moreTags: Map<String, String> = emptyMap(),
        timeBuckets: Array<Duration> = DEFAULT_TIME_BUCKETS,
        meterRegistry: MeterRegistry
    ): T = coroutineMetricsWithNullable(suspendFunc, functionName, moreTags, timeBuckets, meterRegistry)!!

    suspend fun <T: Any> coroutineMetricsWithNullable(
        suspendFunc: suspend () -> T?,
        functionName: String,
        moreTags: Map<String, String> = emptyMap(),
        timeBuckets: Array<Duration> = DEFAULT_TIME_BUCKETS,
        meterRegistry: MeterRegistry
    ): T? {
        require(timeBuckets.isNotEmpty()) { "timeBuckets are mandatory to create latency distribution histogram" }
        val timer = statisticTimerBuilder(
            metricsLabelTag = functionName,
            moreTags = moreTags,
            timeBuckets = timeBuckets
        )
            .register(meterRegistry)
        val clock = meterRegistry.config().clock()
        val start = clock.monotonicTime()
        try {
            return suspendFunc.invoke()
        } finally {
            val end = clock.monotonicTime()
            timer.record(end - start, TimeUnit.NANOSECONDS)
        }
    }
}
