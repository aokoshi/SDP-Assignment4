package shop;

import java.math.BigDecimal;

public record OrderReceipt(String orderId, BigDecimal total, String shipmentId) {
}
