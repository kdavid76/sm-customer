package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.errors.responses.FormErrorResource
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.services.CompanyService
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
class CompanyServiceImpl(
    private val userResourceValidator: UserResourceValidator,
    private val companyResourceValidator: CompanyResourceValidator,
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CompanyService {

    val log = KotlinLogging.logger {}

    override suspend fun findAllCompanies(): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(companyRepository.findAll().map { CompanyConverter.toCompanyResource(it) })
    }

    override suspend fun findCompany(companyCode: String): ServerResponse {
        val company = companyRepository.findByCode(companyCode)

        return company?.let {
            return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(CompanyConverter.toCompanyResource(it))
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    @Transactional
    override suspend fun registerCompany(companyResource: CompanyResource, userResource: UserResource?): ServerResponse {
        val companyErrors = validateCompany(companyResource)
        val userErrors = userResource?.let {
            validateUser(it)
        } ?:  BeanPropertyBindingResult(null, UserResource::class.java.name)

        if (companyErrors.hasErrors() || userErrors.hasErrors()) {
            log.error { "Invalid payload, companyErrors=${companyErrors} and userErrors=${userErrors} were found in request body" }
            return ServerResponse.badRequest().bodyValueAndAwait(
                FormErrorResource.Builder()
                .objectName(CompanyAndUserResource::class.java.name)
                .addFieldErrors(companyErrors)
                .addFieldErrors(userErrors)
                .build())
        }

        val company = companyRepository.findByCode(companyResource.code)
        company?.let {
            log.error { "Company with code=${it.code} already exists" }
            return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        }

        val saved = companyRepository.save(CompanyConverter.toCompany(companyResource))
        log.info { "Successfully saved company=${saved}" }

        var user: UserProfile? = null
        userResource?.let {
            user = userRepository.findByUsername(it.username)
            if (user == null){
                if (userResource.password == null) {
                    userErrors.rejectValue("password", "errors.user.resource.password.missing")
                    return ServerResponse.badRequest().bodyValueAndAwait(
                        FormErrorResource.Builder()
                            .objectName(CompanyAndUserResource::class.java.name)
                            .addFieldErrors(userErrors)
                            .build())
                }
                user = UserConverter.toUserBase(userResource)
                user!!.accountLocked = true
                user!!.enabled = false
                user!!.registrationTime =  Date.from(Instant.now())
                user!!.lastModificationTime =  Date.from(Instant.now())
                user!!.passwordExpiryTime = Date.from(Instant.now().plus(90, ChronoUnit.DAYS))
                user!!.passwordExpiringEnabled = true
                user!!.activationKey = RandomStringUtils.randomAlphanumeric(32)
                user!!.password = passwordEncoder.encode(userResource.password)
            }
            user = addRoleToUser(user!!, CompanyRole(Roles.ROLE_ADMIN, saved.code))
        }

        return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(
            CompanyAndUserResource(
                CompanyConverter.toCompanyResource(saved),
                if (user == null) null else UserConverter.toUserResource(user !!)
            )
        )

    }

    private suspend fun validateUser(userResource: UserResource): Errors {
        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
        userResourceValidator.validate(userResource, errors)
        return errors
    }

    private suspend fun validateCompany(companyResource: CompanyResource): Errors {
        val errors: Errors = BeanPropertyBindingResult(companyResource, CompanyResource::class.java.name)
        companyResourceValidator.validate(companyResource,errors)
        return errors
    }

    private suspend fun addRoleToUser(userProfile: UserProfile, companyRole: CompanyRole): UserProfile {
        userProfile.addCompanyRole(companyRole)
        userProfile.accountLocked
        return userRepository.save(userProfile)
    }
}