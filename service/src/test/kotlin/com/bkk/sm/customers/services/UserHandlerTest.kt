package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.services.handlers.UserHandler
import com.bkk.sm.customers.utils.TestUtils
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

@ActiveProfiles("test")
class UserHandlerTest {

    private val userService = mockk<UserService>()
    private val userHandler = UserHandler(userService)

    @BeforeEach
    fun initMocks() {
        clearMocks(userService)
    }

    @Test
    fun `Find all users`(): Unit = runBlocking {
        // given
        coEvery { userService.findAllUsers() } returns ServerResponse.ok().buildAndAwait()

        // when
        val request = MockServerRequest.builder()
        userHandler.findAll(request.build())

        // then
        coVerify { userService.findAllUsers() }
    }

    @Test
    fun `Find a user by username`(): Unit = runBlocking {
        // given
        coEvery { userService.findUserByUsername(any()) } returns ServerResponse.ok().buildAndAwait()

        val request = MockServerRequest.builder().pathVariable("username", "davidk")

        // when
        userHandler.findByUsername(request.build())

        // then
        coVerify { userService.findUserByUsername("davidk") }
    }

    @Test
    fun `Missing payload while registering user`(): Unit = runBlocking {
        // given
        // given
        val request = MockServerRequest.builder().body(Mono.empty<UserResource>())

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
        coVerify(exactly = 0) { userService.registerUser( any()) }
    }

    @Test
    fun `Registering user`(): Unit = runBlocking {
        // given
        val davidk = TestUtils.createUserProfile(
            "123456789", "davidk",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk"))
        )
        val request = MockServerRequest.builder().body(Mono.just(UserConverter.toUserResource(davidk)))
        coEvery { userService.registerUser(any()) } returns ServerResponse.status(HttpStatus.CREATED).buildAndAwait()

        // when
        val response = userHandler.add(request)

        // then
        response.statusCode() shouldBe HttpStatus.CREATED
        coVerify { userService.registerUser( any()) }
    }

/*
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
 */
}
