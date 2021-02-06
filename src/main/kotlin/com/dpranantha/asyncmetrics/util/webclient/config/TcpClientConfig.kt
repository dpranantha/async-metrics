package com.dpranantha.asyncmetrics.util.webclient.config

data class TcpClientConfig(
    val baseUrl: String,
    val poolName: String,
    val connectionTimeout: Int,
    val requestTimeout: Long,
    val maxConnections: Int,
    val threadPoolSize: Int
)
