package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderValidator validator;
    private final OrderWriter writer;
    private final OrderNotifier notifier;
    private final PricingCalculator pricingCalculator;

    public OrderService(OrderValidator validator, OrderWriter writer,
            OrderNotifier notifier, PricingCalculator pricingCalculator) {
        this.validator = validator;
        this.writer = writer;
        this.notifier = notifier;
        this.pricingCalculator = pricingCalculator;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<ResolvedItem> items = validator.validate(request);
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(ResolvedItem::priceLine).toList(),
                request.customerType() == null ? "regular" : request.customerType(), request.couponCode());
        Order order = writer.save(request, items);
        notifier.confirm(request.customerFullName(), order.getId(), total);
        return order;
    }
}
