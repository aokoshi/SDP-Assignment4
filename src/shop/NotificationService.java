package shop;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final List<String> messages = new ArrayList<>();

    public void sendConfirmation(String email, OrderReceipt receipt) {
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("A valid notification email is required.");
        }
        String message = "To: " + email + " | Order " + receipt.orderId()
                + " confirmed | Total: " + receipt.total()
                + " USD | Shipment: " + receipt.shipmentId();
        messages.add(message);
    }

    public List<String> getMessages() {
        return List.copyOf(messages);
    }
}
