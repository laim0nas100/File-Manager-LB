CREATE TABLE labe2219.UZSAKYMAS (

Data TIMESTAMP not NULL,
Adresas VARCHAR(100) not NULL,
Vienetai INTEGER not NULL CHECK(Vienetai > 0),
PrekesID VARCHAR(15) not NULL,
PilnaKaina double precision not NULL CHECK(PilnaKaina > 0),
AtsakingasID VARCHAR(8) not NULL,
Uzbaigtas BOOLEAN DEFAULT FALSE,
UzsakymoNR SERIAL NOT NULL,
PRIMARY KEY (UzsakymoNR), 
FOREIGN KEY (AtsakingasID) References labe2219.darbuotojas(DarbID) ON DELETE RESTRICT,
FOREIGN KEY (PrekesID) References labe2219.preke(PrekesID) ON DELETE RESTRICT
 )
