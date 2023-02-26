package com.bkk.sm.customers

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.customer.resources.CompanyAndUserResource
import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.common.model.AreaType
import com.bkk.sm.common.model.Roles
import com.bkk.sm.common.utils.CommonResourceTestUtils
import com.bkk.sm.customers.config.RouterConfig
import com.bkk.sm.customers.config.SecurityConfig
import com.bkk.sm.customers.config.TestConfig
import com.bkk.sm.customers.services.handlers.CompanyHandler
import com.bkk.sm.customers.services.handlers.UserHandler
import com.bkk.sm.customers.utils.TestUtils
import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.company.Company
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.ninjasquad.springmockk.MockkBean
import io.kotest.mpp.log
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.LocalDateTime

@WebFluxTest
@Import(
    TestConfig::class,
    RouterConfig::class,
    SecurityConfig::class,
    UserHandler::class,
    CompanyHandler::class,
)
@ActiveProfiles("test")
class CompanyRouterMockedIntegrationTest(
    @Autowired var client: WebTestClient,
) {
    @MockkBean
    lateinit var userRepository: UserRepository

    @MockkBean
    lateinit var companyRepository: CompanyRepository

    val davidk = TestUtils.createUserProfile(
        "123456789",
        "davidk",
        "Krisztian",
        "David",
        "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk")),
    )
    val bkkadmin = TestUtils.createUserProfile(
        "987654",
        "bkkadmin",
        "Mike",
        "Hammer",
        "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_SUPERADMIN, "system")),
    )
    private val bkk = CommonResourceTestUtils.createCompanyResource(
        null, "bkk", "Beszterce KK",
        "bkk@bkk.hu", null, null, "",
        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, 1,
        CommonResourceTestUtils.createAddress(
            "Salgotarjan",
            3100,
            "Medves",
            AreaType.KORUT,
            "86",
            7,
            40,
            null,
        ),
    )
    private val apple = TestUtils.createCompany(
        null, "apple", "Apple",
        "info@apple.com", null, null, "",
        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true, 1,
        CommonResourceTestUtils.createAddress(
            "Salgotarjan",
            3100,
            "Medves",
            AreaType.KORUT,
            "86",
            7,
            40,
            null,
        ),
    )

    @BeforeEach
    fun initialize() {
        client = client.mutate()
            .responseTimeout(Duration.ofMinutes(10))
            .build()
    }

    @Test
    fun `Retrieve all companies`() {
        coEvery {
            companyRepository.findAll()
        } returns flow {
            emit(apple)
            emit(CompanyConverter.toCompany(bkk))
        }

        client
            .get()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyResource::class.java)
            .hasSize(2)
            .contains(CompanyConverter.toCompanyResource(apple), bkk)
    }

    @Test
    fun `Retrieve company by code`() {
        coEvery {
            companyRepository.findByCode("apple")
        } coAnswers {
            apple
        }

        client
            .get()
            .uri("/companies/apple")
            .header("API_VERSION", "V1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(CompanyResource::class.java)
            .hasSize(1)
            .contains(CompanyConverter.toCompanyResource(apple))
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
        val company = bkk.copy()

        company.code = " "
        company.name = " "
        company.email = " "

        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyAndUserResource(company, UserConverter.toUserResource(davidk)))
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
            .bodyValue(CompanyAndUserResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Add company with valid user and companyData`() {
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

        val user = slot<UserProfile>()
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
            .bodyValue(CompanyAndUserResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isCreated
            .expectBody<CompanyAndUserResource>()
            .consumeWith {
                val CompanyAndUserResource = it.responseBody
                Assertions.assertThat(CompanyAndUserResource).isNotNull
                if (CompanyAndUserResource != null) {
                    Assertions.assertThat(CompanyAndUserResource.companyResource).isNotNull
                    Assertions.assertThat(CompanyAndUserResource.companyResource.code).isEqualTo(bkk.code)
                    Assertions.assertThat(CompanyAndUserResource.companyResource.name).isEqualTo(bkk.name)
                    Assertions.assertThat(CompanyAndUserResource.companyResource.email).isEqualTo(bkk.email)
                }
                if (CompanyAndUserResource?.userResource != null) {
                    Assertions.assertThat(CompanyAndUserResource.userResource).isNotNull
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.username).isEqualTo(davidk.username)
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.email).isEqualTo(davidk.email)
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.firstName)
                        .isEqualTo(davidk.firstName)
                }
            }
    }

    @Test
    fun `Add company with not pre-existing user`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            null
        }

        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            null
        }

        val user = slot<UserProfile>()
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
            .bodyValue(CompanyAndUserResource(bkk, UserConverter.toUserResource(davidk).copy(password = "PassWord_1")))
            .exchange()
            .expectStatus().isCreated
            .expectBody<CompanyAndUserResource>()
            .consumeWith {
                val companyAndUserResource = it.responseBody
                Assertions.assertThat(companyAndUserResource).isNotNull
                if (companyAndUserResource != null) {
                    Assertions.assertThat(companyAndUserResource.companyResource).isNotNull
                    Assertions.assertThat(companyAndUserResource.companyResource.code).isEqualTo(bkk.code)
                    Assertions.assertThat(companyAndUserResource.companyResource.name).isEqualTo(bkk.name)
                    Assertions.assertThat(companyAndUserResource.companyResource.email).isEqualTo(bkk.email)
                    Assertions.assertThat(companyAndUserResource.userResource).isNotNull
                    Assertions.assertThat(companyAndUserResource.userResource!!.username)
                        .isEqualTo(davidk.username)
                    Assertions.assertThat(companyAndUserResource.userResource!!.email).isEqualTo(davidk.email)
                    Assertions.assertThat(companyAndUserResource.userResource!!.firstName)
                        .isEqualTo(davidk.firstName)
                }
            }
    }

    // @Test
    fun `Multiple company admin nominations for user`() {
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

        val user = slot<UserProfile>()
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
            .bodyValue(CompanyAndUserResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyAndUserResource>()
            .consumeWith {
                val CompanyAndUserResource = it.responseBody
                Assertions.assertThat(CompanyAndUserResource).isNotNull

                if (CompanyAndUserResource?.userResource != null) {
                    Assertions.assertThat(CompanyAndUserResource.userResource).isNotNull
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.roles).isNotNull
                    CompanyAndUserResource.userResource!!.roles?.let { it1 ->
                        Assertions.assertThat(it1.size).isEqualTo(2)
                    }
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.roles).containsExactly(
                        CompanyRole(Roles.ROLE_SUPERADMIN, "system"),
                        CompanyRole(Roles.ROLE_ADMIN, "bkk"),
                    )
                }
            }
    }

    // @Test
    fun `Nominate user for company admin`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            null
        }

        val noRoleUser = bkkadmin.copy()
        noRoleUser.roles = null
        val username = slot<String>()
        coEvery {
            userRepository.findByUsername(capture(username))
        } answers {
            noRoleUser
        }

        val user = slot<UserProfile>()
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
            .bodyValue(CompanyAndUserResource(bkk, UserConverter.toUserResource(davidk)))
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyAndUserResource>()
            .consumeWith {
                val CompanyAndUserResource = it.responseBody
                Assertions.assertThat(CompanyAndUserResource).isNotNull

                if (CompanyAndUserResource?.userResource != null) {
                    Assertions.assertThat(CompanyAndUserResource.userResource).isNotNull
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.roles).isNotNull
                    CompanyAndUserResource.userResource!!.roles?.let { it1 ->
                        Assertions.assertThat(it1.size).isEqualTo(1)
                    }
                    Assertions.assertThat(CompanyAndUserResource.userResource!!.roles).containsExactly(
                        CompanyRole(Roles.ROLE_ADMIN, "bkk"),
                    )
                }
            }
    }

    @Test
    fun `Add company without admin user`() {
        coEvery {
            companyRepository.findByCode(bkk.code)
        } answers {
            null
        }

        val company = slot<Company>()
        coEvery {
            companyRepository.save(capture(company))
        } answers {
            company.captured
        }
        log { bkk.toString() }
        client
            .post()
            .uri("/companies")
            .header("API_VERSION", "V1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CompanyAndUserResource(bkk, null))
            .exchange()
            .expectStatus().isCreated
            .expectBody<CompanyAndUserResource>()
            .consumeWith {
                val CompanyAndUserResource = it.responseBody
                Assertions.assertThat(CompanyAndUserResource).isNotNull
                if (CompanyAndUserResource != null) {
                    Assertions.assertThat(CompanyAndUserResource.companyResource).isNotNull
                    Assertions.assertThat(CompanyAndUserResource.userResource).isNull()
                    Assertions.assertThat(CompanyAndUserResource.companyResource.code).isEqualTo(bkk.code)
                    Assertions.assertThat(CompanyAndUserResource.companyResource.name).isEqualTo(bkk.name)
                    Assertions.assertThat(CompanyAndUserResource.companyResource.email).isEqualTo(bkk.email)
                    coVerify(exactly = 0) {
                        userRepository.findByUsername(any())
                    }
                }
            }
    }
}
