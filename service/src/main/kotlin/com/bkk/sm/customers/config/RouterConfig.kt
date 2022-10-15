package com.bkk.sm.customers.config

import com.bkk.sm.customers.services.UserHandler
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfig(private val userHandler: UserHandler) {

    val log = KotlinLogging.logger {}

    @Bean
    fun userRoutes(userHandler: UserHandler) = coRouter {
        before {
            log.info{"Processing request from ${it.remoteAddress().orElse(null)} with headers=${it.headers()}"}
            it
        }

        "/users".nest {
            headers {
                it.header("API_VERSION")[0].equals("V1")
            }.nest {
                GET("", userHandler::findAll)

                contentType(MediaType.APPLICATION_JSON).nest {
                    POST("", userHandler::add)
                }

                "/{username}".nest {
                    GET("", userHandler::findByUsername)
                }
            }
        }
    }
}