package com.bkk.sm.mongo.customers.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.lang.NonNull

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import java.time.Instant
import java.util.*

@Document("users")
data class UserBase (
    @Id
    var id: String? = null,

    @Indexed
    @NonNull
    var username: String,

    @JsonIgnore
    @NonNull
    var password: String = "",

    var firstName: String = "",

    @Indexed
    var lastName: String = "",

    @Indexed
    var email: String = "",

    var failedLoginAttempts: Int? = 0,

    var passwordExpiringEnabled: Boolean = true,

    @Version
    var version: Int = 0,

    var roles: MutableList<CompanyRole>? = null,
    var registrationTime: Date? = null,
    var lastModificationTime: Date? = null,
    var passwordExpiryTime: Date? = null,
    var accountExpiryTime: Date? = null,
    var activationKey: String? = null,
    var activatedTime: Date? = null,
    var accountLocked:Boolean = true,
    var enabled: Boolean = false,
    var middleName: String? = null,
) {
    @JsonIgnore
    fun isAccountNonExpired() : Boolean = accountExpiryTime?.after(Date.from(Instant.now())) ?: true
    @JsonIgnore
    fun isPasswordNonExpired() : Boolean = passwordExpiryTime?.after(Date.from(Instant.now())) ?: true
    @JsonIgnore
    fun getGrantedAuthorities() : Collection<GrantedAuthority>  {
        val list = mutableListOf<GrantedAuthority>()
        list.addAll(roles?.asSequence()?.map { it.role }?.distinct()?.map { GrantedAuthority { it.name } }?.toList() ?: mutableListOf())
        return list
    }

}
