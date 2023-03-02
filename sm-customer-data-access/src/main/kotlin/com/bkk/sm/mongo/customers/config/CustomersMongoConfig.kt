package com.bkk.sm.mongo.customers.config

import com.bkk.sm.common.mongo.MongoConfigurationSupportForSM
import com.bkk.sm.mongo.customers.repositories.CompanyRepository
import com.bkk.sm.mongo.customers.repositories.UserRepository
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoManagedTypes
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.util.*

@Configuration
@EnableReactiveMongoRepositories(
    basePackageClasses = [UserRepository::class, CompanyRepository::class],
    reactiveMongoTemplateRef = "customerMongoTemplate",
)
class CustomersMongoConfig : MongoConfigurationSupportForSM() {

    @Value("\${com.bkk.sm.mongo.customers.database}")
    val database: String = ""

    @Value("\${com.bkk.sm.mongo.customers.uri}")
    val uri: String = ""
    override fun configureConverters(converterConfigurationAdapter: MongoConverterConfigurationAdapter?) {
        // No extra converters
    }

    override fun getDatabaseName(): String = database

    @Bean
    @Qualifier("superUserProperties")
    @ConfigurationProperties(prefix = "com.bkk.sm.mongo.customers.init.superuser")
    fun superUserProperties(): Properties = Properties()

    @Bean
    @Qualifier("customerMongoClient")
    fun reactiveMongoClient(): MongoClient = MongoClients.create(uri)

    @Bean("customerMongoTemplate")
    fun reactiveMongoTemplate(
        @Qualifier("customerMongoDbFactory") databaseFactory: ReactiveMongoDatabaseFactory,
        @Qualifier("customerMappingMongoConverter") mongoConverter: MappingMongoConverter,
    ): ReactiveMongoTemplate = ReactiveMongoTemplate(databaseFactory, mongoConverter)

    @Bean
    @Qualifier("customerMongoDbFactory")
    fun reactiveMongoDbFactory(): ReactiveMongoDatabaseFactory =
        SimpleReactiveMongoDatabaseFactory(reactiveMongoClient(), getDatabaseName())

    @Bean
    @Qualifier("customerMappingMongoConverter")
    fun mappingMongoConverter(
        @Qualifier("customerMongoDbFactory") databaseFactory: ReactiveMongoDatabaseFactory?,
        @Qualifier("customerMongoCustomConversion") customConversions: MongoCustomConversions?,
        @Qualifier("customerMongoMappingContext") mappingContext: MongoMappingContext?,
    ): MappingMongoConverter? {
        val converter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext!!)
        converter.customConversions = customConversions!!
        converter.setCodecRegistryProvider(databaseFactory)
        return converter
    }

    @Bean
    @Qualifier("customerMongoMappingContext")
    fun mongoMappingContext(
        @Qualifier("customerMongoCustomConversion") customConversions: MongoCustomConversions,
        @Qualifier("customerMongoManagedTypes") mongoManagedTypes: MongoManagedTypes?,
    ): MongoMappingContext? {
        val mappingContext = MongoMappingContext()
        mappingContext.setManagedTypes(mongoManagedTypes!!)
        mappingContext.setSimpleTypeHolder(customConversions.simpleTypeHolder)
        mappingContext.setFieldNamingStrategy(fieldNamingStrategy())
        mappingContext.isAutoIndexCreation = autoIndexCreation()
        return mappingContext
    }

    @Bean
    @Qualifier("customerMongoCustomConversion")
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions.create { converterConfigurationAdapter: MongoConverterConfigurationAdapter? ->
            configureConverters(
                converterConfigurationAdapter!!,
            )
        }
    }

    @Bean
    @Qualifier("customerMongoManagedTypes")
    @Throws(ClassNotFoundException::class)
    fun mongoManagedTypes(): MongoManagedTypes {
        return MongoManagedTypes.fromIterable(getInitialEntitySet())
    }
}
