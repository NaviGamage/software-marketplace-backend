package com.marketplace.notification;

import com.marketplace.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ApplicationEventPublisher publisher;

    public void orderConfirmed(User buyer, Long orderId, BigDecimal total) {
        send(buyer, "Order #" + orderId + " confirmed",
                "Hi " + buyer.getFullName() + ",\n\n"
                        + "Thank you for your purchase. Your order #" + orderId
                        + " (total: " + total + ") is confirmed.\n"
                        + "You can download your files from your library.\n");
    }

    public void productApproved(User vendor, String productTitle) {
        send(vendor, "Your product was approved",
                "Hi " + vendor.getFullName() + ",\n\n"
                        + "Your product \"" + productTitle + "\" has been approved and is now live.\n");
    }

    public void productRejected(User vendor, String productTitle, String reason) {
        send(vendor, "Your product was rejected",
                "Hi " + vendor.getFullName() + ",\n\n"
                        + "Your product \"" + productTitle + "\" was rejected.\n"
                        + "Reason: " + reason + "\n");
    }

    public void disputeOpened(User vendor, String productTitle, String reason) {
        send(vendor, "A dispute was opened on your product",
                "Hi " + vendor.getFullName() + ",\n\n"
                        + "A buyer opened a dispute on \"" + productTitle + "\".\n"
                        + "Reason: " + reason + "\n"
                        + "Funds for this item are on hold until an admin resolves it.\n");
    }

    public void disputeResolved(User buyer, String productTitle, boolean refunded, String note) {
        send(buyer, "Your dispute was resolved",
                "Hi " + buyer.getFullName() + ",\n\n"
                        + "Your dispute on \"" + productTitle + "\" has been resolved: "
                        + (refunded ? "REFUNDED" : "DISMISSED") + ".\n"
                        + (note != null ? "Note: " + note + "\n" : ""));
    }

    public void payoutApproved(User vendor, BigDecimal amount) {
        send(vendor, "Your payout was approved",
                "Hi " + vendor.getFullName() + ",\n\n"
                        + "Your payout request of " + amount + " has been approved and processed.\n");
    }

    public void payoutRejected(User vendor, BigDecimal amount, String reason) {
        send(vendor, "Your payout was rejected",
                "Hi " + vendor.getFullName() + ",\n\n"
                        + "Your payout request of " + amount + " was rejected.\n"
                        + "Reason: " + reason + "\n"
                        + "The funds are available in your balance again.\n");
    }

    private void send(User to, String subject, String body) {
        publisher.publishEvent(new EmailNotificationEvent(to.getEmail(), subject, body));
    }
}