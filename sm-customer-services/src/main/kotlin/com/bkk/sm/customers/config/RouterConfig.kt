package com.bkk.sm.customers.config

import com.bkk.sm.customers.handlers.impl.MongoCompanyHandlerImpl
import com.bkk.sm.customers.handlers.impl.MongoUserHandlerImpl
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfig {

    private val log = KotlinLogging.logger {}

    @Bean
    fun userRoutes(mongoUserHandlerImpl: MongoUserHandlerImpl) = coRouter {
        before {
            log.info { "Processing User request from ${it.remoteAddress().orElse(null)} with headers=${it.headers()}" }
            it
        }

        "/users".nest {
            headers {
                it.header("API_VERSION")[0] == "V1"
            }.nest {
                GET("", mongoUserHandlerImpl::findAll)
                "/{username}".nest {
                    GET("", mongoUserHandlerImpl::findByUsername)
                    PUT("/activation/{code}", mongoUserHandlerImpl::activate)
                }
                GET("/{username}", mongoUserHandlerImpl::findByUsername)
                contentType(MediaType.APPLICATION_JSON).nest {
                    POST("", mongoUserHandlerImpl::add)
                }
            }
        }
    }

    @Bean
    fun companyRoutes(mongoCompanyHandlerImpl: MongoCompanyHandlerImpl) = coRouter {
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
                it.header("API_VERSION")[0] == "V1"
            }.nest {
                GET("", mongoCompanyHandlerImpl::findAll)

                "/{companycode}".nest {
                    GET("", mongoCompanyHandlerImpl::findByCompanyCode)
                    PUT("/activation/{activationcode}", mongoCompanyHandlerImpl::activate)
                }
                contentType(MediaType.APPLICATION_JSON).nest {
                    POST("", mongoCompanyHandlerImpl::add)
                }
            }
        }
    }
}
