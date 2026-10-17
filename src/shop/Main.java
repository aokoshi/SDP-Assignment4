package shop;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        OrderFacade shop = new OrderFacade();
        Customer alice = new Customer("alice", "alice@example.com", "10 Campus Road");
        Customer bob = new Customer("bob", "bob@example.com", "20 Campus Road");
        Map<String, Integer> cart = Map.of("KEYBOARD", 2);

        System.out.println("1. Bob tries to buy two keyboards with a 10.00 USD balance.");
        try {
            shop.placeOrder(cart, bob);
        } catch (IllegalStateException failure) {
            System.out.println(failure.getMessage());
        }

        System.out.println("\n2. Alice buys the same two keyboards after rollback.");
        OrderReceipt receipt = shop.placeOrder(cart, alice);
        System.out.println("Order: " + receipt.orderId());
        System.out.println("Total: " + receipt.total() + " USD");
        System.out.println("Shipment: " + receipt.shipmentId());
        System.out.println("Order confirmed. The notification is stored in the local outbox.");

        System.out.println("\n3. Another keyboard order fails because stock is now zero.");
        try {
            shop.placeOrder(Map.of("KEYBOARD", 1), alice);
        } catch (IllegalStateException failure) {
            System.out.println(failure.getMessage());
        }
    }
}
