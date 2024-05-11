package com.bkk.sm.customers.services

fun interface NotificationService<T> {
    fun sendNotification(notification: T)
}
