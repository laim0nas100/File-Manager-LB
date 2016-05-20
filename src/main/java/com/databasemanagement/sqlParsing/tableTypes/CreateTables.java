/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement.sqlParsing.tableTypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Laimonas Beniušis
 */
public class CreateTables {
    private static final CreateTables INSTANCE = new CreateTables();
    public String[] tables = new String[5];
    public static CreateTables getInstance(){
        return INSTANCE;
    }
    private CreateTables(){
        tables[0] = "PREKE";
        tables[1] = "ETATAS";
        tables[2] = "DARBUOTOJAS";
        tables[3] = "ATASKAITA";
        tables[4] = "UZSAKYMAS";
    }
    public ArrayList<String> getSequentialOrder(){
        ArrayList<String> list = new ArrayList<>();
        list.add(sqlPreke);
        list.add(sqlEtatas);
        list.add(sqlDarbuotojas);
        list.add(sqlAtaskaita);
        list.add(sqlUzsakymas);
        return list;
    }
    
    public final String sqlAtaskaita = "CREATE TABLE ATASKAITA ("
            + "DarbID VARCHAR(8) not NULL REFERENCES DARBUOTOJAS(DarbID),"
            + "Data DATE not NULL,"
            + "Atejo TIME,"
            + "Isejo TIME,"
            + "Aprasas VARCHAR(200), "
            + "PRIMARY KEY (DarbID, Data) )";
    public final String sqlDarbuotojas = "CREATE TABLE DARBUOTOJAS ("
            + "DarbID VARCHAR(8) not NULL,"
            + "Vardes VARCHAR(20) not NULL,"
            + "Pavarde VARCHAR(20) not NULL,"
            + "Pareigos VARCHAR(50) not NULL REFERENCES ETATAS(Pareigos),"
            + "PRIMARY KEY (DarbID) )";
    public final String sqlEtatas = "CREATE TABLE ETATAS ("
            + "Pareigos VARCHAR(50) not NULL,"
            + "IlgisValandomis FLOAT,"
            + "Alga FLOAT,"
            + "PRIMARY KEY (Pareigos) )";
    public final String sqlPreke = "CREATE TABLE PREKE ("
            + "PrekesID VARCHAR(15) not NULL,"
            + "Kaina FLOAT not NULL,"
            + "Aprasas VARCHAR(1000),"
            + "PRIMARY KEY (PrekesID) )";
    public final String sqlUzsakymas = "CREATE TABLE UZSAKYMAS ("
            + "UzsakymoNR VARCHAR(15) not NULL,"
            + "Data DATE not NULL,"
            + "Adresas VARCHAR(200) not NULL,"
            + "Vienetai INTEGER not NULL,"
            + "PrekesID VARCHAR(15) not NULL REFERENCES PREKE(PrekesID),"
            + "Kaina FLOAT not NULL,"
            + "Aprasas VARCHAR(1000),"
            + "AtsakingasID VARCHAR(8) not NULL REFERENCES DARBUOTOJAS(DarbID),"
            + "PRIMARY KEY (UzsakymoNR) )";
}
