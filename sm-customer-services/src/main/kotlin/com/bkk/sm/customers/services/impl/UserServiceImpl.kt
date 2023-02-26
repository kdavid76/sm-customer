package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.errors.responses.FormErrorResource
import com.bkk.sm.customers.services.UserService
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userResourceValidator: UserResourceValidator,
    private val passwordEncoder: PasswordEncoder,
) : UserService {

    val log = KotlinLogging.logger {}

    override suspend fun findAllUsers(): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(userRepository.findAll().map { UserConverter.toUserResource(it) })
    }

    override suspend fun findUserByUsername(username: String): ServerResponse {
        val user = userRepository.findByUsername(username)

        return user?.let {
            return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(UserConverter.toUserResource(it))
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    override suspend fun registerUser(userResource: UserResource): ServerResponse {
        val errors = validateUser(userResource)
        if (errors.hasErrors()) {
            log.error { "Invalid payload, errors=$errors were found in request body" }
            return ServerResponse.badRequest().bodyValueAndAwait(
                FormErrorResource.Builder()
                    .objectName(UserResource::class.java.name)
                    .addFieldErrors(errors)
                    .build(),
            )
        }

        val user = userRepository.findByUsername(userResource.username)

        user?.let {
            log.error { "User with userName=${user.username} has already been existing in the system" }
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

            log.info { "User[username=${saved.username}, firstName=${saved.firstName}, lastName=${saved.lastName}, activationKey=${saved.activationKey}] has been added to datastore" }
            return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(UserConverter.toUserResource(saved))
        }
    }

    private suspend fun validateUser(userResource: UserResource): Errors {
        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
        userResourceValidator.validate(userResource, errors)
        if (userResource.password == null) {
            errors.rejectValue("password", "errors.user.resource.password.missing")
        }
        return errors
    }
}
