package com.databasemanagement;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.databasemanagement.sqlParsing.Commander;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.dalesbred.result.ResultTable;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class AdminWindowController extends BaseController {

    @FXML ListView consoleList;
    @FXML TextField textField;
    private ObservableList<String> list = FXCollections.observableArrayList();
    private ArrayList<String> stringList = new ArrayList<>();
    private LinkedList<String> commandHistory = new LinkedList<>();
    private int commandHistoryIndex = -1;
    @Override
    public void setUp(String title){
        this.title = title;
        consoleList.setItems(list);
        textField.setOnKeyPressed(eh ->{
            KeyCode code = eh.getCode();
            if(code.equals(KeyCode.UP)){
                changeIndex(-1);
                textField.setText(commandHistory.get(commandHistoryIndex));
            }
            if(code.equals(KeyCode.DOWN)){
                changeIndex(1);
                textField.setText(commandHistory.get(commandHistoryIndex));
            }
        });
    }
    @Override
    public void exit() {
        ViewManager.getInstance().closeWindow(title);
    }
    @FXML public void submit() throws SQLException{
        String command = this.textField.getText().trim();
        addLine("$ "+command);
        this.commandHistory.add(command);
        this.commandHistoryIndex = this.commandHistory.size();
        this.textField.clear();
        parseCommand(command);
    }
        private void addLine(String...strings){
        this.list.addAll(Arrays.asList(strings));
    }
    private void changeIndex(int i){
        this.commandHistoryIndex+=i;
        if(commandHistoryIndex>=this.commandHistory.size()){
            commandHistoryIndex = commandHistory.size()-1;
        }
        if(commandHistoryIndex<0){
            commandHistoryIndex = 0;
        }
        
    }
    private String[] removeEmpty(String[] array){
        ArrayList<String> l  = new ArrayList<>();
        for(String s:array){
            if(!s.isEmpty()){
                while((s.charAt(0)==' ')&&(s.length()>0)){
                    s = s.replaceFirst(" ", "");
                }
                l.add(s);
            }
        }
        array = l.toArray(new String[l.size()]);
        return array;
    }
    private String parseCommand(String prefix,String fullCommand){
        fullCommand = fullCommand.replaceFirst(prefix, "");
        String vars = fullCommand.replace("(", "").replace(")", "");
        String[] split = vars.split(",");
        split = removeEmpty(split);
        System.out.println(Arrays.toString(split));
        String result = "";
        if(prefix.equals("fire")){
           result = "DELETE FROM labe2219.darbuotojas WHERE labe2219.darbuotojas.DarbID ='"+split[0]+"'";
        }else if(prefix.equals("hire")){
            String values = "'"+split[0]+"','"+split[1]+"','"+split[2]+"','"+split[3]+"'";
            result = "INSERT INTO labe2219.darbuotojas VALUES ( "+values+" )";
        }else if(prefix.equals("productOut")){
            result = "UPDATE labe2219.preke SET Yra = FALSE WHERE prekesID='"+split[0]+"'";
        }else if(prefix.equals("productNew")){
            result = "INSERT INTO labe2219.darbuotojas VALUES (";
            String values = "'"+split[0]+"',"+split[1];
            if(split.length>2){
                values+=",'"+split[2]+"'";
            }
            result += values+" )";
        }else if(prefix.equals("showReports")){
            if(split.length>0){
                result = "SELECT * from labe2219.ataskaita WHERE data::date ='"+split[0]+"'";
            }else{
                result = "SELECT * from labe2219.ataskaita";
            }
                  
        }else if(prefix.equals("showProducts")){
            result = "SELECT * from labe2219.preke";
        }else if(prefix.equals("showStaff")){
            result = "SELECT * from labe2219.staff";
        }else if(prefix.equals("showOrders")){
            result = "SELECT * from labe2219.uzsakymas";
        }else if(prefix.equals("showStaffOrders")){
            result = "SELECT * from labe2219.DARBUOTOJU_UZSAKYMAI";
        }else if(prefix.equals("showStaffOrderCount")){
            result = "SELECT * from labe2219.DARBUOTOJU_UZIMTUMAS";
        }
        return result;
        
    }
    private void parseCommand(String com) throws SQLException{
        try{
            stringList.clear();
            if(com.startsWith("/")){//execute command
                com = com.substring(1);
                if(com.startsWith("clear")){
                    this.consoleList.getItems().clear();
                }
                if(com.startsWith("help")){
                    stringList.add("Komandos: (parametrai,skliausteliuose,!nebutini)  ");
                    stringList.add("/help - šita komanda");
                    stringList.add("/clear - išvalyti console");
                    stringList.add("/exit - System.exit(0) (Pabaigti darbą ir išjungti)");
                    stringList.add("/quit - išjungti šį langą");
                    stringList.add("fire (darbID) - atlesti darbuotoja");
                    stringList.add("hire (darbID,vardas,pavarde,pareigos)");
                    stringList.add("productNew (prekesID,kaina,!aprasas)");
                    stringList.add("productOut (prekesID)");
                    stringList.add("showReports (!data)");
                    stringList.add("showProducts");
                    stringList.add("showStaff");
                    stringList.add("showOrders");
                    stringList.add("showStaffOrders");
                    stringList.add("showStaffOrderCount");
                    stringList.add("==[Query]");
                    stringList.add("=[Statement]");
                    this.addLine(stringList.toArray(new String[stringList.size()]));       
                }
                if(com.startsWith("exit")){
                    System.exit(0);
                }
                if(com.startsWith("quit")){
                    this.exit();
                }
            }else if(com.startsWith("==")){ //execute query

                ResultTable table = Commander.getInstance().getTable(com.substring(2));
                ViewManager.getInstance().newTableWindow(table);
            }else if(com.startsWith("=")){ //execute statement
                Statement state = Commander.getInstance().con.createStatement();
                state.execute(com.substring(1));

            }else{//execute predefined

                if(com.startsWith("showReports")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showReports",com))); 
                }else if(com.startsWith("fire")){
                    Commander.getInstance().db.update(parseCommand("fire",com));
                }else if(com.startsWith("hire")){
                    Commander.getInstance().db.update(parseCommand("hire",com));
                }else if(com.startsWith("productOut")){
                    Commander.getInstance().db.update(parseCommand("productOut",com));
                }else if(com.startsWith("productNew")){
                    Commander.getInstance().db.update(parseCommand("productNew",com));
                }else if(com.startsWith("showProducts")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showProducts",com)));
                }else if(com.startsWith("showStaffOrders")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showStaffOrders",com)));           
                }else if(com.startsWith("showStaffOrderCount")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showStaffOrderCount",com)));              
                }else if(com.startsWith("showStaff")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showStaff",com)));           
                }else if(com.startsWith("showOrders")){
                    ViewManager.getInstance().newTableWindow(Commander.getInstance().getTable(parseCommand("showOrders",com)));
                }

            }
        }catch(Exception ex){
            System.out.println(ex);
        }
    }

    
}
