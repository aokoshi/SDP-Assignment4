package shop;

import java.util.HashMap;
import java.util.Map;

public class OrderFacade {
    private final InventoryService inventory = new InventoryService();
    private final PaymentProcessor payment = new PaymentProcessor();
    private final ShippingService shipping = new ShippingService();
    private final NotificationService notifications = new NotificationService();
    private int nextOrderId = 1;

    public OrderReceipt placeOrder(Map<String, Integer> cart, Customer customer) {
        if (customer == null || cart == null) {
            throw new IllegalArgumentException("Customer and cart are required.");
        }
        Map<String, Integer> items = new HashMap<>(cart);
        InventoryService.Reservation reservation = inventory.reserve(items);
        String paymentId = null;
        String shipmentId = null;

        try {
            paymentId = payment.charge(customer.id(), reservation.total());
            shipmentId = shipping.schedule(customer.address(), items);
            OrderReceipt receipt = new OrderReceipt(
                    "ORD-" + nextOrderId++, reservation.total(), shipmentId);
            notifications.sendConfirmation(customer.email(), receipt);
            return receipt;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            shipping.cancel(shipmentId);
            payment.refund(paymentId);
            inventory.release(reservation.id());
            throw new IllegalStateException("Order failed: " + failure.getMessage(), failure);
        }
    }
}
