package com.bkk.sm.customers.config

// @Configuration
class TopicConfiguration {
    /*
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

     */
}
