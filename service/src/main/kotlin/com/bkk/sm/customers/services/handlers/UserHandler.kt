package com.bkk.sm.customers.services.handlers

import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.customers.services.UserService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.reactive.function.server.buildAndAwait

@Component
class UserHandler(
    private val userService: UserService
) {
    val log = KotlinLogging.logger {}

    suspend fun findAll(request: ServerRequest): ServerResponse {
        log.info { "Finding all users registered in the system" }
        return userService.findAllUsers()
    }

    suspend fun findByUsername(request: ServerRequest): ServerResponse {
        val username = request.pathVariable("username")
        log.info { "Finding user by username=${username}" }
        return userService.findUserByUsername(username)
    }

    suspend fun add(request: ServerRequest): ServerResponse {
        val userResource = request.awaitBodyOrNull<UserResource>()

        userResource?.let {
            log.info { "Registering user=${userResource}" }
            return userService.registerUser(userResource)
        }

        log.error { "Registering user failed due to missing payload." }
        return ServerResponse.badRequest().buildAndAwait()
    }
/*
    private fun validateUserResource(userResource: UserResource?): Errors? {
        if (userResource == null) {
            return null
        }
        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
        userResourceValidator.validate(userResource, errors)

        return errors
    }
 */
}