-- Virtualios lenteles (VIEWS)
CREATE OR REPLACE VIEW labe2219.STAFF AS SELECT etatas.pareigos, COUNT(CASE WHEN labe2219.etatas.pareigos = labe2219.darbuotojas.pareigos THEN 1 END) AS "Darbuotoju kiekis" from labe2219.etatas,labe2219.darbuotojas GROUP BY etatas.pareigos
CREATE OR REPLACE VIEW labe2219.DARBUOTOJU_UZSAKYMAI AS SELECT darbuotojas.DarbID,darbuotojas.vardas,darbuotojas.pavarde,uzsakymas.uzsakymoNR FROM labe2219.darbuotojas INNER JOIN labe2219.uzsakymas ON labe2219.uzsakymas.atsakingasID = labe2219.darbuotojas.darbID
CREATE OR REPLACE VIEW labe2219.DARBUOTOJU_UZIMTUMAS AS SELECT darbuotojas.DarbID,darbuotojas.vardas,darbuotojas.pavarde, COUNT(CASE WHEN labe2219.darbuotojas.darbID = labe2219.uzsakymas.atsakingasID THEN 1 END) AS "viso",COUNT(CASE WHEN (labe2219.darbuotojas.darbID = labe2219.uzsakymas.atsakingasID AND NOT labe2219.uzsakymas.uzbaigtas) THEN 1 END) AS "nebaigtu" FROM labe2219.darbuotojas,labe2219.uzsakymas GROUP BY labe2219.darbuotojas.darbID
--
--Index-ai
CREATE UNIQUE INDEX ataskaitos ON labe2219.Ataskaita(DarbID, Data)
CREATE INDEX uzsakymai ON labe2219.Uzsakymas(uzsakymoNR)