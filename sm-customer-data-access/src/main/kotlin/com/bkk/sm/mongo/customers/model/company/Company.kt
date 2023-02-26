package com.bkk.sm.mongo.customers.model.company

import com.bkk.sm.common.model.Address
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("companies")
data class Company(
    @Id
    var id: String?,

    @Indexed
    var code: String,

    var name: String,

    @Indexed
    var email: String,

    var address: Address,

    @Indexed
    var taxId: String? = "",

    var bankAccountNumber: String? = "",
    var optionalContactInfo: String? = "",
    var activationToken: String? = "",

    var activationTime: LocalDateTime?,
    var registrationTime: LocalDateTime?,
    var lastModificationTime: LocalDateTime?,

    var enabled: Boolean? = false,

    @Version
    var version: Long = 0,
) {
    override fun toString(): String = "Company[id=${id ?: "N/A"}, code=$code}, name=$name, email=$email," +
        " address=$address, taxId=${taxId ?: ""}, bankAccountNumber=${bankAccountNumber ?: ""}," +
        "optionalContactInfo=${optionalContactInfo ?: ""}, activationToken=${activationToken ?: ""}," +
        " activationTime=${activationTime ?: ""}, registrationTime=${registrationTime ?: ""}," +
        " lastModificationTime=${lastModificationTime ?: ""}, enabled=${enabled ?: ""}, version=$version]"
}
