package shop;

import java.util.HashMap;
import java.util.Map;

public class ShippingService {
    private final Map<String, Shipment> shipments = new HashMap<>();
    private int nextShipmentId = 1;

    public String schedule(String address, Map<String, Integer> items) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Shipping address is required.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Shipment must contain items.");
        }
        Map<String, Integer> shipmentItems = Map.copyOf(items);
        for (var item : shipmentItems.entrySet()) {
            if (item.getValue() <= 0) {
                throw new IllegalArgumentException("Shipment quantities must be positive.");
            }
        }

        String shipmentId = "SHIP-" + nextShipmentId++;
        shipments.put(shipmentId, new Shipment(address, shipmentItems));
        return shipmentId;
    }

    public boolean cancel(String shipmentId) {
        return shipments.remove(shipmentId) != null;
    }

    private record Shipment(String address, Map<String, Integer> items) {
    }
}
