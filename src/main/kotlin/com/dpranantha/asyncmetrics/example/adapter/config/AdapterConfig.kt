package com.dpranantha.asyncmetrics.example.adapter.config

import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultWebClientFactory.createTcpClientConfig
import com.dpranantha.asyncmetrics.util.webclient.factory.DefaultWebClientFactory.createWebClientJsonContentType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AdapterConfig(val environment: Environment) {

    @Bean
    fun xkcdWebClient(): WebClient {
        return createWebClientJsonContentType(
            createTcpClientConfig(environment, "xkcd")
        )
    }
}
