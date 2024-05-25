package com.bkk.sm.customers.services.impl

import com.bkk.sm.customers.services.NotificationService

// @Component
class KafkaNotificationServiceImpl : NotificationService<String> {

    override fun sendNotification(notification: String) {
        // notificationTemplate.send(KafkaTopics.NOTIFICATIONS.topicName, notification)
    }
}
