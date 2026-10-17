package shop;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class InventoryService {
    private final Map<String, BigDecimal> prices = Map.of(
            "KEYBOARD", new BigDecimal("50.00"),
            "MOUSE", new BigDecimal("25.00"));
    private final Map<String, Integer> stock = new HashMap<>(Map.of(
            "KEYBOARD", 2, "MOUSE", 5));
    private final Map<String, Map<String, Integer>> reservations = new HashMap<>();
    private int nextReservationId = 1;

    public Reservation reserve(Map<String, Integer> cart) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var item : cart.entrySet()) {
            String product = item.getKey();
            Integer quantity = item.getValue();
            if (product == null || !prices.containsKey(product)) {
                throw new IllegalArgumentException("Unknown product: " + product);
            }
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive: " + product);
            }
            if (quantity > stock.get(product)) {
                throw new IllegalStateException("Not enough stock: " + product);
            }
            total = total.add(prices.get(product).multiply(BigDecimal.valueOf(quantity)));
        }

        Map<String, Integer> items = Map.copyOf(cart);
        String reservationId = "RES-" + nextReservationId++;
        items.forEach((product, quantity) -> stock.put(product, stock.get(product) - quantity));
        reservations.put(reservationId, items);
        return new Reservation(reservationId, total);
    }

    public void release(String reservationId) {
        Map<String, Integer> items = reservations.remove(reservationId);
        if (items != null) {
            items.forEach((product, quantity) -> stock.merge(product, quantity, Integer::sum));
        }
    }

    public record Reservation(String id, BigDecimal total) {
    }
}
