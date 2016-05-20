    CREATE OR REPLACE FUNCTION isInStock() RETURNS "trigger" AS $$
    DECLARE inStock boolean;
    BEGIN
        SELECT labe2219.preke.yra INTO inStock FROM labe2219.preke WHERE labe2219.preke.prekesID = NEW.prekesID ;
        if inStock != TRUE THEN 
            RAISE EXCEPTION 'Not in stock';
        END IF;
        RETURN NEW;        
    END;
    $$
    LANGUAGE plpgsql;
    --
    CREATE TRIGGER uzsakymui BEFORE INSERT ON labe2219.uzsakymas 
        FOR EACH ROW EXECUTE PROCEDURE isInStock();