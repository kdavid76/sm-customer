package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.kafka.KafkaTopics
import com.bkk.sm.common.kafka.notification.Notification
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
@ExtendWith(MockKExtension::class)
class KafkaNotificationServiceImplTest {

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
}
