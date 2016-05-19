/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement.sqlParsing;

import com.databasemanagement.sqlParsing.tableTypes.CreateTables;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.dalesbred.Database;
import org.dalesbred.result.ResultTable;

/**
 *
 * @author Laimonas Beniušis
 */
public class Commander {

    public static ArrayList<String> readFromFile(String URL) throws FileNotFoundException, IOException {
        ArrayList<String> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(URL),"UTF8"));
        reader.lines().forEach((String ln) -> {
            int indexOf = ln.indexOf(";");      //Find comment start
            if(indexOf!=0){
                if(indexOf==-1){
                        list.add(ln.trim());   
                    }else{
                        list.add(ln.substring(0,indexOf).trim());
                    }
                }
        });
        reader.close();
        return list;
    }
    private String DB_URL;
    private String USER;
    private String PASS;
    private static final Commander INSTANCE = new Commander();
    public Database db;
    public Connection con;
    private Commander(){
        DB_URL ="not defined";
        PASS = "not definded";
        USER ="not defined";
        
    }
    public void establishConnection(String...strings){
        try{
            DB_URL = strings[0];
            USER = strings[1];
            PASS = strings[2];
            System.out.println("Connecting: \n"+DB_URL+"\n"+USER+"\n"+PASS);
            con = DriverManager.getConnection(DB_URL,USER,PASS);
            db = Database.forUrlAndCredentials(DB_URL, USER, PASS);
            if(con == null){
                System.out.println("Connection Failed");
            }else{
                System.out.println("Connected");
            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }
    public ResultTable getTable(String query){
        return db.findTable(query);
    }
    public void insertTo(String tableName,String values){
        String s = "INSERT INTO "+tableName+" VALUES ( "+values.trim()+" )";
        System.out.println(s);
        db.update(s);
    }
    public static Commander getInstance(){
        return INSTANCE;
    }
    public static String getCurrentTime(){
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }
    public static String getCurrentDate(){
        return new SimpleDateFormat("YYYY-MM-dd").format(Calendar.getInstance().getTime());
    }
}
