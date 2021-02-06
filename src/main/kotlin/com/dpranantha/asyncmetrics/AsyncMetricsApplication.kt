package com.dpranantha.asyncmetrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AsyncMetricsApplication

fun main(args: Array<String>) {
    runApplication<AsyncMetricsApplication>(*args)
}
