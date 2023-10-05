package com.bkk.sm.customers.config

import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestConfig {

    @Bean
    fun userResourceValidator(): UserResourceValidator = UserResourceValidator()

    @Bean
    fun companyResourceValidator(): CompanyResourceValidator = CompanyResourceValidator()
}
