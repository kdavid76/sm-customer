package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.common.customer.resources.UserResource
import org.springframework.web.reactive.function.server.ServerResponse

interface CompanyService {
    suspend fun findAllCompanies(): ServerResponse
    suspend fun findCompany(companyCode: String): ServerResponse
    suspend fun registerCompany(companyResource: CompanyResource, userResource: UserResource?): ServerResponse
}
