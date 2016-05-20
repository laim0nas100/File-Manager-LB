/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement;

import com.databasemanagement.sqlParsing.Commander;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import org.dalesbred.result.ResultTable;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class WorkerController extends BaseController {
    private String sessionStart;
    private String time;
    private String workerID;
    @FXML public ComboBox combobox;
    @FXML public TextArea text;
    ResultTable table;
    public void setUp(String title,String workerID) throws SQLException{
        super.setUp(title);
        this.workerID = workerID;
        sessionStart = Commander.getCurrentDate();
        time = Commander.getCurrentTime();
        
        try{
            CallableStatement prepareCall = Commander.getInstance().con.prepareCall("SELECT * FROM labe2219.ataskaita WHERE (ataskaita.DarbID = '"+this.workerID +"') AND (ataskaita.Data ='"+this.sessionStart+"')");
            ResultSet query = prepareCall.executeQuery();
            query.next();
            String string = query.getString(4);
            this.text.setText(string);
        }catch(Exception e){
            System.out.println(e.getMessage());
            this.text.setText("Nepildyta");
        }
        Commander.getInstance().db.update("DELETE FROM ONLY labe2219.ataskaita WHERE (DarbID = '"+this.workerID +"') AND (Data ='"+this.sessionStart+"')");
        String value = "'"+workerID+"','"+sessionStart+"','"+time+"','"+this.text.getText()+"'";
        Commander.getInstance().insertTo("labe2219.ataskaita",value);
        table = Commander.getInstance().getTable("SELECT labe2219.uzsakymas.uzsakymoNr from labe2219.uzsakymas WHERE (uzsakymas.atsakingasID = '"+
                this.workerID+"' ) AND (NOT uzsakymas.uzbaigtas)");
        for(ResultTable.ResultRow row:table.getRows()){
            combobox.getItems().add(row.get(0));
        }
    }
    @Override
    public void exit(){
        ViewManager.getInstance().closeWindow(this.title);
    }
    @FXML public void submit() throws SQLException{
        
        PreparedStatement ps = Commander.getInstance().con
                .prepareStatement("UPDATE labe2219.ATASKAITA SET aprasas = ? WHERE Data ='"+this.sessionStart+"' AND DarbID ='"+this.workerID+"'");
        ps.setString(1, this.text.getText());
        ps.executeUpdate();
        ps.close();
    }
    @FXML public void markAsCompleted(){
        String selectedItem = (String) this.combobox.getSelectionModel().getSelectedItem();
        Commander.getInstance().db.update("UPDATE labe2219.uzsakymas SET uzbaigtas = true WHERE uzsakymas.uzsakymoNR = '"+selectedItem+"'");
        fillOrdersCombobox();
    }
    public ObservableList<String> getUnfinishedOrders(){
        ObservableList<String> orderList = FXCollections.observableArrayList();
        ResultTable table2 = Commander.getInstance().getTable("SELECT labe2219.uzsakymas.uzsakymoNR FROM labe2219.uzsakymas WHERE (labe2219.uzsakymas.atsakingasID = '"+this.workerID+"' AND NOT labe2219.uzsakymas.uzbaigtas)");
        table2.getRows().forEach(row ->{
        orderList.add(row.get(0).toString());
        });
        return orderList;
    }
    @FXML public void fillOrdersCombobox(){
        this.combobox.getItems().clear();
        this.combobox.getItems().addAll(getUnfinishedOrders());
    }
    
}
