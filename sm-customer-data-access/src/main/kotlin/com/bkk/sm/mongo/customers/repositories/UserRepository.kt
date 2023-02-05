package com.bkk.sm.mongo.customers.repositories

import com.bkk.sm.mongo.customers.model.user.UserProfile
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<UserProfile, String> {
    suspend fun findByUsername(username: String): UserProfile?
}
