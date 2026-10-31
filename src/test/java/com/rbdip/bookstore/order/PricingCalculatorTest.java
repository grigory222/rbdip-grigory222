package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {
    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({
        "10, 10, regular, NONE, 100.00",
        "10, 11, regular, NONE, 104.50",
        "100, 1, vip, NONE, 90.00",
        "100, 1, wholesale, NONE, 85.00",
        "100, 1, unknown, NONE, 100.00",
        "100, 1, VIP, NONE, 100.00",
        "100, 1, regular, SAVE10, 90.00",
        "100, 1, regular, SAVE20PERCENT, 80.00",
        "100, 1, vip, SAVE10, 80.00",
        "100, 1, wholesale, SAVE20PERCENT, 68.00",
        "5, 1, regular, SAVE10, 0.00",
        "10, 1, regular, SAVE10, 0.00",
        "0, 1, regular, NONE, 0.00",
        "1000, 1, regular, NONE, 1000.00",
        "999.99, 1, regular, NONE, 999.99",
        "1250, 1, regular, SAVE20PERCENT, 1000.00",
        "1250.01, 1, regular, SAVE20PERCENT, 980.01",
        "1100, 1, vip, NONE, 990.00",
        "2000, 1, wholesale, SAVE20PERCENT, 1332.80",
        "0.004, 1, regular, NONE, 0.00",
        "1000.01, 1, regular, NONE, 980.01",
        "1010, 1, regular, SAVE10, 1000.00",
        "1011, 1, regular, SAVE10, 980.98",
        "100, 11, vip, SAVE20PERCENT, 752.40",
        "0.005, 1, regular, NONE, 0.01",
        "-10, 1, regular, NONE, 0.00",
        "10, -1, regular, NONE, 0.00",
        "10, 0, regular, NONE, 0.00"
    })
    void preservesDiscountsAndBoundaries(String price, int quantity, String type, String coupon, String expected) {
        BigDecimal actual = calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), type, coupon);
        assertThat(actual).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void sumsLinesBeforeApplyingCustomerAndCouponDiscounts() {
        var items = List.of(new PricingCalculator.LineItem(new BigDecimal("10"), 2),
                new PricingCalculator.LineItem(new BigDecimal("5"), 11));
        assertThat(calculator.calculateOrderTotal(items, "vip", "SAVE10"))
                .isEqualTo(new BigDecimal("55.03"));
    }

    @Test
    void nullOptionsMeanNoDiscount() {
        assertThat(calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(BigDecimal.TEN, 1)), null, null))
                .isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void emptyOrderHasZeroTotal() {
        assertThat(calculator.calculateOrderTotal(List.of(), "regular", null))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void nullItemsAreNotAcceptedByCalculator() {
        assertThatThrownBy(() -> calculator.calculateOrderTotal(null, "regular", null))
                .isInstanceOf(NullPointerException.class);
    }
}
