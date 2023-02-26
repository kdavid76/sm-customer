package com.bkk.sm.mongo.customers.config

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.util.*

@Configuration
@EnableReactiveMongoRepositories(basePackages = ["com.bkk.sm.mongo.customers.repositories"])
class CustomersMongoConfig : AbstractReactiveMongoConfiguration() {

    @Value("\${com.bkk.sm.mongo.customers.database}")
    val database: String = ""

    @Value("\${com.bkk.sm.mongo.customers.uri}")
    val uri: String = ""

    override fun getDatabaseName(): String = database

    @Bean(name = ["superUserProperties"])
    @ConfigurationProperties(prefix = "com.bkk.sm.mongo.customers.init.superuser")
    fun superUserProperties(): Properties = Properties()

    @Bean(name = ["customerMongoClient"])
    override fun reactiveMongoClient(): MongoClient = MongoClients.create(uri)

    @Bean(name = ["customerMongoDbFactory"])
    override fun reactiveMongoDbFactory(): ReactiveMongoDatabaseFactory =
        SimpleReactiveMongoDatabaseFactory(reactiveMongoClient(), databaseName)

    @Bean(name = ["customerMongoTemplate"])
    override fun reactiveMongoTemplate(
        databaseFactory: ReactiveMongoDatabaseFactory,
        mongoConverter: MappingMongoConverter,
    ): ReactiveMongoTemplate = ReactiveMongoTemplate(databaseFactory, mongoConverter)
}
