package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
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

@ActiveProfiles("test")
class UserHandlerTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val userResourceValidator = UserResourceValidator()

    private val userHandler: UserHandler = UserHandler(userRepository, passwordEncoder, userResourceValidator)

    private val davidk = TestUtils.createUserProfile(
        "123456789", "davidk",
        "Krisztian", "David", "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk"))
    )
    private val bkkadmin = TestUtils.createUserProfile(
        "987654", "bkkadmin",
        "Mike", "Hammer", "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_SUPERADMIN, "system"))
    )

    @BeforeEach
    fun initMocks() {
        clearMocks(userRepository, passwordEncoder)
    }

    @Test
    fun `Find all the users`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findAll() } returns flow {
            emit(davidk)
            emit(bkkadmin)
        }

        // when
        val request = MockServerRequest.builder()
        val response = userHandler.findAll(request.build())

        response.statusCode() shouldBe HttpStatus.OK

        val responseString = TestUtils.getResponseString(response)

        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        val typeFactory = mapper.typeFactory
        val collectionType = typeFactory.constructCollectionType(ArrayList::class.java, UserResource::class.java)
        val resultList = mapper.readValue<List<UserResource>>(responseString, collectionType)
        resultList shouldNotBe null
        resultList shouldContain UserConverter.toUserResource(davidk)
        resultList shouldContain UserConverter.toUserResource(bkkadmin)
    }

    @Test
    fun `Find user by username`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findByUsername("davidk") } answers {
            davidk
        }

        val request = MockServerRequest.builder().pathVariable("username", "davidk")

        // when
        val response = userHandler.findByUsername(request.build())

        response.statusCode() shouldBe HttpStatus.OK

        val responseString = TestUtils.getResponseString(response)

        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        val typeFactory = mapper.typeFactory
        val simpleType = typeFactory.constructSimpleType(UserResource::class.java, null)
        val result = mapper.readValue<UserResource>(responseString, simpleType)
        result shouldNotBe null
        result.username shouldBe "davidk"
        result.email shouldBe "my@email.com"
    }

    @Test
    fun `Do not find user by username`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findByUsername("davidk") } answers {
            null
        }

        val request = MockServerRequest.builder().pathVariable("username", "davidk")

        // when
        val response = userHandler.findByUsername(request.build())

        // then
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }


    @Test
    fun `Empty payload while adding new user`() = runBlocking {
        // given
        val request = MockServerRequest.builder().body(Mono.empty<UserResource>())

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Invalid payload while adding new user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk).copy(firstName = "")
        val request = MockServerRequest.builder().body(Mono.just(user))

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Username already taken when adding new user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk)
        val request = MockServerRequest.builder().body(Mono.just(user))

        coEvery { userRepository.findByUsername(user.username) } answers { bkkadmin }

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT

    }

    @Test
    fun `Add new user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk)
        val request = MockServerRequest.builder().body(Mono.just(user))

        coEvery { userRepository.findByUsername(user.username) } answers { null }
        coEvery { userRepository.save(any()) } answers { bkkadmin }
        coEvery { passwordEncoder.encode(any()) } answers { "password" }

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED

        val responseString = TestUtils.getResponseString(response)
        responseString shouldNotBe null

        val mapper = jacksonObjectMapper()
        val typeFactory = mapper.typeFactory
        val type = typeFactory.constructSimpleType(UserResource::class.java, null)
        val result = mapper.readValue<UserResource>(responseString, type)

        result shouldNotBe null
        result.username shouldBe bkkadmin.username
        result.email shouldBe bkkadmin.email
    }
}
