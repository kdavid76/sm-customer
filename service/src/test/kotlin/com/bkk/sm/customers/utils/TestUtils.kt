package com.bkk.sm.customers.utils

import com.bkk.sm.mongo.customers.model.Address
import com.bkk.sm.mongo.customers.model.company.Company
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.resources.CompanyResource
import com.bkk.sm.mongo.customers.resources.UserResource
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import java.time.LocalDateTime

class TestUtils {

    companion object {
        private val DEFAULT_CONTEXT: ServerResponse.Context = object : ServerResponse.Context {
            override fun messageWriters(): List<HttpMessageWriter<*>> {
                return HandlerStrategies.withDefaults().messageWriters()
            }

            override fun viewResolvers(): List<ViewResolver> {
                return listOf()
            }
        }

        suspend fun getResponseString(serverResponse: ServerResponse): String? {
            val request = MockServerHttpRequest.get("http://foo.foo").build()
            val exchange = MockServerWebExchange.from(request)
            serverResponse.writeTo(exchange, DEFAULT_CONTEXT).awaitSingleOrNull()
            val response = exchange.response
            return response.bodyAsString.awaitSingleOrNull()
        }

        fun createUserProfile(
            id: String,
            username: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>?
        ) = UserProfile(
            id = id, username = username, firstName = firstName, lastName = lastName,
            email = email, roles = roles
        )

        fun createUserResource(
            id: String,
            username: String,
            password: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>
        ) = UserResource(
            id = id, username = username, password = password, firstName = firstName,
            lastName = lastName, email = email, roles = roles
        )

        fun createAddress(
            postCode: String,
            city: String,
            firstLine: String,
            secondLine: String?,
            thirdLine: String?
        ) = Address(
            postCode = postCode, city = city, firstLine = firstLine, secondLine = secondLine,
            thirdLine = thirdLine
        )

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
        ) = CompanyResource(
            id = id, code = code, name = name, email = email, taxId = taxId,
            bankAccountNumber = bankAccountNumber, activationToken = activationToken,
            activationTime = activationTime, registrationTime = registrationTime,
            lastModificationTime = lastModificationTime, enabled = enabled,
            version = version, address = address
        )

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
        ) = Company(
            id = id, code = code, name = name, email = email, taxId = taxId,
            bankAccountNumber = bankAccountNumber, activationToken = activationToken,
            activationTime = activationTime, registrationTime = registrationTime,
            lastModificationTime = lastModificationTime, enabled = enabled,
            version = version, address = address
        )
    }
}