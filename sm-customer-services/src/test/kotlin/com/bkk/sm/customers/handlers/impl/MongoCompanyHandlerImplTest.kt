package com.bkk.sm.customers.handlers.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.model.AreaType
import com.bkk.sm.common.model.Roles
import com.bkk.sm.common.utils.CommonResourceTestUtils
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@ActiveProfiles("test")
class MongoCompanyHandlerImplTest {
    private val userRepository = mockk<UserRepository>()
    private val companyRepository = mockk<CompanyRepository>()
    private val userResourceValidator = mockk<UserResourceValidator>()
    private val companyResourceValidator = mockk<CompanyResourceValidator>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private val companyHandler = MongoCompanyHandlerImpl(
        userResourceValidator = userResourceValidator,
        userRepository = userRepository,
        companyResourceValidator = companyResourceValidator,
        companyRepository = companyRepository,
        passwordEncoder = passwordEncoder,
    )

    private val bkk = TestUtils.createCompany(
        UUID.randomUUID().toString(), "bkk", "Beszterce KK",
        "besztercekk@email.com", "12345678-1-11", "11111111-22222222-33333333",
        "", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()), true,
        1,
        CommonResourceTestUtils.createAddress(
            "Salgotarjan",
            3100,
            "First Line",
            AreaType.KORUT,
            "11",
            1,
            1,
            null,
        ),
    )

    @BeforeEach
    fun initMocks() {
        clearMocks(userRepository, companyRepository, userResourceValidator, companyResourceValidator, passwordEncoder)
    }

    @Test
    fun `Find all companies`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findAll() } returns flow { emit(bkk) }

        // when
        val request = MockServerRequest.builder()
        val response = companyHandler.findAll(request.build())

        // then
        coVerify { companyRepository.findAll() }
        response.statusCode() shouldBe HttpStatus.OK
    }

    @Test
    fun `Find a company by company code`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findByCode(any()) } returns bkk
        val request = MockServerRequest.builder().pathVariable("companycode", "bkk")

        // when
        val response = companyHandler.findByCompanyCode(request.build())

        // then
        coVerify { companyRepository.findByCode("bkk") }
        response.statusCode() shouldBe HttpStatus.OK
    }

    @Test
    fun `Not find a company by code`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findByCode("bkk") } returns null
        val request = MockServerRequest.builder().pathVariable("companycode", "bkk")

        // when
        val response = companyHandler.findByCompanyCode(request.build())

        // then
        coVerify { companyRepository.findByCode("bkk") }
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `Missing payload while registering new company and user`() = runBlocking {
        // given
        val request = MockServerRequest.builder().body(Mono.empty<CompanyAndUserResource>())

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
        coVerify(exactly = 0) { userRepository.save(any()) }
        coVerify(exactly = 0) { companyRepository.save(any()) }
    }

    @Test
    fun `Registering company with new user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        davidk.password = "Pass_Word1"
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyAndUserResource(companyResource = company, userResource = user, isNewUser = true)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))

        coEvery { companyRepository.findByCode(company.code) } returns null
        coEvery { userRepository.findByUsername(davidk.username) } returns null
        coEvery { passwordEncoder.encode(any()) } returns "Pass_Word1s"
        coEvery { companyRepository.save(any()) } returns bkk
        coEvery { userRepository.save(any()) } returns davidk
        coEvery { companyResourceValidator.validate(any(), any()) } returns Unit
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        coVerify { userRepository.save(any()) }
        coVerify { companyRepository.save(any()) }
        coVerify { userRepository.findByUsername(davidk.username) }
        coVerify { companyRepository.findByCode(bkk.code) }
        coVerify { companyResourceValidator.validate(any(), any()) }
        coVerify { userResourceValidator.validate(any(), any()) }
    }

    @Test
    fun `Registering company with existing user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        davidk.password = "Pass_Word1"
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyAndUserResource(company, user, false)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))

        coEvery { companyRepository.findByCode(company.code) } returns null
        coEvery { userRepository.findByUsername(davidk.username) } returns davidk
        coEvery { companyRepository.save(any()) } returns bkk
        coEvery { companyResourceValidator.validate(any(), any()) } returns Unit

        coEvery { passwordEncoder.encode(any()) } returns "Pass_Word1s"
        coEvery { userRepository.save(any()) } returns davidk
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED

        coVerify { companyRepository.save(any()) }
        coVerify { userRepository.findByUsername(davidk.username) }
        coVerify { companyRepository.findByCode(bkk.code) }
        coVerify { companyResourceValidator.validate(any(), any()) }
        coVerify { userRepository.save(any()) }
        coVerify(exactly = 0) { passwordEncoder.encode(any()) }
        coVerify(exactly = 0) { userResourceValidator.validate(any(), any()) }
    }

    @Test
    fun `Conflicting company codes`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        davidk.password = "Pass_Word1"
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyAndUserResource(company, user, false)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))

        coEvery { companyResourceValidator.validate(any(), any()) } returns Unit
        coEvery { companyRepository.findByCode(company.code) } returns bkk

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT

        coVerify { companyRepository.findByCode(bkk.code) }
        coVerify { companyResourceValidator.validate(any(), any()) }
        coVerify(exactly = 0) { companyRepository.save(any()) }
        coVerify(exactly = 0) { userRepository.findByUsername(davidk.username) }
        coVerify(exactly = 0) { userRepository.save(any()) }
        coVerify(exactly = 0) { passwordEncoder.encode(any()) }
        coVerify(exactly = 0) { userResourceValidator.validate(any(), any()) }
    }
}
