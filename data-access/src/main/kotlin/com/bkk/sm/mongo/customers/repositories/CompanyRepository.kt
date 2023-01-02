package com.bkk.sm.mongo.customers.repositories

import com.bkk.sm.mongo.customers.model.company.Company
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : CoroutineCrudRepository<Company, String> {
    suspend fun findByCode(code: String): Company?
}
