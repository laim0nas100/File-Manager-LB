;Virtualios lenteles (VIEWS)
CREATE VIEW labe2219.STAFF AS SELECT etatas.pareigos, count(CASE WHEN labe2219.etatas.pareigos = labe2219.darbuotojas.pareigos THEN 1 END) AS "Darbuotoju kiekis" from labe2219.etatas,labe2219.darbuotojas GROUP BY etatas.pareigos;
;
CREATE VIEW labe2219.DARBUOTOJU_UZSAKYMAI AS SELECT darbuotojas.vardas,darbuotojas.pavarde, uzsakymas.uzsakymoNR FROM labe2219.darbuotojas INNER JOIN labe2219.uzsakymas ON labe2219.uzsakymas.atsakingasID = labe2219.darbuotojas.darbID;
;
;Index-ai
CREATE UNIQUE INDEX labe2219.Ataskaita ON labe2219.Ataskaita(labe2219.Ataskaita.DarbID, labe2219.Ataskaita.Data);
CREATE INDEX labe2219.Uzsakymas ON labe2219.Uzsakymas(labe2219.uzsakymas.uzsakymoNR);
;
;Trigger-iai
