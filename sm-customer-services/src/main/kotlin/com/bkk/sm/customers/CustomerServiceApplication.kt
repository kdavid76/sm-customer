package com.bkk.sm.customers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.bkk.sm.mongo.customers", "com.bkk.sm.customers", "com.bkk.sm.common"]
)
class CustomerServiceApplication

fun main(args: Array<String>) {
    runApplication<CustomerServiceApplication>(*args)
}
