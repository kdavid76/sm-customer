package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyWithAdminResource
import com.bkk.sm.common.customer.validators.CompanyWithAdminResourceValidator
import com.bkk.sm.common.model.Roles
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

@Component
class CompanyHandler(
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
    private val companyWithAdminResourceValidator: CompanyWithAdminResourceValidator
) {
    val log = KotlinLogging.logger {}

    suspend fun findAll(request: ServerRequest): ServerResponse {
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(companyRepository.findAll().map { CompanyConverter.toCompanyResource(it) })
    }

    suspend fun findByCompanyCode(request: ServerRequest): ServerResponse {
        val code = request.pathVariable("companycode")
        val company = companyRepository.findByCode(code)
        return company?.let {
            return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(CompanyConverter.toCompanyResource(it))
        } ?: ServerResponse.notFound().buildAndAwait()
    }

    @Transactional
    suspend fun add(request: ServerRequest): ServerResponse {

        val companyWithAdminResource = request.awaitBodyOrNull<CompanyWithAdminResource>()
        val errors: Errors? = validateCompanyWithAdminResource(companyWithAdminResource)
        errors?.let {
            if (errors.hasErrors()) {
                log.error { "Invalid payload, errors=$errors were found in request body" }
                return ServerResponse.badRequest().bodyValueAndAwait(errors.allErrors)
            }
        } ?: run {
            log.error { "Missing payload from request" }
            return ServerResponse.badRequest().buildAndAwait()
        }

        val company = companyRepository.findByCode(companyWithAdminResource!!.companyResource.code)
        company?.let {
            log.error { "Company with code=${it.code} already exists" }
            return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        }

        val saved = companyRepository.save(CompanyConverter.toCompany(companyWithAdminResource.companyResource))
        log.info { "Successfully saved company=$saved" }

        var user: UserProfile? = null
        companyWithAdminResource.userResource?.let {
            user = userRepository.findByUsername(it.username) ?: UserConverter.toUserBase(it)
            user = addRoleToUser(user !!, CompanyRole(Roles.ROLE_ADMIN, saved.code))
        }

        return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(
            CompanyWithAdminResource(
                CompanyConverter.toCompanyResource(saved),
                if (user == null) null else UserConverter.toUserResource(user !!)
            )
        )
    }

    private fun validateCompanyWithAdminResource(companyWithAdminResource: CompanyWithAdminResource?): Errors? {
        companyWithAdminResource?.let {
            val errors = BeanPropertyBindingResult(companyWithAdminResource, CompanyWithAdminResource::class.java.name)
            companyWithAdminResourceValidator.validate(companyWithAdminResource, errors)
            return errors
        } ?: run {
            return null
        }
    }

    private suspend fun addRoleToUser(userProfile: UserProfile, companyRole: CompanyRole): UserProfile {
        userProfile.roles?.add(companyRole) ?: run {
            userProfile.roles = mutableListOf(companyRole)
        }
        return userRepository.save(userProfile)
    }
}