package com.bkk.sm.customers.services.impl

import com.bkk.sm.common.kafka.KafkaTopics
import com.bkk.sm.common.kafka.notification.Notification
import com.bkk.sm.customers.services.NotificationService
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaNotificationServiceImpl(
    private val notificationTemplate: KafkaTemplate<String, Notification>,
) : NotificationService<Notification> {
    override fun sendNotification(notification: Notification) {
        notificationTemplate.send(KafkaTopics.NOTIFICATIONS.topicName, notification)
    }
}
