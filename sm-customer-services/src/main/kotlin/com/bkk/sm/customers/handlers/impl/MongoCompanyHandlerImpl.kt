package com.bkk.sm.customers.handlers.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.errors.responses.FormErrorResource
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.handlers.CompanyHandler
import com.bkk.sm.customers.utils.CompanyValidator
import com.bkk.sm.customers.utils.UserValidator
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.company.Company
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
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
class MongoCompanyHandlerImpl(
    private val userResourceValidator: UserResourceValidator,
    private val companyResourceValidator: CompanyResourceValidator,
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CompanyHandler {
    val log = KotlinLogging.logger {}

    override suspend fun findAll(request: ServerRequest): ServerResponse {
        log.info { "Finding all companies registered in the system" }
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(companyRepository.findAll().map { CompanyConverter.toCompanyResource(it) })
    }

    override suspend fun findByCompanyCode(request: ServerRequest): ServerResponse {
        val companyCode = request.pathVariable("companycode")
        val company = companyRepository.findByCode(companyCode)

        return if (company != null) {
            ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(CompanyConverter.toCompanyResource(company))
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }

    override suspend fun add(request: ServerRequest): ServerResponse {
        val companyAndUserResource = request.awaitBodyOrNull<CompanyAndUserResource>()

        return if (companyAndUserResource != null) {
            log.info { "Registering company=${companyAndUserResource.companyResource} and user=${companyAndUserResource.userResource ?: "N/A"}" }
            registerCompanyAndUser(companyAndUserResource)
        } else {
            log.error { "Registering company failed due to missing payload." }
            ServerResponse.badRequest().buildAndAwait()
        }
    }

    override suspend fun activate(request: ServerRequest): ServerResponse {
        val companycode = request.pathVariable("companycode")
        val token = request.pathVariable("activationcode")

        val company = companyRepository.findCompanyByCodeAndActivationTokenAfter(companycode, token)

        return if (company != null) {
            activateCompany(company)
        } else {
            ServerResponse.notFound().buildAndAwait()
        }
    }

    private suspend fun registerCompanyAndUser(companyAndUserResource: CompanyAndUserResource): ServerResponse {
        val errors = validate(companyAndUserResource)
        if (errors != null) {
            return ServerResponse.badRequest().bodyValueAndAwait(errors)
        }
        // Step 1: Find company and return conflict if it exists
        val companyResource = companyAndUserResource.companyResource
        val company = companyRepository.findByCode(companyResource.code)
        company?.let {
            log.error { "Company code=${it.code} already exists in database" }
            return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        }

        // Step 2: Save the company into database
        val savedCompany = companyRepository.save(CompanyConverter.toCompany(companyResource))
        log.info { "Successfully saved company=$savedCompany" }

        val userResource = companyAndUserResource.userResource
            ?: return ServerResponse.status(HttpStatus.CREATED)
                .bodyValueAndAwait(
                    CompanyAndUserResource(
                        companyResource = CompanyConverter.toCompanyResource(savedCompany),
                        userResource = null,
                    ),
                )

        // Step 3: Check if user exists
        var user = userRepository.findByUsername(userResource.username)

        // Step 4: Create user profile if the user is a new one
        if (user == null) {
            val now = Date.from(Instant.now())
            user = UserConverter.toUserBase(userResource)
            user.accountLocked = true
            user.enabled = false
            user.registrationTime = now
            user.lastModificationTime = now
            user.passwordExpiryTime = Date.from(Instant.now().plus(90, ChronoUnit.DAYS))
            user.passwordExpiringEnabled = true
            user.activationKey = RandomStringUtils.randomAlphanumeric(32)
            user.password = passwordEncoder.encode(userResource.password)
        }

        // Step 5: add company role to user
        user.addCompanyRole(CompanyRole(Roles.ROLE_ADMIN, savedCompany.code))

        // Step 6: save or update user
        val savedUser = userRepository.save(user)

        return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(
            CompanyAndUserResource(
                CompanyConverter.toCompanyResource(savedCompany),
                UserConverter.toUserResource(savedUser),
            ),
        )
    }

    private fun validate(companyAndUserResource: CompanyAndUserResource): FormErrorResource? {
        val userResource = companyAndUserResource.userResource
        var userErrors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
        val companyErrors =
            CompanyValidator.validateCompany(companyAndUserResource.companyResource, companyResourceValidator)

        if (companyAndUserResource.isNewUser) {
            userErrors = userResource?.let {
                UserValidator.validateUser(it, userResourceValidator)
            } ?: BeanPropertyBindingResult(null, UserResource::class.java.name)
        }

        if (companyErrors.hasErrors() || userErrors.hasErrors()) {
            log.error { "Invalid payload, companyErrors=$companyErrors and userErrors=$userErrors were found in request body" }
            return FormErrorResource.Builder()
                .objectName(CompanyAndUserResource::class.java.name)
                .addFieldErrors(companyErrors)
                .addFieldErrors(userErrors)
                .build()
        }

        return null
    }

    private suspend fun activateCompany(company: Company): ServerResponse {
        company.activationTime = Date.from(Instant.now())
        company.activationKey = null
        company.lastModificationTime = Date.from(Instant.now())
        company.enabled = true

        val saved = companyRepository.save(company)
        log.info { "Company=$saved is activated" }

        return ServerResponse.ok().bodyValueAndAwait(saved)
    }
}
