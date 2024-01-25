package com.bkk.sm.customers.handlers.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.user.UserProfile
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

@ActiveProfiles("test")
class MongoUserHandlerImplTest {

    private val userRepository = mockk<UserRepository>()
    private val userResourceValidator = mockk<UserResourceValidator>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private val mongoUserHandlerImpl = MongoUserHandlerImpl(userRepository, userResourceValidator, passwordEncoder)

    @BeforeEach
    fun initMocks() {
        clearMocks(userRepository, userResourceValidator, passwordEncoder)
    }

    @Test
    fun `Find all users`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findAll() } returns flow { emit(UserProfile(username = "some-user")) }
        val request = MockServerRequest.builder()

        // when
        val response = mongoUserHandlerImpl.findAll(request.build())

        // then
        coVerify { userRepository.findAll() }
        response.statusCode() shouldBe HttpStatus.OK
    }

    @Test
    fun `Find a user by username`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findByUsername("davidk") } returns UserProfile(username = "davidk")
        val request = MockServerRequest.builder().pathVariable("username", "davidk")

        // when
        val response = mongoUserHandlerImpl.findByUsername(request.build())

        // then
        coVerify { userRepository.findByUsername("davidk") }
        response.statusCode() shouldBe HttpStatus.OK
    }

    @Test
    fun `Not find a user by username`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findByUsername("davidk") } returns null
        val request = MockServerRequest.builder().pathVariable("username", "davidk")

        // when
        val response = mongoUserHandlerImpl.findByUsername(request.build())

        // then
        coVerify { userRepository.findByUsername("davidk") }
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `Missing payload while registering user`(): Unit = runBlocking {
        // given
        val request = MockServerRequest.builder().body(Mono.empty<UserResource>())

        // when
        val response = mongoUserHandlerImpl.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `User already exists`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")),
        )
        davidk.password = "PassWord_1"
        val request = MockServerRequest.builder().body(Mono.just(UserConverter.toUserResource(davidk)))
        coEvery { userRepository.findByUsername(davidk.username) } returns davidk
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit

        // when
        val response = mongoUserHandlerImpl.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT
        coVerify { userRepository.findByUsername(davidk.username) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `Registering user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789",
            "davidk",
            "Krisztian",
            "David",
            "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")),
        )
        davidk.password = "PassWord_1"

        val request = MockServerRequest.builder().body(Mono.just(UserConverter.toUserResource(davidk)))
        coEvery { userRepository.findByUsername(davidk.username) } returns null
        coEvery { userRepository.save(any()) } returns davidk
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit
        coEvery { passwordEncoder.encode(any()) } returns "PassWord_1"

        // when
        val response = mongoUserHandlerImpl.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        coVerify { userRepository.findByUsername(davidk.username) }
        coVerify { userRepository.save(any()) }
    }
}
