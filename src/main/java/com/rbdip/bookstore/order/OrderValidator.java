package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {
    private final ProductRepository products;

    public OrderValidator(ProductRepository products) {
        this.products = products;
    }

    public List<ResolvedItem> validate(CreateOrderRequest request) {
        requireText(request.customerFullName(), "customerFullName");
        requireText(request.customerAddress(), "customerAddress");
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        return request.items().stream().map(this::resolve).toList();
    }

    private ResolvedItem resolve(CreateOrderRequest.Item item) {
        Product product = products.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
        int quantity = item.quantity() == null ? 1 : item.quantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return new ResolvedItem(product, quantity);
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
