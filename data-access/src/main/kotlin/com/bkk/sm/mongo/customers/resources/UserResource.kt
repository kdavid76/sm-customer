package com.bkk.sm.mongo.customers.resources

import com.bkk.sm.mongo.customers.model.company.CompanyRole
import java.util.*

data class UserResource(
    var id: String? = null,
    var username: String,
    var password: String? = null,
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var failedLoginAttempts: Int? = 0,
    var roles: MutableList<CompanyRole>? = ArrayList(),
    var registrationTime: Date? = null,
    var lastModificationTime: Date? = null,
    var passwordExpiringEnabled: Boolean = true,
    var passwordExpiryTime: Date? = null,
    var activationKey: String? = null,
    var activatedTime: Date? = null,
    var accountLocked:Boolean = true,
    var enabled: Boolean = false,
    var middleName: String? = null,
)