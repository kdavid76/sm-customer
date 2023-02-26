package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.model.AreaType
import com.bkk.sm.common.model.Roles
import com.bkk.sm.common.utils.CommonResourceTestUtils
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.validation.Errors
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
class CompanyServiceImplTest {

    private val userRepository = mockk<UserRepository>()
    private val companyRepository = mockk<CompanyRepository>()
    private val userResourceValidator = mockk<UserResourceValidator>()
    private val companyResourceValidator = mockk<CompanyResourceValidator>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private val impl = CompanyServiceImpl(userResourceValidator, companyResourceValidator, companyRepository, userRepository, passwordEncoder)

    private val bkk = TestUtils.createCompany(
        UUID.randomUUID().toString(), "bkk", "Beszterce KK",
        "besztercekk@email.com", "12345678-1-11", "11111111-22222222-33333333",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
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
    private val skse = TestUtils.createCompany(
        UUID.randomUUID().toString(), "skse", "Salgotarjani KSE",
        "skse@email.com", "87654321-2-22", "44444444-55555555-66666666",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
        1,
        CommonResourceTestUtils.createAddress(
            "Salgotarjan",
            3100,
            "Martirok",
            AreaType.UT,
            "18",
            1,
            1,
            null,
        ),
    )

    @BeforeEach
    fun initMocks() {
        clearMocks(userRepository, companyRepository, userResourceValidator, companyResourceValidator)
    }

    @Test
    fun `Find all companies`(): Unit = runBlocking {
        // given
        coEvery { companyRepository.findAll() } returns flow {
            emit(bkk)
            emit(skse)
        }

        // when
        val response = impl.findAllCompanies()

        // then
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
        coEvery { companyRepository.findByCode("bkk") } answers {
            bkk
        }

        // when
        val response = impl.findCompany("bkk")

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
        coEvery { companyRepository.findByCode("bkk") } answers {
            null
        }

        // when
        val response = impl.findCompany("bkk")

        // then
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `Invalid payload while adding new company user`() = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        val companyResource = CompanyConverter.toCompanyResource(bkk).copy(name = "")
        val userResource = UserConverter.toUserResource(davidk).copy(firstName = "")
        val companyErrors = slot<Errors>()
        coEvery { companyResourceValidator.validate(companyResource, capture(companyErrors)) } answers {
            companyErrors.captured
            companyErrors.captured.rejectValue("name", "error.test.name")
        }
        val userErrors = slot<Errors>()
        coEvery { userResourceValidator.validate(userResource, capture(userErrors)) } answers {
            userErrors.captured
            userErrors.captured.rejectValue("firstName", "error.test.firstName")
        }

        // when
        val response = impl.registerCompany(companyResource, userResource)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Add already existing company`() = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        davidk.password = "somePassword"
        val companyResource = CompanyConverter.toCompanyResource(bkk)
        val userResource = UserConverter.toUserResource(davidk)
        coEvery { companyRepository.findByCode(any()) } answers {
            bkk
        }
        val companyErrors = slot<Errors>()
        coEvery { companyResourceValidator.validate(companyResource, capture(companyErrors)) } answers {
            companyErrors.captured
        }
        val userErrors = slot<Errors>()
        coEvery { userResourceValidator.validate(userResource, capture(userErrors)) } answers {
            userErrors.captured
        }

        // when
        val response = impl.registerCompany(companyResource, userResource)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT
    }

    @Test
    fun `Add company with user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "icecode")),
        )
        val companyResource = CompanyConverter.toCompanyResource(bkk)
        val userResource = UserConverter.toUserResource(davidk)
        coEvery { companyRepository.findByCode("bkk") } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }
        coEvery { userRepository.findByUsername("davidk") } answers {
            davidk
        }
        val up = slot<UserProfile>()
        coEvery { userRepository.save(capture(up)) } answers {
            up.captured
        }
        val companyErrors = slot<Errors>()
        coEvery { companyResourceValidator.validate(companyResource, capture(companyErrors)) } answers {
            companyErrors.captured
        }
        val userErrors = slot<Errors>()
        coEvery { userResourceValidator.validate(userResource, capture(userErrors)) } answers {
            userErrors.captured
        }

        // when
        val response = impl.registerCompany(companyResource, userResource)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyAndUserResourceType = typeFactory.constructType(CompanyAndUserResource::class.java)
        val result = mapper.readValue<CompanyAndUserResource>(responseString, companyAndUserResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldNotBe null
        result.userResource?.roles shouldNotBe null
        (result.userResource?.roles?.size ?: 0) shouldBe 2
        result.userResource?.roles!! shouldContain CompanyRole(Roles.ROLE_ADMIN, "bkk")
    }

    @Test
    fun `Add company with user who does not exist`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            null,
        )
        davidk.password = "password"
        val companyResource = CompanyConverter.toCompanyResource(bkk)
        val userResource = UserConverter.toUserResource(davidk)
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }
        coEvery { userRepository.findByUsername(any()) } answers {
            null
        }
        val companyErrors = slot<Errors>()
        coEvery { companyResourceValidator.validate(companyResource, capture(companyErrors)) } answers {
            companyErrors.captured
        }
        val userErrors = slot<Errors>()
        coEvery { userResourceValidator.validate(userResource, capture(userErrors)) } answers {
            userErrors.captured
        }
        val up = slot<UserProfile>()
        coEvery { userRepository.save(capture(up)) } answers {
            up.captured
        }
        coEvery { passwordEncoder.encode(any()) } returns "samplePassword"

        // when
        val response = impl.registerCompany(companyResource, userResource)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyAndUserResourceType = typeFactory.constructType(CompanyAndUserResource::class.java)
        val result = mapper.readValue<CompanyAndUserResource>(responseString, companyAndUserResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldNotBe null
        result.userResource?.roles shouldNotBe null
        (result.userResource?.roles?.size ?: 0) shouldBe 1
        result.userResource?.roles!! shouldContain CompanyRole(Roles.ROLE_ADMIN, "bkk")
    }

    @Test
    fun `Add company without user`(): Unit = runBlocking {
        // given
        val companyResource = CompanyConverter.toCompanyResource(bkk)
        coEvery { companyRepository.findByCode(any()) } answers {
            null
        }
        coEvery { companyRepository.save(any()) } answers {
            bkk
        }
        val companyErrors = slot<Errors>()
        coEvery { companyResourceValidator.validate(companyResource, capture(companyErrors)) } answers {
            companyErrors.captured
        }

        // when
        val response = impl.registerCompany(companyResource, null)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val typeFactory = mapper.typeFactory
        val companyAndUserResourceType = typeFactory.constructType(CompanyAndUserResource::class.java)
        val result = mapper.readValue<CompanyAndUserResource>(responseString, companyAndUserResourceType)

        result shouldNotBe null
        result.companyResource shouldBe CompanyConverter.toCompanyResource(bkk)
        result.userResource shouldBe null
    }
}
