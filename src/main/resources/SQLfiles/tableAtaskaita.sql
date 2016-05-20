CREATE TABLE labe2219.ATASKAITA (
DarbID VARCHAR(8) not NULL,
Data DATE not NULL,
Laikas TIME not NULL,
Aprasas VARCHAR(200),
PRIMARY KEY (DarbID, Data),
FOREIGN KEY (DarbID) References labe2219.Darbuotojas (DarbID) ON DELETE RESTRICT
)
