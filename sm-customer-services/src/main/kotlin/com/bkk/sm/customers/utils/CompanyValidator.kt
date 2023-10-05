package com.bkk.sm.customers.utils

import com.bkk.sm.common.customer.resources.CompanyResource
import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

class CompanyValidator {
    companion object {
        fun validateCompany(companyResource: CompanyResource, validator: CompanyResourceValidator): Errors {
            val errors: Errors = BeanPropertyBindingResult(companyResource, CompanyResource::class.java.name)
            validator.validate(companyResource, errors)
            return errors
        }
    }
}
