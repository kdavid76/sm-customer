package com.bkk.sm.customers.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder(16)

    @Bean
    fun configureSecurity(http: ServerHttpSecurity): SecurityWebFilterChain {

        return http
            .csrf { csrf -> csrf.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .authorizeExchange { exchanges ->
                exchanges.pathMatchers("/users/**").permitAll()
                    .pathMatchers("/companies/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().denyAll()
            }
            .build()
    }
}
