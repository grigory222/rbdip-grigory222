package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MigrationDataTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        flyway(null).clean();
        var source = new PGSimpleDataSource();
        source.setUrl(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void normalizationPreservesExistingOrdersAndHistoricalProducts() {
        flyway("1").migrate();
        jdbc.update("INSERT INTO products(name, price) VALUES ('Book', 20), ('Book', 20)");
        jdbc.update("INSERT INTO orders(customer_full_name, customer_address) VALUES ('Anna Ivanova', 'Address'),"
                + " ('Anna Ivanova', 'Address')");
        jdbc.update("INSERT INTO order_items(order_id, product_name, product_price, quantity)"
                + " VALUES (1, 'Book', 20, 2), (2, 'Old book', 15, 3)");
        flyway("2").migrate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT c.full_name FROM orders o JOIN customers c ON c.id=o.customer_id",
                String.class)).containsExactly("Anna Ivanova", "Anna Ivanova");
        assertThat(jdbc.queryForList("SELECT p.name FROM order_items i JOIN products p ON p.id=i.product_id"
                + " ORDER BY i.id", String.class)).containsExactly("Book", "Old book");
        assertThat(jdbc.queryForObject("SELECT sum(p.price*i.quantity) FROM order_items i"
                + " JOIN products p ON p.id=i.product_id", java.math.BigDecimal.class))
                .isEqualByComparingTo("85");
    }

    @Test
    void bothApplicationVersionsCanWriteNamesUntilContract() {
        flyway("2").migrate();
        jdbc.update("INSERT INTO customers(full_name) VALUES ('Anna Maria Ivanova'), ('Cher')");
        flyway("3").migrate();
        assertThat(jdbc.queryForList("SELECT last_name FROM customers ORDER BY id", String.class))
                .containsExactly("Maria Ivanova", "");
        jdbc.update("INSERT INTO customers(full_name) VALUES ('Old Client')");
        assertThat(jdbc.queryForObject("SELECT first_name FROM customers WHERE full_name='Old Client'",
                String.class)).isEqualTo("Old");
        jdbc.update("INSERT INTO customers(first_name,last_name) VALUES ('New','Client')");
        assertThat(jdbc.queryForObject("SELECT full_name FROM customers WHERE first_name='New'",
                String.class)).isEqualTo("New Client");
        jdbc.update("UPDATE customers SET full_name='Changed Old' WHERE first_name='Old'");
        assertThat(jdbc.queryForObject("SELECT last_name FROM customers WHERE first_name='Changed'",
                String.class)).isEqualTo("Old");
        jdbc.update("UPDATE customers SET last_name='Updated' WHERE first_name='New'");
        assertThat(jdbc.queryForObject("SELECT full_name FROM customers WHERE first_name='New'",
                String.class)).isEqualTo("New Updated");
        flyway(null).migrate();
        jdbc.update("INSERT INTO customers(first_name,last_name) VALUES ('Final','Client')");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(5);
    }

    private Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword()).cleanDisabled(false);
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }
}
