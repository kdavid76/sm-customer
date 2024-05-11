package com.bkk.sm.customers.handlers.impl

import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.errors.responses.FormErrorResource
import com.bkk.sm.customers.handlers.UserHandler
import com.bkk.sm.customers.utils.UserValidator
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class MongoUserHandlerImpl(
    private val userRepository: UserRepository,
    private val userResourceValidator: UserResourceValidator,
    private val passwordEncoder: PasswordEncoder,
) : UserHandler {
    private val log = KotlinLogging.logger {}

    override suspend fun findAll(request: ServerRequest): ServerResponse {
        log.info { "Finding all users registered in the system" }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(userRepository.findAll().map { UserConverter.toUserResource(it) })
    }

    override suspend fun findByUsername(request: ServerRequest): ServerResponse {
        val username = request.pathVariable("username")
        val user = userRepository.findByUsername(username)

        return if (user != null) {
            log.debug { "User=$username found." }
            return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(UserConverter.toUserResource(user))
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }

    override suspend fun add(request: ServerRequest): ServerResponse {
        val userResource = request.awaitBodyOrNull<UserResource>()

        return if (userResource != null) {
            log.info { "Registering user=$userResource" }
            registerUser(userResource)
        } else {
            log.error { "Registering user failed due to missing payload." }
            ServerResponse.badRequest().buildAndAwait()
        }
    }

    override suspend fun activate(request: ServerRequest): ServerResponse {
        val username = request.pathVariable("username")
        val code = request.pathVariable("code")

        val user = userRepository.findByUsernameAndActivationKeyAfter(username, code)
        return if (user != null) {
            activateUser(user)
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }

    private suspend fun registerUser(userResource: UserResource): ServerResponse {
        val errors = UserValidator.validateUser(userResource, userResourceValidator)
        if (errors.hasErrors()) {
            log.error { "Invalid payload for registering user, errors=$errors were found in request body" }
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
            val saved = convertAndSaveUser(userResource)
            log.info { "User[username=${saved.username}, firstName=${saved.firstName}, lastName=${saved.lastName}, activationKey=${saved.activationKey}] has been added to datastore" }
            return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(UserConverter.toUserResource(saved))
        }
    }

    private suspend fun activateUser(userProfile: UserProfile): ServerResponse {
        userProfile.activationKey = null
        userProfile.enabled = true
        userProfile.accountLocked = false
        userProfile.activatedTime = Date.from(Instant.now())

        val saved = userRepository.save(userProfile)
        log.info { "User=$saved is activated" }

        return ServerResponse.ok().bodyValueAndAwait(saved)
    }

    private suspend fun convertAndSaveUser(userResource: UserResource): UserProfile {
        val converted = UserConverter.toUserBase(userResource)

        converted.accountLocked = true
        converted.enabled = false
        converted.registrationTime = Date.from(Instant.now())
        converted.passwordExpiryTime = Date.from(Instant.now().plus(90, ChronoUnit.DAYS))
        converted.activationKey = RandomStringUtils.randomAlphanumeric(32)
        converted.password = passwordEncoder.encode(userResource.password)
        converted.passwordExpiringEnabled = true

        return userRepository.save(converted)
    }
}
