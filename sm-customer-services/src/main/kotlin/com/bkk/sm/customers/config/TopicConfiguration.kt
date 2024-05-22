package com.bkk.sm.customers.config

import com.bkk.sm.common.kafka.KafkaTopics
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin

// @Configuration
class TopicConfiguration {

    @Value("\${spring.kafka.producer.bootstrap-servers}")
    val kafkaBootstrap: String = ""

    @Bean
    fun admin(): KafkaAdmin {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBootstrap
        return KafkaAdmin(configs)
    }

    @Bean
    fun notificationTopic(): NewTopic = TopicBuilder.name(KafkaTopics.NOTIFICATIONS.topicName)
        .partitions(1)
        .replicas(1)
        .build()
}
