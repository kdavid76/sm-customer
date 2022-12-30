package com.bkk.sm.customers.config

import com.bkk.sm.common.customer.validators.CompanyResourceValidator
import com.bkk.sm.common.customer.validators.UserResourceValidator
import com.bkk.sm.customers.services.CompanyService
import com.bkk.sm.customers.services.UserService
import com.bkk.sm.customers.services.impl.CompanyServiceImpl
import com.bkk.sm.customers.services.impl.UserServiceImpl
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@Profile("test")
class TestConfig {

    @Bean
    fun userResourceValidator(): UserResourceValidator = UserResourceValidator()

    @Bean
    fun companyResourceValidator(): CompanyResourceValidator = CompanyResourceValidator()

    @Bean
    fun userService(userRepository: UserRepository, passwordEncoder: PasswordEncoder): UserService = UserServiceImpl(userRepository, userResourceValidator(), passwordEncoder)

    @Bean
    fun companyService(userRepository: UserRepository, companyRepository: CompanyRepository, passwordEncoder: PasswordEncoder):
            CompanyService = CompanyServiceImpl(userResourceValidator(), companyResourceValidator(), companyRepository, userRepository, passwordEncoder)
}