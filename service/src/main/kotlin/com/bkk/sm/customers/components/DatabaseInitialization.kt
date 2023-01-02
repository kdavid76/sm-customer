package com.bkk.sm.customers.components

import com.bkk.sm.common.customer.company.CompanyRole
import com.bkk.sm.common.model.Roles
import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.repositories.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@ConditionalOnProperty(value = ["com.bkk.sm.mongo.customers.init.enable"], havingValue = "true")
@Component
class DatabaseInitialization(
    private val userMongoRepository: UserRepository,
    @Qualifier("superUserProperties") private val userProperties: Properties
) {
    val log = KotlinLogging.logger {}

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init() {
        val username = userProperties.getProperty("username")

        runBlocking {
            launch {
                val existingUser = userMongoRepository.findByUsername(username)
                val companyRoles = mutableListOf(CompanyRole(Roles.ROLE_SUPERADMIN, "system"))
                if (existingUser == null) {
                    val userProfile = UserProfile(
                        username = username,
                        password = userProperties.getProperty("password"),
                        firstName = userProperties.getProperty("firstname"),
                        lastName = userProperties.getProperty("lastname"),
                        email = userProperties.getProperty("email"),
                        version = 0,
                        accountLocked = false,
                        enabled = true,
                        failedLoginAttempts = 0,
                        activationKey = "",
                        activatedTime = Date.from(Instant.now()),
                        lastModificationTime = Date.from(Instant.now()),
                        passwordExpiryTime = null,
                        roles = companyRoles,
                        passwordExpiringEnabled = false
                    )
                    val savedUser = userMongoRepository.save(userProfile)
                    log.info { "Superuser has been added=$savedUser" }
                } else {
                    log.info { "Superuser has already been defined=$existingUser" }
                }
            }
        }
    }
}
