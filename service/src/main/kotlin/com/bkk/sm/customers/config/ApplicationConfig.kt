package com.bkk.sm.customers.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

@Configuration
class ApplicationConfig {

    @Bean
    fun transactionManager(rdbf: ReactiveMongoDatabaseFactory): ReactiveMongoTransactionManager {
        return ReactiveMongoTransactionManager(rdbf)
    }

    @Bean
    fun transactionOperator(rtm: ReactiveTransactionManager): TransactionalOperator {
        return TransactionalOperator.create(rtm)
    }
}