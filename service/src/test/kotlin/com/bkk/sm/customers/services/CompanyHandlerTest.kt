package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.model.AreaType
import com.bkk.sm.common.model.Roles
import com.bkk.sm.common.utils.CommonResourceTestUtils
import com.bkk.sm.customers.services.handlers.CompanyHandler
import com.bkk.sm.customers.services.impl.CompanyServiceImpl
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
class CompanyHandlerTest {
    private val companyService = mockk<CompanyServiceImpl>()
    private val companyHandler = CompanyHandler(companyService)

    private val bkk = TestUtils.createCompany(
        UUID.randomUUID().toString(), "bkk", "Beszterce KK",
        "besztercekk@email.com", "12345678-1-11", "11111111-22222222-33333333",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
        1, CommonResourceTestUtils.createAddress("Salgotarjan",
            3100, "First Line", AreaType.KORUT, "11",
            1, 1, null
            )
    )

    @BeforeEach
    fun initMocks() {
        clearMocks(companyService)
    }

    @Test
    fun `Find all companies`(): Unit = runBlocking {
        // given
        coEvery { companyService.findAllCompanies() } returns ServerResponse.ok().buildAndAwait()

        // when
        val request = MockServerRequest.builder()
        companyHandler.findAll(request.build())

        // then
        coVerify { companyService.findAllCompanies() }
    }

    @Test
    fun `Find a company by company code`(): Unit = runBlocking {
        // given
        coEvery { companyService.findCompany(any()) } returns ServerResponse.ok().buildAndAwait()

        val request = MockServerRequest.builder().pathVariable("companycode", "bkk")

        // when
        companyHandler.findByCompanyCode(request.build())

        // then
        coVerify { companyService.findCompany("bkk") }
    }

    @Test
    fun `Empty payload while adding new company and user`() = runBlocking {
        // given
        val request = MockServerRequest.builder().body(Mono.empty<CompanyAndUserResource>())

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
        coVerify(exactly = 0) { companyService.registerCompany( any(), any()) }
    }

    @Test
    fun `Add company`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode"))
        )
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyAndUserResource(company, user)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))

        coEvery { companyService.registerCompany(any(), any()) } returns ServerResponse.status(HttpStatus.CREATED).buildAndAwait()

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        coVerify { companyService.registerCompany(any(), any()) }
    }
}