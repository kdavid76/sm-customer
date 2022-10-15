package com.bkk.sm.mongo.customers.model

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

class SmUser(userBase: UserBase) :
    User(userBase.username, userBase.password, userBase.enabled,
            userBase.isAccountNonExpired(), userBase.isPasswordNonExpired(),
            !userBase.accountLocked, userBase.getGrantedAuthorities()), UserDetails {
}