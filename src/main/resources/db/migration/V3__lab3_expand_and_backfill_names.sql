ALTER TABLE customers ADD COLUMN first_name VARCHAR(255), ADD COLUMN last_name VARCHAR(255);

-- Триггер поддерживает обе версии приложения во время перехода:
-- старая пишет full_name, новая пишет first_name и last_name.
CREATE FUNCTION sync_customer_names() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.full_name IS NOT NULL THEN
            NEW.first_name := split_part(NEW.full_name, ' ', 1);
            NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') = 0 THEN ''
                ELSE substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) END;
        ELSE
            NEW.full_name := NEW.first_name || CASE WHEN NEW.last_name = '' THEN '' ELSE ' ' || NEW.last_name END;
        END IF;
    ELSIF NEW.full_name IS DISTINCT FROM OLD.full_name THEN
        NEW.first_name := split_part(NEW.full_name, ' ', 1);
        NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') = 0 THEN ''
            ELSE substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) END;
    ELSE
        NEW.full_name := NEW.first_name || CASE WHEN NEW.last_name = '' THEN '' ELSE ' ' || NEW.last_name END;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER customer_names_sync BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_names();

UPDATE customers SET first_name = split_part(full_name, ' ', 1),
    last_name = CASE WHEN strpos(full_name, ' ') = 0 THEN ''
        ELSE substr(full_name, strpos(full_name, ' ') + 1) END;
ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL, ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE customers ADD CONSTRAINT customers_split_contact_key
    UNIQUE NULLS NOT DISTINCT (first_name, last_name, address, phone);
