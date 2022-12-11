package com.bkk.sm.mongo.customers.converters

import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.mongo.customers.model.company.Company

class CompanyConverter {
    companion object {

        fun toCompanyResource(company: Company) = CompanyResource(
            id = company.id, code = company.code,
            name = company.name, email = company.email, address = company.address, taxId = company.taxId,
            bankAccountNumber = company.bankAccountNumber, optionalContactInfo = company.optionalContactInfo,
            activationToken = company.activationToken, activationTime = company.activationTime,
            registrationTime = company.registrationTime, lastModificationTime = company.lastModificationTime,
            enabled = company.enabled, version = company.version
        )

        fun toCompany(company: CompanyResource) = Company(
            id = company.id, code = company.code,
            name = company.name, email = company.email, address = company.address, taxId = company.taxId,
            bankAccountNumber = company.bankAccountNumber, optionalContactInfo = company.optionalContactInfo,
            activationToken = company.activationToken, activationTime = company.activationTime,
            registrationTime = company.registrationTime, lastModificationTime = company.lastModificationTime,
            enabled = company.enabled, version = company.version
        )
    }
}