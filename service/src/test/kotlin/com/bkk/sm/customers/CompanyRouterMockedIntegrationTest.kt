package com.bkk.sm.customers

import com.bkk.sm.customers.config.RouterConfig
import com.bkk.sm.customers.config.SecurityConfig
import com.bkk.sm.customers.config.TestConfig
import com.bkk.sm.customers.services.CompanyHandler
import com.bkk.sm.customers.services.UserHandler
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.Roles
import com.bkk.sm.mongo.customers.model.company.Company
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.model.user.UserBase
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.bkk.sm.mongo.customers.resources.CompanyWithAdminResource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.slot
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
import java.time.LocalDateTime

@WebFluxTest
@Import(TestConfig::class, RouterConfig::class,
    SecurityConfig::class, UserHandler::class, CompanyHandler::class)
@ActiveProfiles("test")
class CompanyRouterMockedIntegrationTest(
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
    private val bkk = TestUtils.createCompanyResource(null, "bkk", "Beszterce KK",
        "bkk@bkk.hu", null, null, "",
        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, 1,
        TestUtils.createAddress("3100", "salgótarján", "Utca. 1", null, null))


    @BeforeEach
    fun initialize() {
        client = client.mutate()
            .responseTimeout(Duration.ofMinutes(10))
            .build()
    }

    @Test
    fun `The whole payload is missing while adding new company`() {
        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `Try to add company with invalid parameters`() {
        val company = bkk

        company.code = " "
        company.name = " "
        company.email = " "

        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyWithAdminResource(company, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `Try to add company with already existing code`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            CompanyConverter.toCompany(bkk)
        }
        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyWithAdminResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Add company with valid user adn companyData`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            null
        }

        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            davidk
        }

        val user = slot<UserBase>()
        coEvery {
            userRepository.save(capture(user))
        } answers {
            user.captured
        }

        val company = slot<Company>()
        coEvery {
            companyRepository.save(capture(company))
        } answers {
            company.captured
        }

        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyWithAdminResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyWithAdminResource>()
            .consumeWith {
                val companyWithAdminResource = it.responseBody
                Assertions.assertThat(companyWithAdminResource).isNotNull
                if (companyWithAdminResource != null) {
                    Assertions.assertThat(companyWithAdminResource.companyResource).isNotNull
                    Assertions.assertThat(companyWithAdminResource.companyResource.code).isEqualTo(bkk.code)
                    Assertions.assertThat(companyWithAdminResource.companyResource.name).isEqualTo(bkk.name)
                    Assertions.assertThat(companyWithAdminResource.companyResource.email).isEqualTo(bkk.email)
                }
                if (companyWithAdminResource != null) {
                    Assertions.assertThat(companyWithAdminResource.userResource).isNotNull
                    Assertions.assertThat(companyWithAdminResource.userResource.username).isEqualTo(davidk.username)
                    Assertions.assertThat(companyWithAdminResource.userResource.email).isEqualTo(davidk.email)
                    Assertions.assertThat(companyWithAdminResource.userResource.firstName).isEqualTo(davidk.firstName)
                }
            }
    }

    @Test
    fun `Check if role is added to existing user`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            null
        }

        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            bkkadmin
        }

        val user = slot<UserBase>()
        coEvery {
            userRepository.save(capture(user))
        } answers {
            user.captured
        }

        val company = slot<Company>()
        coEvery {
            companyRepository.save(capture(company))
        } answers {
            company.captured
        }

        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyWithAdminResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyWithAdminResource>()
            .consumeWith {
                val companyWithAdminResource = it.responseBody
                Assertions.assertThat(companyWithAdminResource).isNotNull
                if (companyWithAdminResource != null) {
                    Assertions.assertThat(companyWithAdminResource.userResource).isNotNull
                    Assertions.assertThat(companyWithAdminResource.userResource.roles).isNotNull
                    companyWithAdminResource.userResource.roles?.let { it1 -> Assertions.assertThat(it1.size).isEqualTo(2) }
                    Assertions.assertThat(companyWithAdminResource.userResource.roles).containsExactly(
                        CompanyRole(Roles.ROLE_SUPERADMIN, "system"),
                        CompanyRole(Roles.ROLE_ADMIN, "bkk")
                    )
                }
            }
    }
}