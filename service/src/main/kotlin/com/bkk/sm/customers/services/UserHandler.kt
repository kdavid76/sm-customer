package com.bkk.sm.customers.services

import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserBase
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.bkk.sm.mongo.customers.resources.UserResource
import com.bkk.sm.mongo.customers.validators.UserResourceValidator
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.reactive.function.server.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class UserHandler(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userResourceValidator: UserResourceValidator
) {
    val log = KotlinLogging.logger {}

    suspend fun findAll(request: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(userRepository.findAll().map { UserConverter.toUserResource(it) })
    }

    suspend fun findByUsername(request: ServerRequest): ServerResponse {
        val username = request.pathVariable("username")
        val user = userRepository.findByUsername(username)
        return user?.let {
            return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(UserConverter.toUserResource(it))
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    suspend fun add(request: ServerRequest): ServerResponse {

        val userResource = request.awaitBodyOrNull<UserResource>()
        val errors: Errors? = validateUserResource(userResource)
        errors?.let {
            if(errors.hasErrors()) {
                log.error { "Invalid payload, errors=$errors were found in request body" }
                return ServerResponse.badRequest().bodyValueAndAwait(errors.allErrors)
            }
        } ?: run {
            return ServerResponse.badRequest().buildAndAwait()
        }

        val user: UserBase? = userRepository.findByUsername(userResource!!.username)

        user?.let {
            log.error { "User with userName=${user.username} has already been exists in the system" }
            return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        } ?: run {
            val converted = UserConverter.toUserBase(userResource)

            converted.accountLocked = true
            converted.enabled = false
            converted.registrationTime = Date.from(Instant.now())
            converted.passwordExpiryTime = Date.from(Instant.now().plus(90, ChronoUnit.DAYS))
            converted.activationKey = RandomStringUtils.randomAlphanumeric(32)
            converted.password = passwordEncoder.encode(userResource.password)
            converted.passwordExpiringEnabled = true

            val saved = userRepository.save(converted)

            saved.let {
                log.info { "User[username=${saved.username}, firstName=[${saved.firstName}, lastName=[${saved.lastName}, activationKey=[${saved.activationKey}]] added to datastore" }
                return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(UserConverter.toUserResource(saved))
            }
        }
    }

    private fun validateUserResource(userResource: UserResource?): Errors? {
        if(userResource == null) {
            return null
        }
        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
        userResourceValidator.validate(userResource, errors)

        return errors
    }
}