package com.bkk.sm.customers.utils

import com.bkk.sm.mongo.customers.model.Address
import com.bkk.sm.mongo.customers.model.company.Company
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.model.user.UserBase
import com.bkk.sm.mongo.customers.resources.CompanyResource
import com.bkk.sm.mongo.customers.resources.UserResource
import java.time.LocalDateTime

class TestUtils {

    companion object {
        fun createUser(
            id: String,
            username: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>
        ) = UserBase(id = id, username = username, firstName = firstName, lastName = lastName,
            email = email, roles = roles)

        fun createUserResource(
            id: String,
            username: String,
            password: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>
        ) = UserResource(id = id, username = username, password = password, firstName = firstName,
                lastName = lastName, email = email, roles = roles)

        fun createAddress(
            postCode: String,
            city: String,
            firstLine: String,
            secondLine: String?,
            thirdLine:String?
        ) = Address(postCode=postCode, city=city, firstLine=firstLine, secondLine=secondLine,
                thirdLine=thirdLine)

        fun createCompanyResource(
            id: String?,
            code: String,
            name: String,
            email: String,
            taxId: String?,
            bankAccountNumber: String?,
            activationToken: String?,
            activationTime: LocalDateTime?,
            registrationTime: LocalDateTime?,
            lastModificationTime: LocalDateTime?,
            enabled: Boolean?,
            version: Long,
            address: Address
        ) = CompanyResource(id=id, code=code, name=name, email=email, taxId=taxId,
                bankAccountNumber = bankAccountNumber, activationToken = activationToken,
                activationTime = activationTime, registrationTime = registrationTime,
                lastModificationTime = lastModificationTime, enabled = enabled,
                version = version, address = address)

        fun createCompany(
            id: String?,
            code: String,
            name: String,
            email: String,
            taxId: String?,
            bankAccountNumber: String?,
            activationToken: String?,
            activationTime: LocalDateTime?,
            registrationTime: LocalDateTime?,
            lastModificationTime: LocalDateTime?,
            enabled: Boolean?,
            version: Long,
            address: Address
        ) = Company(id=id, code=code, name=name, email=email, taxId=taxId,
            bankAccountNumber = bankAccountNumber, activationToken = activationToken,
            activationTime = activationTime, registrationTime = registrationTime,
            lastModificationTime = lastModificationTime, enabled = enabled,
            version = version, address = address)
    }
}