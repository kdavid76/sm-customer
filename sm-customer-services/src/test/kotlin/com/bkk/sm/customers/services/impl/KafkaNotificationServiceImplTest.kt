package com.bkk.sm.customers.services.impl

// @ActiveProfiles("test")
// @ExtendWith(MockKExtension::class)
class KafkaNotificationServiceImplTest {
    /*
        @MockK
        lateinit var kafkaTemplate: KafkaTemplate<String, Notification>

        lateinit var service: KafkaNotificationServiceImpl

        @BeforeEach
        fun initMocks() {
            MockKAnnotations.init(this)
            service = KafkaNotificationServiceImpl(kafkaTemplate)
        }

        @Test
        fun `Kafka called with message`() {
            val notification = mockk<Notification>()
            val result = mockk<CompletableFuture<SendResult<String, Notification>>>()

            every {
                kafkaTemplate.send(eq(KafkaTopics.NOTIFICATIONS.topicName), any())
            } returns result
            service.sendNotification(notification)

            verify { kafkaTemplate.send(eq(KafkaTopics.NOTIFICATIONS.topicName), any()) }
        }
     */
}
