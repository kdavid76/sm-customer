package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.resources.CompanyResource
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator

@Component
class CompanyResourceValidator: Validator {

    override fun supports(clazz: Class<*>): Boolean {
        return CompanyResource::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "code", "errors.company.resource.code.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "errors.company.resource.name.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "errors.company.resource.email.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "address.postCode", "errors.company.resource.address.postcode.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "address.city", "errors.company.resource.address.city.required")
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "address.firstLine", "errors.company.resource.address.firstline.required")

        val company = target as CompanyResource

        // Email address must be in the right format
        val emailMatcher = UserResourceValidator.EMAIL_PATTERN.matcher(company.email)
        if(!emailMatcher.matches()) {
            errors.rejectValue("email", "errors.user.resource.email.format")
        }
    }
}