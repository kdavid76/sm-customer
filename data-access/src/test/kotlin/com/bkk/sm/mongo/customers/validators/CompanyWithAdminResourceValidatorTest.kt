package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.converters.CompanyConverter
import com.bkk.sm.mongo.customers.converters.UserConverter
import com.bkk.sm.mongo.customers.model.Roles
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.resources.CompanyWithAdminResource
import com.bkk.sm.mongo.customers.util.TestUtils
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.validation.BeanPropertyBindingResult
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
class CompanyWithAdminResourceValidatorTest {
    private val companyResourceValidatorMock = mockk<CompanyResourceValidator>()
    private val userResourceValidatorMock = mockk<UserResourceValidator>()
    private val validator = CompanyWithAdminResourceValidator(companyResourceValidatorMock, userResourceValidatorMock)

    private val bkk = TestUtils.createCompany(
        UUID.randomUUID().toString(), "bkk", "Beszterce KK",
        "besztercekk@email.com", "12345678-1-11", "11111111-22222222-33333333",
        "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), true,
        1, TestUtils.createAddress("3100", "Salgotarjan", "First Line 11", "", ""),
    )

    private val davidk = TestUtils.createUserProfile(
        "123456789", "davidk",
        "Krisztian", "David", "my@email.com",
        mutableListOf(CompanyRole(Roles.ROLE_ADMIN, "bkk"))
    )

    @BeforeEach
    fun initialise() {
        clearMocks(companyResourceValidatorMock, userResourceValidatorMock)
    }

    @Test
    fun `Check supporting the right class`() {

        validator.supports(CompanyWithAdminResource::class.java) shouldBe true
        validator.supports(String::class.java) shouldBe false
    }

    @Test
    fun `Check if company validator called`() {
        // given
        val resourceMock = mockk<CompanyWithAdminResource>()
        val errorsMock = mockk<BeanPropertyBindingResult>()
        every { resourceMock.companyResource } answers { CompanyConverter.toCompanyResource(bkk) }
        every { resourceMock.userResource } answers { null }
        every { companyResourceValidatorMock.validate(any(), any()) } answers { }
        every { errorsMock.addAllErrors(any()) } answers { }

        // when
        validator.validate(resourceMock, errorsMock)

        // then
        verify {
            userResourceValidatorMock wasNot called
        }
    }

    @Test
    fun `Check if user validator called`() {
        // given
        val resourceMock = mockk<CompanyWithAdminResource>()
        val errorsMock = mockk<BeanPropertyBindingResult>()
        every { resourceMock.companyResource } answers { CompanyConverter.toCompanyResource(bkk) }
        every { resourceMock.userResource } answers { UserConverter.toUserResource(davidk) }
        every { companyResourceValidatorMock.validate(any(), any()) } answers { }
        every { userResourceValidatorMock.validate(any(), any()) } answers { }
        every { errorsMock.addAllErrors(any()) } answers { }

        // when
        validator.validate(resourceMock, errorsMock)

        // then
        verify(exactly = 1) { userResourceValidatorMock.validate(any(), any()) }
        verify(exactly = 1) { companyResourceValidatorMock.validate(any(), any()) }
    }
}