package com.bkk.sm.customers.config

import com.bkk.sm.customers.services.handlers.CompanyHandler
import com.bkk.sm.customers.services.handlers.UserHandler
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfig() {

    val log = KotlinLogging.logger {}

    @Bean
    fun userRoutes(userHandler: UserHandler) = coRouter {
        before {
            log.info { "Processing User request from ${it.remoteAddress().orElse(null)} with headers=${it.headers()}" }
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

    @Bean
    fun companyRoutes(companyHandler: CompanyHandler) = coRouter {
        before {
            log.info {
                "Processing Company request from ${
                    it.remoteAddress().orElse(null)
                } with headers=${it.headers()}"
            }
            it
        }

        "/companies".nest {
            headers {
                it.header("API_VERSION")[0].equals("V1")
            }.nest {
                GET("", companyHandler::findAll)
                GET("/{companycode}", companyHandler::findByCompanyCode)

                contentType(MediaType.APPLICATION_JSON).nest {
                    POST("", companyHandler::add)
                }
            }
        }
    }
}
