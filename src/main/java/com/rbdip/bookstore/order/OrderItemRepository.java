package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
    @Query("select count(i) > 0 from OrderItem i, Order o where i.orderId = o.id"
            + " and i.product.id = :productId and o.customer.firstName = :firstName"
            + " and o.customer.lastName = :lastName")
    boolean purchasedBy(Long productId, String firstName, String lastName);
}
