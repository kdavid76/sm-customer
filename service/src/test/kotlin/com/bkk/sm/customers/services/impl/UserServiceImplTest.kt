package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.common.model.Roles
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.validation.Errors

@ActiveProfiles("test")
class UserServiceImplTest {

    private val userRepository = mockk<UserRepository>()
    private val userResourceValidator = mockk<UserResourceValidator>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    private val impl = UserServiceImpl(userRepository, userResourceValidator, passwordEncoder)

    private val davidk = TestUtils.createUserProfile(
        "123456789",
        "davidk",
        "Krisztian",
        "David",
        "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk"))
    )
    private val bkkadmin = TestUtils.createUserProfile(
        "987654",
        "bkkadmin",
        "Mike",
        "Hammer",
        "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_SUPERADMIN, "system"))
    )

    @Test
    fun `Find all the users`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findAll() } returns flow {
            emit(davidk)
            emit(bkkadmin)
        }

        // when
        val response = impl.findAllUsers()

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

        // when
        val response = impl.findUserByUsername(davidk.username)
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
    fun `Missing user by username`(): Unit = runBlocking {
        // given
        coEvery { userRepository.findByUsername("davidk") } answers {
            null
        }

        // when
        val response = impl.findUserByUsername(davidk.username)

        // then
        response.statusCode() shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `Trying to add invalid user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk).copy(firstName = "")
        val errors = slot<Errors>()
        coEvery { userResourceValidator.validate(any(), capture(errors)) } answers {
            errors.captured
            errors.captured.rejectValue("firstName", "error.firstname.missing")
        }

        // when
        val response = impl.registerUser(user)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `Username already taken when adding new user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk)
        user.password = "somePassword"

        coEvery { userRepository.findByUsername(user.username) } answers { bkkadmin }
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit

        // when
        val response = impl.registerUser(user)

        // then
        response.statusCode() shouldBe HttpStatus.CONFLICT
    }

    @Test
    fun `Add new user`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk)
        user.password = "somePassword"

        coEvery { userRepository.findByUsername(user.username) } answers { null }
        coEvery { userRepository.save(any()) } answers { bkkadmin }
        coEvery { passwordEncoder.encode(any()) } answers { "password" }
        coEvery { userResourceValidator.validate(any(), any()) } returns Unit

        // when
        val response = impl.registerUser(user)

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

    @Test
    fun `Add new user with no password`() = runBlocking {
        // given
        val user = UserConverter.toUserResource(davidk)

        // when
        val response = impl.registerUser(user)

        // then
        response.statusCode() shouldBe HttpStatus.BAD_REQUEST
    }
}
