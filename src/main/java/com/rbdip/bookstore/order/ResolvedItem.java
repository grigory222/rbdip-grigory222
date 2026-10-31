package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

public record ResolvedItem(Product product, int quantity) {
    public PricingCalculator.LineItem priceLine() {
        return new PricingCalculator.LineItem(product.getPrice(), quantity);
    }
}
