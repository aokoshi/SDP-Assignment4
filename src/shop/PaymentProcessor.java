package shop;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PaymentProcessor {
    private final Map<String, BigDecimal> balances = new HashMap<>(Map.of(
            "alice", new BigDecimal("100.00"),
            "bob", new BigDecimal("10.00")));
    private final Map<String, Charge> charges = new HashMap<>();
    private int nextPaymentId = 1;

    public String charge(String customerId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        BigDecimal balance = balances.get(customerId);
        if (balance == null) {
            throw new IllegalArgumentException("Unknown payment account: " + customerId);
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Payment declined: insufficient balance.");
        }

        String paymentId = "PAY-" + nextPaymentId++;
        balances.put(customerId, balance.subtract(amount));
        charges.put(paymentId, new Charge(customerId, amount));
        return paymentId;
    }

    public void refund(String paymentId) {
        Charge charge = charges.remove(paymentId);
        if (charge != null) {
            balances.merge(charge.customerId(), charge.amount(), BigDecimal::add);
        }
    }

    private record Charge(String customerId, BigDecimal amount) {
    }
}
