package com.bkk.sm.mongo.customers.repositories

import com.bkk.sm.mongo.customers.model.UserBase
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<UserBase, String> {
    suspend fun findByUsername(username: String): UserBase?
}