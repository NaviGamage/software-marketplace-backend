package com.marketplace.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    /**
     * Runs only after the publishing transaction commits, and on a separate
     * thread. A mail failure is logged and swallowed — it must never break
     * the business operation that triggered it.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(EmailNotificationEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.to());
            message.setSubject(event.subject());
            message.setText(event.body());
            mailSender.send(message);
            log.info("Email sent to {} — {}", event.to(), event.subject());
        } catch (Exception e) {
            log.error("Failed to send email to {} — {}", event.to(), event.subject(), e);
        }
    }
}