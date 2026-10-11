package com.smartstart.portal.notification.service;

import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;

/** Delivery abstraction: email today, SMS (or others) later. */
public interface NotificationSender {

    NotificationChannel channel();

    /** Delivers the notification to all its recipients or throws with a human-readable reason. */
    void send(Notification notification) throws NotificationDeliveryException;

    class NotificationDeliveryException extends Exception {
        public NotificationDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
