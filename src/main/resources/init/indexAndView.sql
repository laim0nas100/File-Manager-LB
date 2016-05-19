;
CREATE VIEW labe2219.STAFF AS SELECT etatas.pareigos, count(CASE WHEN labe2219.etatas.pareigos = labe2219.darbuotojas.pareigos THEN 1 END) AS "Darbuotoju kiekis" from labe2219.etatas,labe2219.darbuotojas GROUP BY etatas.pareigos;
;
;
CREATE VIEW labe2219.DARBUOTOJU_UZSAKYMAI AS SELECT darbuotojas.vardas,darbuotojas.pavarde, uzsakymas.uzsakymoNR,uzsakymas.uzbaigtas FROM labe2219.darbuotojas,labe2219.uzsakymas WHERE(labe2219.uzsakymas.atsakingasID = labe2219.darbuotojas.darbID);