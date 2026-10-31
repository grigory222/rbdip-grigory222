package com.rbdip.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BookstoreApiTest extends AbstractIntegrationTest {
    @Test
    void listsProductsAndReviewsWithAndWithoutComments() {
        Long product = product();
        assertThat(restTemplate.getForEntity("/products", List.class).getBody()).hasSize(1);
        String path = "/products/" + product + "/reviews";
        assertThat(restTemplate.postForEntity(path, Map.of("rating", 5), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        restTemplate.postForEntity(path, Map.of("authorName", "Anna", "rating", 4, "comment", "Good"), Map.class);
        List<?> reviews = restTemplate.getForEntity(path, List.class).getBody();
        assertThat(reviews).hasSize(2);
        assertThat(((Map<?, ?>) reviews.get(0)).get("authorName")).isEqualTo("anonymous");
        assertThat(((Map<?, ?>) reviews.get(0)).get("comment")).isEqualTo("");
        assertThat(((Map<?, ?>) reviews.get(1)).get("comment")).isEqualTo("Good");
    }

    @Test
    void reusesCustomerAndDefaultsQuantity() {
        Long product = product();
        var request = order("Ivan Petrov", "Address", List.of(new CreateOrderRequest.Item(product, null)));
        for (int i = 0; i < 2; i++) {
            assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(1);
        List<?> orders = restTemplate.getForEntity("/orders", List.class).getBody();
        assertThat(orders).hasSize(2);
        List<?> items = (List<?>) ((Map<?, ?>) orders.get(0)).get("items");
        assertThat(((Map<?, ?>) items.get(0)).get("quantity")).isEqualTo(1);
    }

    @Test
    void invalidRequestsReturnBadRequestAndDoNotSaveOrders() {
        Long product = product();
        var validItems = List.of(new CreateOrderRequest.Item(product, 1));
        List<CreateOrderRequest> requests = List.of(
                order(null, "Address", validItems), order(" ", "Address", validItems),
                order("Ivan", null, validItems), order("Ivan", " ", validItems),
                order("Ivan", "Address", null), order("Ivan", "Address", List.of()),
                order("Ivan", "Address", List.of(new CreateOrderRequest.Item(product, 0))),
                order("Ivan", "Address", List.of(new CreateOrderRequest.Item(product, -1))),
                order("Ivan", "Address", List.of(new CreateOrderRequest.Item(Long.MAX_VALUE, 1))));
        for (var request : requests) {
            assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    private CreateOrderRequest order(String name, String address, List<CreateOrderRequest.Item> items) {
        return new CreateOrderRequest(name, address, null, null, null, items);
    }

    private Long product() {
        Map<?, ?> body = restTemplate.postForEntity("/products",
                Map.of("name", "Refactoring", "price", "40.00", "description", "Book"), Map.class).getBody();
        return ((Number) body.get("id")).longValue();
    }
}
