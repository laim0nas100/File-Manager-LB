CREATE TABLE labe2219.PREKE (
PrekesID VARCHAR(15) not NULL,
Kaina double precision not NULL CHECK (Kaina >0),
Aprasas VARCHAR(1000),
Yra boolean DEFAULT TRUE,
PRIMARY KEY (PrekesID) 
)
