package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.resources.UserResource
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator
import java.util.regex.Pattern

@Component
class UserResourceValidator: Validator {

    companion object {
        val PASSWORD_PATTERN: Pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*#?&_\\-+=\\(\\)§:,;])[a-zA-Z\\d@\$!%*#?&_\\-+=\\(\\)§:,;]{8,}\$")
        val EMAIL_PATTERN: Pattern = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,5}\$")
    }

    override fun supports(clazz: Class<*>): Boolean {
        return UserResource::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        val user = target as UserResource

        // Validate mandatory String fields
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "errors.user.resource.username.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstName", "errors.user.resource.firstname.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "lastName", "errors.user.resource.lastname.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "errors.user.resource.email.required")

        // At least one valid CompanyRole is required
        val roles = user.roles
        roles?.let {
            if(roles.size == 0) {
                errors.rejectValue("roles", "errors.user.resource.roles.required")
            }
        } ?: run {
            errors.rejectValue("roles", "errors.user.resource.roles.required")
        }

        // If password defined it must match the required format
        user.password?.let {
            val matcher = PASSWORD_PATTERN.matcher(user.password)
            if(!matcher.matches()) {
                errors.rejectValue("password", "errors.user.resource.password.format")
            }
        }

        // Email address must be in the right format
        val emailMatcher = EMAIL_PATTERN.matcher(user.email)
        if(!emailMatcher.matches()) {
            errors.rejectValue("email", "errors.user.resource.email.format")
        }
    }
}