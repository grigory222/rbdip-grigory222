-- Только после перевода всех экземпляров приложения на first_name/last_name.
DROP TRIGGER customer_names_sync ON customers;
DROP FUNCTION sync_customer_names();
ALTER TABLE customers DROP COLUMN full_name;
