package com.bkk.sm.customers.handlers

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

interface UserHandler {
    suspend fun findAll(request: ServerRequest): ServerResponse
    suspend fun findByUsername(request: ServerRequest): ServerResponse
    suspend fun add(request: ServerRequest): ServerResponse
    suspend fun activate(request: ServerRequest): ServerResponse
}
