package shop;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class OrderFacadeTest {
    private static final Customer ALICE =
            new Customer("alice", "alice@example.com", "10 Campus Road");
    private static final Customer BOB =
            new Customer("bob", "bob@example.com", "20 Campus Road");

    public static void main(String[] args) {
        calculatesTotalAndCreatesReceipt();
        debitsBalanceOnSuccess();
        restoresStockAfterDeclinedPayment();
        refundsPaymentAfterShippingFailure();
        rollsBackAfterNotificationFailure();
        rejectsUnknownProductWithoutChangingStock();
        rejectsUnavailableCartWithoutPartialReservation();
        rejectsInvalidQuantities();
        rejectsEmptyCart();
        rejectsMissingInputs();
        restoresStockAfterUnknownAccount();
        releasesReservationOnlyOnce();
        refundsChargeOnlyOnce();
        createsAndCancelsShipmentDirectly();
        protectsNotificationOutbox();
        System.out.println("All 15 tests passed.");
    }

    private static void calculatesTotalAndCreatesReceipt() {
        OrderReceipt receipt = new OrderFacade().placeOrder(
                Map.of("KEYBOARD", 1, "MOUSE", 1), ALICE);
        check(receipt.total().compareTo(new BigDecimal("75.00")) == 0, "Cart total");
        check(receipt.orderId().equals("ORD-1"), "Order ID");
        check(receipt.shipmentId().equals("SHIP-1"), "Shipment ID");
    }

    private static void debitsBalanceOnSuccess() {
        OrderFacade shop = new OrderFacade();
        shop.placeOrder(Map.of("MOUSE", 3), ALICE);
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("MOUSE", 2), ALICE), "insufficient balance");
        shop.placeOrder(Map.of("MOUSE", 1), ALICE);
    }

    private static void restoresStockAfterDeclinedPayment() {
        OrderFacade shop = new OrderFacade();
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 2), BOB), "Payment declined");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 1), ALICE), "Not enough stock");
    }

    private static void refundsPaymentAfterShippingFailure() {
        OrderFacade shop = new OrderFacade();
        Customer missingAddress = new Customer("alice", "alice@example.com", " ");
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 2), missingAddress), "address");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
    }

    private static void rollsBackAfterNotificationFailure() {
        OrderFacade shop = new OrderFacade();
        Customer invalidEmail = new Customer("alice", "invalid-email", "10 Campus Road");
        RuntimeException failure = expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 2), invalidEmail), "email");
        check(failure.getCause() instanceof IllegalArgumentException, "Original failure cause");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
    }

    private static void rejectsUnknownProductWithoutChangingStock() {
        OrderFacade shop = new OrderFacade();
        expectFailure(IllegalArgumentException.class,
                () -> shop.placeOrder(Map.of("TABLET", 1), ALICE), "Unknown product");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
    }

    private static void rejectsUnavailableCartWithoutPartialReservation() {
        OrderFacade shop = new OrderFacade();
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 1, "MOUSE", 6), ALICE), "stock");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
    }

    private static void rejectsInvalidQuantities() {
        OrderFacade shop = new OrderFacade();
        for (int quantity : new int[]{0, -1}) {
            expectFailure(IllegalArgumentException.class,
                    () -> shop.placeOrder(Map.of("MOUSE", quantity), ALICE), "positive");
        }
        Map<String, Integer> cart = new HashMap<>();
        cart.put("MOUSE", null);
        expectFailure(IllegalArgumentException.class,
                () -> shop.placeOrder(cart, ALICE), "positive");
    }

    private static void rejectsEmptyCart() {
        expectFailure(IllegalArgumentException.class,
                () -> new OrderFacade().placeOrder(Map.of(), ALICE), "at least one item");
    }

    private static void rejectsMissingInputs() {
        OrderFacade shop = new OrderFacade();
        expectFailure(IllegalArgumentException.class,
                () -> shop.placeOrder(null, ALICE), "required");
        expectFailure(IllegalArgumentException.class,
                () -> shop.placeOrder(Map.of("MOUSE", 1), null), "required");
        Map<String, Integer> cart = new HashMap<>();
        cart.put(null, 1);
        expectFailure(IllegalArgumentException.class,
                () -> shop.placeOrder(cart, ALICE), "Unknown product");
    }

    private static void restoresStockAfterUnknownAccount() {
        OrderFacade shop = new OrderFacade();
        Customer unknown = new Customer("unknown", "new@example.com", "30 Campus Road");
        expectFailure(IllegalStateException.class,
                () -> shop.placeOrder(Map.of("KEYBOARD", 2), unknown), "Unknown payment account");
        shop.placeOrder(Map.of("KEYBOARD", 2), ALICE);
    }

    private static void releasesReservationOnlyOnce() {
        InventoryService inventory = new InventoryService();
        InventoryService.Reservation reservation = inventory.reserve(Map.of("KEYBOARD", 2));
        inventory.release(reservation.id());
        inventory.release(reservation.id());
        inventory.reserve(Map.of("KEYBOARD", 2));
        expectFailure(IllegalStateException.class,
                () -> inventory.reserve(Map.of("KEYBOARD", 1)), "stock");
    }

    private static void refundsChargeOnlyOnce() {
        PaymentProcessor payment = new PaymentProcessor();
        String paymentId = payment.charge("alice", new BigDecimal("100.00"));
        payment.refund(paymentId);
        payment.refund(paymentId);
        payment.charge("alice", new BigDecimal("100.00"));
        expectFailure(IllegalStateException.class,
                () -> payment.charge("alice", BigDecimal.ONE), "insufficient balance");
    }

    private static void createsAndCancelsShipmentDirectly() {
        ShippingService shipping = new ShippingService();
        String shipmentId = shipping.schedule(ALICE.address(), Map.of("MOUSE", 1));
        check(shipping.cancel(shipmentId), "Existing shipment is cancelled");
        check(!shipping.cancel(shipmentId), "Shipment cannot be cancelled twice");
    }

    private static void protectsNotificationOutbox() {
        NotificationService notifications = new NotificationService();
        OrderReceipt receipt = new OrderReceipt("ORD-1", new BigDecimal("25.00"), "SHIP-1");
        notifications.sendConfirmation(ALICE.email(), receipt);
        check(notifications.getMessages().size() == 1, "One queued notification");
        check(notifications.getMessages().get(0).contains("25.00 USD"), "Notification total");
        expectFailure(UnsupportedOperationException.class,
                () -> notifications.getMessages().clear(), null);
        expectFailure(IllegalArgumentException.class,
                () -> notifications.sendConfirmation("bad-email", receipt), "email");
        check(notifications.getMessages().size() == 1, "Invalid email adds no message");
    }

    private static RuntimeException expectFailure(
            Class<? extends RuntimeException> type, Runnable action, String messagePart) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            check(type.isInstance(failure), "Expected " + type.getSimpleName());
            if (messagePart != null) {
                check(failure.getMessage().contains(messagePart), "Expected message: " + messagePart);
            }
            return failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
