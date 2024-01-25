package com.bkk.sm.customers.utils

import com.bkk.sm.common.customer.resources.UserResource
import com.bkk.sm.common.customer.validators.UserResourceValidator
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

class UserValidator {

    companion object {
        fun validateUser(
            userResource: UserResource,
            validator: UserResourceValidator,
            checkPassword: Boolean = true,
        ): Errors {
            val errors: Errors = BeanPropertyBindingResult(userResource, UserResource::class.java.name)
            validator.validate(userResource, errors)
            if (checkPassword && userResource.password == null) {
                errors.rejectValue("password", "errors.user.resource.password.missing")
            }
            return errors
        }
    }
}
