package com.marketplace.notification;

public record EmailNotificationEvent(String to, String subject, String body) {
}