    CREATE OR REPLACE FUNCTION hasUnfinishedWork() RETURNS "trigger" AS $$
    DECLARE ammount INTEGER;
    BEGIN
        SELECT labe2219.darbuotoju_uzimtumas.Nebaigtu INTO ammount FROM labe2219.DARBUOTOJU_UZIMTUMAS WHERE labe2219.DARBUOTOJU_UZIMTUMAS.DarbID = OLD.darbID ;
        if (ammount != 0) THEN 
            RAISE EXCEPTION 'Has unfinished work';
        END IF;
        RETURN OLD;        
    END;
    $$
    LANGUAGE plpgsql;
    --
    CREATE TRIGGER atleidimui BEFORE DELETE ON labe2219.darbuotojas
        FOR EACH ROW EXECUTE PROCEDURE hasUnfinishedWork();