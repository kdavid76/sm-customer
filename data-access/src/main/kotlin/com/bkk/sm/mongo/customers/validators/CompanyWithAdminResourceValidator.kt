package com.bkk.sm.mongo.customers.validators

import com.bkk.sm.mongo.customers.resources.CompanyWithAdminResource
import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Component
class CompanyWithAdminResourceValidator (
    private val companyValidator: CompanyResourceValidator,
    private val userValidator: UserResourceValidator
): Validator {

    override fun supports(clazz: Class<*>): Boolean {
        return CompanyWithAdminResource::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        val resource = target as CompanyWithAdminResource


        val companyErrors = BeanPropertyBindingResult(resource.companyResource, CompanyWithAdminResource::class.java.name)
        companyValidator.validate(resource.companyResource, companyErrors)
        errors.addAllErrors(companyErrors)

        resource.userResource?.let {
            val userErrors = BeanPropertyBindingResult(it, CompanyWithAdminResource::class.java.name)
            userValidator.validate(it, userErrors)
            errors.addAllErrors(userErrors)
        }
    }
}