package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PricingCalculator {
    private static final String VIP = "vip";
    private static final String WHOLESALE = "wholesale";
    private static final String FIXED_COUPON = "SAVE10";
    private static final String PERCENT_COUPON = "SAVE20PERCENT";
    private static final int BULK_QUANTITY = 10;
    private static final BigDecimal BULK_FACTOR = new BigDecimal("0.95");
    private static final BigDecimal VIP_FACTOR = new BigDecimal("0.9");
    private static final BigDecimal WHOLESALE_FACTOR = new BigDecimal("0.85");
    private static final BigDecimal COUPON_FACTOR = new BigDecimal("0.8");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_FACTOR = new BigDecimal("0.98");

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = subtotal(items);

        if (VIP.equals(customerType)) {
            total = total.multiply(VIP_FACTOR);
        } else if (WHOLESALE.equals(customerType)) {
            total = total.multiply(WHOLESALE_FACTOR);
        }

        if (FIXED_COUPON.equals(couponCode)) {
            total = total.subtract(BigDecimal.TEN);
        } else if (PERCENT_COUPON.equals(couponCode)) {
            total = total.multiply(COUPON_FACTOR);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_FACTOR);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal subtotal(List<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY) {
                linePrice = linePrice.multiply(BULK_FACTOR);
            }
            total = total.add(linePrice);
        }

        return total;
    }
}
