package com.bkk.sm.mongo.customers.converters

import com.bkk.sm.mongo.customers.model.user.UserProfile
import com.bkk.sm.mongo.customers.resources.UserResource

class UserConverter {
    companion object {
        val PASSWORD_MASK: String = "*****"

        fun toUserResource(user: UserProfile): UserResource {
            return UserResource(
                id = user.id,
                username = user.username,
                firstName = user.firstName,
                lastName = user.lastName,
                middleName = user.middleName,
                email = user.email,
                registrationTime = user.registrationTime,
                enabled = user.enabled,
                accountLocked = user.accountLocked,
                activatedTime = user.activatedTime,
                lastModificationTime = user.lastModificationTime,
                passwordExpiryTime = user.passwordExpiryTime,
                password = if (user.password.isBlank()) null else PASSWORD_MASK,
                failedLoginAttempts = user.failedLoginAttempts,
                activationKey = user.activationKey,
                roles = user.roles,
                passwordExpiringEnabled = user.passwordExpiringEnabled
            )
        }

        fun toUserBase(user: UserResource): UserProfile {
            return UserProfile(
                id = user.id, username = user.username, firstName = user.firstName,
                lastName = user.lastName, middleName = user.middleName,
                email = user.email, registrationTime = user.registrationTime,
                enabled = user.enabled, accountLocked = user.accountLocked,
                activatedTime = user.activatedTime, lastModificationTime = user.lastModificationTime,
                passwordExpiryTime = user.passwordExpiryTime, password = user.password ?: "",
                failedLoginAttempts = user.failedLoginAttempts, activationKey = user.activationKey,
                roles = user.roles, passwordExpiringEnabled = user.passwordExpiringEnabled
            )
        }
    }

}