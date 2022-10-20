package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.model.Roles
import com.bkk.sm.mongo.customers.model.company.CompanyRole
import com.bkk.sm.mongo.customers.resources.UserResource
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

@ActiveProfiles("test")
class UserResourceValidatorTest {
    private val validator = UserResourceValidator()

    @Test
    fun `Check supporting the right class`(){
        Assertions.assertThat(validator.supports(UserResource::class.java)).isTrue
        Assertions.assertThat(validator.supports(String::class.java)).isFalse
    }

    @Test
    fun `Rejecting whitespace errors`(){
        val userResource = UserResource(id = "123456", firstName = " ", lastName = " ", email = " ",
            username = " "
        )

        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)

        validator.validate(userResource, errors)

        Assertions.assertThat(errors.errorCount).isEqualTo(6)
        Assertions.assertThat(errors.getFieldError("email")).isNotNull
        Assertions.assertThat(errors.getFieldError("firstName")).isNotNull
        Assertions.assertThat(errors.getFieldError("username")).isNotNull
        Assertions.assertThat(errors.getFieldError("lastName")).isNotNull
        Assertions.assertThat(errors.getFieldError("roles")).isNotNull
    }

    @Test
    fun `Rejecting email and password format errors`(){
        val userResource = UserResource(id = "123456", firstName = "firstName", lastName = "lastName", email = "emailemail.com",
            username = "username", password = "passwd", roles = mutableListOf()
        )

        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)

        validator.validate(userResource, errors)

        Assertions.assertThat(errors.errorCount).isEqualTo(3)
        Assertions.assertThat(errors.getFieldError("email")).isNotNull
        Assertions.assertThat(errors.getFieldError("password")).isNotNull
        Assertions.assertThat(errors.getFieldError("roles")).isNotNull
    }

    @Test
    fun `Verifying valid resource`(){
        val userResource = UserResource(id = "123456", firstName = "firstName", lastName = "lastName", email = "email@email.com",
            username = "username", password = "Pass?Word_1", roles = mutableListOf(CompanyRole(Roles.ROLE_USER, "bkk")),
        )

        val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)

        validator.validate(userResource, errors)

        Assertions.assertThat(errors.errorCount).isEqualTo(0)
    }
}