package com.dpranantha.asyncmetrics.example.service

import com.dpranantha.asyncmetrics.example.adapter.XkcdClient
import com.dpranantha.asyncmetrics.example.model.Xkcd
import com.dpranantha.asyncmetrics.util.coroutine.CoroutineMetric.coroutineMetricsWithNullable
import com.dpranantha.asyncmetrics.util.reactor.ReactorMetric.withStatisticalMetrics
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class XkcdService(
    private val xkcdClient: XkcdClient,
    private val meterRegistry: MeterRegistry
) {

    suspend fun getComicById(id: String): Xkcd? = coroutineScope {
        coroutineMetricsWithNullable(
            suspendFunc = suspend {
                logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
                xkcdClient
                    .getComicById(id)
                    ?.let { Xkcd(it.num, it.img, it.title, it.month, it.year, it.transcript) }
            },
            functionName = "service.getComicById",
            meterRegistry = meterRegistry
        )
    }

    fun getComicByIdAsMono(id: String): Mono<Xkcd?> = Mono.defer {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
        xkcdClient
            .getComicByIdAsMono(id)
            .map { it?.let {
                Xkcd(it.num, it.img, it.title, it.month, it.year, it.transcript) } }
    }.withStatisticalMetrics(
        flowName = "service.getComicByIdAsMono",
        meterRegistry = meterRegistry
    )

    companion object {
        private val logger = LoggerFactory.getLogger(XkcdService::class.java)
    }
}
