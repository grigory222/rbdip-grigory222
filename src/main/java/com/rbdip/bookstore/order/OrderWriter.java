package com.rbdip.bookstore.order;

import java.util.List;
import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderWriter {
    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final OrderItemRepository items;

    public OrderWriter(OrderRepository orders, OrderItemRepository items, CustomerRepository customers) {
        this.customers = customers;
        this.orders = orders;
        this.items = items;
    }

    public Order save(CreateOrderRequest request, List<ResolvedItem> resolved) {
        Customer candidate = new Customer(
                request.customerFullName(), request.customerAddress(), request.customerPhone());
        Customer customer = customers.findByFirstNameAndLastNameAndAddressAndPhone(
                candidate.getFirstName(), candidate.getLastName(), candidate.getAddress(), candidate.getPhone())
                .orElseGet(() -> customers.save(candidate));
        Order order = orders.save(new Order(customer, "new"));
        for (ResolvedItem item : resolved) {
            items.save(new OrderItem(order.getId(), item.product(), item.quantity()));
        }
        return order;
    }
}
