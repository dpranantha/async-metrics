package com.dpranantha.asyncmetrics.util.reactor

import com.dpranantha.asyncmetrics.util.helper.DefaultConstants.DEFAULT_TIME_BUCKETS
import com.dpranantha.asyncmetrics.util.helper.TimerBuilder.statisticTimerBuilder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import reactor.core.publisher.Mono
import java.time.Duration

object ReactorMetric {

    fun <T> Mono<T?>.withStatisticalMetrics(
        flowName: String,
        moreTags: Map<String, String> = emptyMap(),
        timeBuckets: Array<Duration> = DEFAULT_TIME_BUCKETS,
        meterRegistry: MeterRegistry
    ): Mono<T?> {
        require(timeBuckets.isNotEmpty()) { "timeBuckets are mandatory to create latency distribution histogram" }
        return this.name(flowName)
            .metrics()
            .assembleWithStatistics(flowName, moreTags, timeBuckets, meterRegistry)
    }

    private fun <T> Mono<T?>.assembleWithStatistics(
        metricsLabelTag: String,
        moreTags: Map<String, String>,
        timeBuckets: Array<Duration>,
        meterRegistry: MeterRegistry
    ): Mono<T?> {
        var subscribeToTerminateSample: Timer.Sample? = null

        val subscribeToCompleteTimer = statisticTimerBuilder(
            metricsLabelTag,
            "complete",
            moreTags,
            timeBuckets
        )
            .register(meterRegistry)

        val subscribeToCancelTimer = statisticTimerBuilder(
            metricsLabelTag,
            "cancel",
            moreTags,
            timeBuckets
        )
            .register(meterRegistry)

        val subscribeToErrorTimer = statisticTimerBuilder(
            metricsLabelTag,
            "error",
            moreTags,
            timeBuckets
        )
            .register(meterRegistry)

        return this
            .doOnSubscribe { subscribeToTerminateSample = Timer.start(meterRegistry) }
            .doOnSuccess { subscribeToTerminateSample?.stop(subscribeToCompleteTimer) }
            .doOnCancel { subscribeToTerminateSample?.stop(subscribeToCancelTimer) }
            .doOnError { subscribeToTerminateSample?.stop(subscribeToErrorTimer) }

    }

}
