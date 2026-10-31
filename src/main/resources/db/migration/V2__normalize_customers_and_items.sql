CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    UNIQUE NULLS NOT DISTINCT (full_name, address, phone)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT REFERENCES customers(id);
UPDATE orders o SET customer_id = c.id FROM customers c
WHERE o.customer_full_name = c.full_name
  AND o.customer_address IS NOT DISTINCT FROM c.address
  AND o.customer_phone IS NOT DISTINCT FROM c.phone;
ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE orders DROP COLUMN customer_full_name, DROP COLUMN customer_address, DROP COLUMN customer_phone;
CREATE INDEX orders_customer_idx ON orders(customer_id);

-- В старых позициях нет product_id. Сопоставляем по обоим сохранённым
-- атрибутам; если товар уже удалён/изменён, сохраняем исторический вариант.
INSERT INTO products (name, price)
SELECT DISTINCT i.product_name, i.product_price FROM order_items i
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = i.product_name AND p.price = i.product_price);
ALTER TABLE order_items ADD COLUMN product_id BIGINT REFERENCES products(id);
UPDATE order_items i SET product_id = (
    SELECT min(p.id) FROM products p WHERE p.name = i.product_name AND p.price = i.product_price
);
ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE order_items DROP COLUMN product_name, DROP COLUMN product_price;
CREATE INDEX order_items_order_idx ON order_items(order_id);
CREATE INDEX order_items_product_idx ON order_items(product_id);
