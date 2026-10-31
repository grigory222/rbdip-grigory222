package com.rbdip.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import com.rbdip.bookstore.purchase.PurchaseHistory;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class OrderQueriesTest extends AbstractIntegrationTest {
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PurchaseHistory purchases;

    @Test
    void fetchesDistinctCustomersAndProductsInConstantNumberOfQueries() {
        for (int i = 0; i < 8; i++) {
            long product = product("Book" + i);
            var request = new CreateOrderRequest("Customer " + i, "Address", null, null, null,
                    List.of(new CreateOrderRequest.Item(product, 1), new CreateOrderRequest.Item(product, 2)));
            assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        }
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var response = restTemplate.getForEntity("/orders", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        assertThat(response.getBody()).hasSize(8);
        for (Object value : response.getBody()) {
            Map<?, ?> order = (Map<?, ?>) value;
            assertThat((List<?>) order.get("items")).hasSize(2);
            assertThat(order.get("customerFullName").toString()).startsWith("Customer ");
        }
    }

    @Test
    void purchaseContractMatchesBothAuthorAndProduct() {
        long bought = product("Bought");
        long other = product("Other");
        var request = new CreateOrderRequest("Anna Maria Ivanova", "Address", null, null, null,
                List.of(new CreateOrderRequest.Item(bought, 1)));
        restTemplate.postForEntity("/orders", request, Map.class);
        assertThat(purchases.hasPurchased(bought, "Anna Maria Ivanova")).isTrue();
        assertThat(purchases.hasPurchased(bought, "Someone Else")).isFalse();
        assertThat(purchases.hasPurchased(other, "Anna Maria Ivanova")).isFalse();
        assertThat(purchases.hasPurchased(bought, null)).isFalse();
        assertThat(purchases.hasPurchased(bought, " ")).isFalse();
    }

    private long product(String name) {
        Map<?, ?> response = restTemplate.postForEntity("/products",
                Map.of("name", name, "price", "10"), Map.class).getBody();
        return ((Number) response.get("id")).longValue();
    }
}
