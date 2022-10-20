package com.bkk.sm.customers

import com.bkk.sm.customers.config.RouterConfig
import com.bkk.sm.customers.config.SecurityConfig
import com.bkk.sm.customers.config.TestConfig
import com.bkk.sm.customers.services.CompanyHandler
import com.bkk.sm.customers.services.UserHandler
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.Roles
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.model.user.UserBase
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.bkk.sm.mongo.customers.resources.UserResource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

@WebFluxTest
@Import(TestConfig::class, RouterConfig::class,
    SecurityConfig::class, UserHandler::class, CompanyHandler::class)
@ActiveProfiles("test")
class UserRouterMockedIntegrationTest(
    @Autowired var client: WebTestClient
) {
    @MockkBean
    lateinit var userRepository: UserRepository

    @MockkBean
    lateinit var companyRepository: CompanyRepository

    val davidk = TestUtils.createUser("123456789", "davidk",
        "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")))
    val bkkadmin = TestUtils.createUser("987654", "bkkadmin",
        "Mike", "Hammer", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_SUPERADMIN, "system")))

    @BeforeEach
    fun initialize() {
        client = client.mutate()
            .responseTimeout(Duration.ofMinutes(10))
            .build()
    }

    @Test
    fun `Retrieve all users`() {
        coEvery {
            userRepository.findAll()
        } returns flow {
            emit(davidk)
            emit(bkkadmin)
        }

        client
            .get()
            .uri("/users")
            .header("API_VERSION", "V1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(UserResource::class.java)
            .hasSize(2)
            .contains(UserConverter.toUserResource(davidk), UserConverter.toUserResource(bkkadmin))
    }

    @Test
    fun `Retrieve users with username`() {
        coEvery {
            userRepository.findByUsername("davidk")
        } coAnswers {
            davidk
        }

        client
            .get()
            .uri("/users/davidk")
            .header("API_VERSION", "V1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(UserResource::class.java)
            .hasSize(1)
            .contains(UserConverter.toUserResource(davidk))
    }

    @Test
    fun `Cannot find user with username`() {
        coEvery {
            userRepository.findByUsername("soosg")
        } coAnswers {
            null
        }

        client
            .get()
            .uri("/users/soosg")
            .header("API_VERSION", "V1")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Add a new user with empty request body`() {
        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            null
        }

        val user = slot<UserBase>()
        coEvery {
            userRepository.save(capture(user))
        } answers {
            user.captured
        }

        client
            .post()
            .uri("/users")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `Add a new user with valid request body`() {
        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            null
        }

        val user = slot<UserBase>()
        coEvery {
            userRepository.save(capture(user))
        } answers {
            user.captured
        }

        val dkResource = TestUtils.createUserResource("123456789", "davidk", "Jamcsa1?",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")))

        client
            .post()
            .uri("/users")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dkResource)
            .exchange()
            .expectStatus().isCreated
            .expectBody<UserResource>()
            .consumeWith {
                val userResource = it.responseBody
                Assertions.assertThat(userResource).isNotNull
                Assertions.assertThat(userResource!!.email).isEqualTo(dkResource.email)
                Assertions.assertThat(userResource.firstName).isEqualTo(dkResource.firstName)
                Assertions.assertThat(userResource.registrationTime).isNotNull
            }
    }

    @Test
    fun `Add a new user with already existing username`() {
        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            TestUtils.createUser("11111111", "bkkadmin", "Beszterce", "KK", "bkk@bkk.com", mutableListOf(CompanyRole(Roles.ROLE_USER, "bkk")))
        }
        val dkResource = TestUtils.createUserResource("123456789", "davidk", "Jamcsa1?",
            "Krisztian", "David", "my@email.com",
            mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")))

        client
            .post()
            .uri("/users")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dkResource)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
}