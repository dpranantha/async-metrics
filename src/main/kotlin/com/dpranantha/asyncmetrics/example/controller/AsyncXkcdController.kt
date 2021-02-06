package com.dpranantha.asyncmetrics.example.controller

import com.dpranantha.asyncmetrics.example.controller.exception.ComicNotFoundException
import com.dpranantha.asyncmetrics.example.model.Xkcd
import com.dpranantha.asyncmetrics.example.service.XkcdService
import com.dpranantha.asyncmetrics.util.coroutine.CoroutineMetric.coroutineMetrics
import com.dpranantha.asyncmetrics.util.reactor.ReactorMetric.withStatisticalMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springdoc.api.ErrorMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1")
class AsyncXkcdController(
    private val xkcdService: XkcdService,
    private val meterRegistry: MeterRegistry
) {

    @Operation(summary = "Get a comic by its id from xkcd")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Found the comic",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Xkcd::class))]
        ), ApiResponse(
            responseCode = "400",
            description = "Invalid id supplied",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorMessage::class))]
        ), ApiResponse(
            responseCode = "404",
            description = "Comic not found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorMessage::class))]
        )]
    )
    @GetMapping("/comic/{id}")
    @Throws(
        ComicNotFoundException::class
    )
    suspend fun getComicById(@PathVariable("id") id: String): ResponseEntity<Xkcd> = coroutineScope {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
        coroutineMetrics(
            suspendFunc = suspend {
                val comic = xkcdService.getComicById(id) ?: throw ComicNotFoundException("Comic with id= $id cannot be found in XKCD")
                ResponseEntity.ok().body(comic)
            },
            functionName = "GET_COMIC_BY_ID_CONTROLLER",
            meterRegistry = meterRegistry
        )
    }

    @Operation(summary = "Get a comic by its id from xkcd using mono")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Found the comic",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Xkcd::class))]
        ), ApiResponse(
            responseCode = "400",
            description = "Invalid id supplied",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorMessage::class))]
        ), ApiResponse(
            responseCode = "404",
            description = "Comic not found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorMessage::class))]
        )]
    )
    @GetMapping("/comic-mono/{id}")
    @Throws(
        ComicNotFoundException::class
    )
    fun getComicByIdAsMono(@PathVariable("id") id: String): Mono<ResponseEntity<Xkcd>> = Mono.defer {
        logger.debug("Thread XkcdClient: ${Thread.currentThread().name}")
        xkcdService.getComicByIdAsMono(id)
            .map { ResponseEntity.ok().body(it) }
            .switchIfEmpty(
                Mono.error(ComicNotFoundException("Comic with id= $id NOT FOUND!"))
            )
            .withStatisticalMetrics(
                flowName = "GET_COMIC_BY_ID_MONO_CONTROLLER",
                meterRegistry = meterRegistry
            )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncXkcdController::class.java)
    }
}
