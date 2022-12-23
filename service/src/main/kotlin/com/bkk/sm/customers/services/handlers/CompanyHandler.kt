package com.bkk.sm.customers.services.handlers

import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.customers.services.CompanyService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.reactive.function.server.buildAndAwait

@Component
class CompanyHandler(
    private val companyService: CompanyService
) {
    val log = KotlinLogging.logger {}

    suspend fun findAll(request: ServerRequest): ServerResponse {
        log.info { "Finding all companies registered in the system" }
        return companyService.findAllCompanies()
    }

    suspend fun findByCompanyCode(request: ServerRequest): ServerResponse {
        val code = request.pathVariable("companycode")
        return companyService.findCompany(code)
    }

    suspend fun add(request: ServerRequest): ServerResponse {
        val companyAndUserResource = request.awaitBodyOrNull<CompanyAndUserResource>()

        companyAndUserResource?.let {
            log.info { "Registering company=${companyAndUserResource.companyResource} and user=${companyAndUserResource.userResource ?: "N/A"}" }
            return companyService.registerCompany(companyAndUserResource.companyResource, companyAndUserResource.userResource)
        }

        log.error { "Registering company failed due to missing payload." }
        return ServerResponse.badRequest().buildAndAwait()
    }

}