package com.bkk.sm.customers.utils

import com.bkk.sm.mongo.customers.model.CompanyRole
import com.bkk.sm.mongo.customers.model.UserBase
import com.bkk.sm.mongo.customers.resources.UserResource
import com.ninjasquad.springmockk.isMock

class TestUtils {

    companion object {
        fun createUser(
            id: String,
            username: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>
        ) = UserBase(id = id, username = username, firstName = firstName, lastName = lastName, roles = roles)

        fun createUserResource(
            id: String,
            username: String,
            password: String,
            firstName: String,
            lastName: String,
            email: String,
            roles: MutableList<CompanyRole>
        ) = UserResource(id = id, username = username, password = password, firstName = firstName,
                lastName = lastName, email = email, roles = roles)
    }
}