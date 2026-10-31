package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=3")
@DirtiesContext
class MigrationApiLoadTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate http;

    @Test
    void newApplicationKeepsServingReadsAndWritesDuringContract() throws Exception {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        http.getRestTemplate().setRequestFactory(factory);
        Map<?, ?> product = http.postForEntity("/products",
                Map.of("name", "Book", "price", "10"), Map.class).getBody();
        long productId = ((Number) product.get("id")).longValue();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger requests = new AtomicInteger();
        var errors = new CopyOnWriteArrayList<Throwable>();
        var started = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            final int worker = i;
            pool.submit(() -> {
                int sequence = 0;
                try {
                    do {
                        checkRequests(productId, "Customer" + worker + " " + sequence++);
                        requests.addAndGet(2);
                        started.countDown();
                    } while (running.get());
                } catch (Throwable failure) {
                    errors.add(failure);
                    started.countDown();
                }
            });
        }
        try {
            assertThat(started.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(errors).isEmpty();
            Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .load().migrate();
            checkRequests(productId, "After Contract");
        } finally {
            running.set(false);
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(errors).isEmpty();
        assertThat(requests.get()).isGreaterThanOrEqualTo(4);
    }

    private void checkRequests(long productId, String name) {
        var request = new CreateOrderRequest(name, "Address", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        assertThat(http.postForEntity("/orders", request, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(http.getForEntity("/orders", List.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
