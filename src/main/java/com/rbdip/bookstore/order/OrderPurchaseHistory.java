package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.purchase.PurchaseHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPurchaseHistory implements PurchaseHistory {
    private final OrderItemRepository items;

    public OrderPurchaseHistory(OrderItemRepository items) {
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchased(Long productId, String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return false;
        }
        Customer name = new Customer(customerName, null, null);
        return items.purchasedBy(productId, name.getFirstName(), name.getLastName());
    }
}
