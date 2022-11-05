package com.bkk.sm.mongo.customers.resources

data class ActivateUserResource(
    val username: String,
    val activationCode: String
)