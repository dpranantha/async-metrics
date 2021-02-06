package com.dpranantha.asyncmetrics.util.webclient.factory

import com.dpranantha.asyncmetrics.util.webclient.config.TcpClientConfig
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.TcpClient
import java.util.concurrent.TimeUnit.MILLISECONDS

object DefaultWebClientFactory {

    /**
     * convention spring properties: httpClient.$serviceName
     */
    fun createTcpClientConfig(
        env: Environment,
        serviceName: String
    ): TcpClientConfig = TcpClientConfig(
        env.getRequiredProperty("httpClient.$serviceName.baseUrl", String::class.java),
        serviceName,
        env.getRequiredProperty("httpClient.$serviceName.connectionTimeout", Int::class.java),
        env.getRequiredProperty("httpClient.$serviceName.requestTimeout", Long::class.java),
        env.getRequiredProperty("httpClient.$serviceName.maxConnections", Int::class.java),
        env.getRequiredProperty("httpClient.$serviceName.threadPoolSize", Int::class.java)
    )

    fun createWebClientJsonContentType(config: TcpClientConfig): WebClient {
        val tcpClient = TcpClient.create(
            ConnectionProvider.builder(config.poolName)
                .maxConnections(config.maxConnections)
                .fifo()
                .build())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectionTimeout)
            .doOnConnected { connection ->
                connection.addHandlerLast(ReadTimeoutHandler(config.requestTimeout, MILLISECONDS))
                connection.addHandlerLast(WriteTimeoutHandler(config.requestTimeout, MILLISECONDS)) }
            .runOn(NioEventLoopGroup(config.threadPoolSize))
        return WebClient
            .builder()
            .baseUrl(config.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient)))
            .build()
    }
}
