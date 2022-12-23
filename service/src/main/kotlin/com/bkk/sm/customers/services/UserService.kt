package com.bkk.sm.customers.services

import com.bkk.sm.common.customer.resources.UserResource
import org.springframework.web.reactive.function.server.ServerResponse

interface UserService {
    suspend fun findAllUsers(): ServerResponse
    suspend fun findUserByUsername(username: String): ServerResponse
    suspend fun registerUser(userResource: UserResource): ServerResponse
}