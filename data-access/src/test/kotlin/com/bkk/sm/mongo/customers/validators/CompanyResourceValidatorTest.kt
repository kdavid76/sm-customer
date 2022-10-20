package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.model.Address
import com.bkk.sm.mongo.customers.resources.CompanyResource
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import java.time.LocalDateTime

@ActiveProfiles("test")
class CompanyResourceValidatorTest {

    private val validator = CompanyResourceValidator()

    @Test
    fun `Check supporting the right class`(){
        Assertions.assertThat(validator.supports(CompanyResource::class.java)).isTrue
        Assertions.assertThat(validator.supports(String::class.java)).isFalse
    }

    @Test
    fun `Verifying valid resource`(){
        val companyResource = CompanyResource(id =  "123456789", name ="Beszterce KK", code = "bkk", email="bkk@gmail.com",
            address = Address(postCode = "3100", city = "Salgotarjan", firstLine = "Medves Krt. 86.", secondLine = null,
            thirdLine = null), activationTime = LocalDateTime.now(), registrationTime = LocalDateTime.now(),
            lastModificationTime = LocalDateTime.now())

        val errors: Errors = BeanPropertyBindingResult(companyResource, CompanyResource::class.java.name)

        validator.validate(companyResource, errors)
        Assertions.assertThat(errors.errorCount).isEqualTo(0)
    }

    @Test
    fun `Verifying invalid resource`(){
        val companyResource = CompanyResource(id =  "123456789", name =" ", code = " ", email="bkkgmail.com",
            address = Address(postCode = " ", city = " ", firstLine = " ", secondLine = null,
                thirdLine = null), activationTime = LocalDateTime.now(), registrationTime = LocalDateTime.now(),
            lastModificationTime = LocalDateTime.now())

        val errors: Errors = BeanPropertyBindingResult(companyResource, CompanyResource::class.java.name)

        validator.validate(companyResource, errors)
        Assertions.assertThat(errors.errorCount).isEqualTo(6)
    }
}