package com.bkk.sm.mongo.customers.model

data class Address(
    val postCode: String,
    val city: String,
    val firstLine: String,
    val secondLine: String?,
    val thirdLine: String?
) {
    override fun toString(): String = "Address[postCode=$postCode, city=$city, firstLine=$firstLine," +
            " secondLine=${secondLine ?: ""}, thirdLine=${thirdLine ?: ""}]"
}