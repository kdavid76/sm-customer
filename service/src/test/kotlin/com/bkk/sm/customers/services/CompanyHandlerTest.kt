package com.bkk.sm.customers.services

import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.Roles
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.bkk.sm.mongo.customers.resources.CompanyResource
import com.bkk.sm.mongo.customers.resources.CompanyWithAdminResource
import com.bkk.sm.mongo.customers.validators.CompanyResourceValidator
import com.bkk.sm.mongo.customers.validators.CompanyWithAdminResourceValidator
import com.bkk.sm.mongo.customers.validators.UserResourceValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
class CompanyHandlerTest {
    private val userRepository = mockk<UserRepository>()
    private val companyRepository = mockk<CompanyRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val validator = CompanyWithAdminResourceValidator(CompanyResourceValidator(), UserResourceValidator())

    private val companyHandler = CompanyHandler(userRepository, companyRepository, validator)

    private val bkk = TestUtils.createCompany(
        UUID.randomUUID().toString(), "bkk", "Beszterce KK",
        "besztercekk@email.com", "12345678-1-11", "11111111-22222222-33333333",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
        1, TestUtils.createAddress("3100", "Salgotarjan", "First Line 11", "", ""),
    )
    private val skse = TestUtils.createCompany(
        UUID.randomUUID().toString(), "skse", "Salgotarjani KSE",
        "skse@email.com", "87654321-2-22", "44444444-55555555-66666666",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
        1, TestUtils.createAddress("3100", "Salgotarjan", "Martirok u. 18.", "", ""),
    )

    @BeforeEach
    fun initMocks() {
        clearMocks(userRepository, companyRepository, passwordEncoder)
    }

    @Test
    fun `Find all companies`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findAll() } returns flow {
            emit(bkk)
            emit(skse)
        }

        // when
        val request = MockServerRequest.builder()
        val response = companyHandler.findAll(request.build())

        response.statusCode() shouldBe HttpStatus.OK

        val responseString = TestUtils.getResponseString(response)

        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val collectionType = typeFactory.constructCollectionType(ArrayList::class.java, CompanyResource::class.java)
        val resultList = mapper.readValue<List<CompanyResource>>(responseString, collectionType)

        resultList shouldNotBe null
        resultList shouldContain CompanyConverter.toCompanyResource(bkk)
        resultList shouldContain CompanyConverter.toCompanyResource(skse)
    }

    @Test
    fun `Find a company by company code`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findByCode(any()) } answers {
            bkk
        }
        val request = MockServerRequest.builder().pathVariable("companycode", "bkk")

        // when
        val response = companyHandler.findByCompanyCode(request.build())

        // then
        response.statusCode() shouldBe HttpStatus.OK

        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyResourceType = typeFactory.constructType(CompanyResource::class.java)
        val result = mapper.readValue<CompanyResource>(responseString, companyResourceType)
        result shouldNotBe null
        result shouldBe CompanyConverter.toCompanyResource(bkk)
    }

    @Test
    fun `Not find a company by company code`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        val request = MockServerRequest.builder().pathVariable("companycode", "bkk")

        // when
        val response = companyHandler.findByCompanyCode(request.build())

        // then
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `Empty payload while adding new company and user`() = runBlocking {
        // given
        val request = MockServerRequest.builder().body(Mono.empty<CompanyWithAdminResourceValidator>())

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Invalid payload while adding new company user`() = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode"))
        )
        val company = CompanyConverter.toCompanyResource(bkk).copy(name = "")
        val user = UserConverter.toUserResource(davidk).copy(firstName = "")
        val companyWithAdmin = CompanyWithAdminResource(company, user)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Add already existing company`() = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode"))
        )
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyWithAdminResource(company, user)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))
        coEvery { companyRepository.findByCode(any()) } answers {
            bkk
        }

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT
    }

    @Test
    fun `Add company with user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode"))
        )
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyWithAdminResource(company, user)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }
        coEvery { userRepository.findByUsername(any()) } answers {
            davidk
        }
        val up = slot<UserProfile>()
        coEvery { userRepository.save(capture(up)) } answers {
            up.captured
        }

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyWithAdminResourceType = typeFactory.constructType(CompanyWithAdminResource::class.java)
        val result = mapper.readValue<CompanyWithAdminResource>(responseString, companyWithAdminResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldNotBe null
        result.userResource?.roles shouldNotBe null
        (result.userResource?.roles?.size ?: 0) shouldBe 2
        result.userResource?.roles !! shouldContain CompanyRole(Roles.ROLE_ADMIN, "bkk")
    }

    @Test
    fun `Add company with user who does not exist`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com", null
        )
        val company = CompanyConverter.toCompanyResource(bkk)
        val user = UserConverter.toUserResource(davidk)
        val companyWithAdmin = CompanyWithAdminResource(company, user)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }
        coEvery { userRepository.findByUsername(any()) } answers {
            null
        }
        val up = slot<UserProfile>()
        coEvery { userRepository.save(capture(up)) } answers {
            up.captured
        }

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyWithAdminResourceType = typeFactory.constructType(CompanyWithAdminResource::class.java)
        val result = mapper.readValue<CompanyWithAdminResource>(responseString, companyWithAdminResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldNotBe null
        result.userResource?.roles shouldNotBe null
        (result.userResource?.roles?.size ?: 0) shouldBe 1
        result.userResource?.roles !! shouldContain CompanyRole(Roles.ROLE_ADMIN, "bkk")
    }

    @Test
    fun `Add company without user`(): Unit = runBlocking {
        // given
        val company = CompanyConverter.toCompanyResource(bkk)
        val companyWithAdmin = CompanyWithAdminResource(company, null)
        val request = MockServerRequest.builder().body(Mono.just(companyWithAdmin))
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }

        // when
        val response = companyHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyWithAdminResourceType = typeFactory.constructType(CompanyWithAdminResource::class.java)
        val result = mapper.readValue<CompanyWithAdminResource>(responseString, companyWithAdminResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldBe null
    }
}