package com.databasemanagement;

import com.databasemanagement.sqlParsing.Commander;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.dalesbred.result.ResultTable;

public class FXMLController extends BaseController {
    
    @FXML public TextField address;
    @FXML public TextField ammount;
    
    @FXML public Tab tabOrders;
    @FXML public Tab tabProducts;
    @FXML public Tab tabLogIn;
    @FXML public Tab tabMain;
    @FXML public TabPane tabPane;
    @FXML public AnchorPane tableAnchor;
    @FXML public ComboBox comboboxWorkers;
    @FXML public TableView changeableTable1;
    @FXML public TableView changeableTable2;
    @FXML public ComboBox comboboxProducts;
    @Override
    public void setUp(String title){
        super.setUp(title);
        
            
        
    }
    @Override
    public void exit(){
        ViewManager.getInstance().closeWindow(title);
    }
    @FXML public void dropTables() throws IOException{
        ArrayList<String> tablesURL = new ArrayList<>();
        tablesURL.addAll(Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\tablesOrder.txt"));
        String[] tables = new String[tablesURL.size()];
        tablesURL.toArray(tables);
        for(int i=tables.length-1;i>=0;i--){
            Commander.getInstance().db.update("DROP TABLE "+tables[i] +" CASCADE");
        }
    }
    @FXML public void createAdditionalThings() throws IOException{
        ArrayList<String> list = Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\indexAndView.sql");
        list.forEach(s ->{
            Commander.getInstance().db.update(s);
        });
    }
    @FXML public void createTables() throws IOException{
        ArrayList<String> tablesURL = new ArrayList<>();
        tablesURL.addAll(Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\tablesID.txt"));
        String parentFolder = tablesURL.remove(0);
        for(String s:tablesURL){
            ArrayList<String> buffer = Commander.readFromFile(parentFolder+s);
            String query="";
            for(String st:buffer){
                query+=st;
            }
            Commander.getInstance().db.update(query);
        }
    }
    @FXML public void fillSampleData() throws IOException{
        Platform.runLater(()->{
            ArrayList<String> buffer = new ArrayList<>();
            File folder = new File("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\SQLfiles\\sampleData");
            File[] listFiles = folder.listFiles();
            for(File f:listFiles){
                try {
                    buffer = Commander.readFromFile(f.getAbsolutePath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                String tableName = buffer.remove(0);
                for(String value:buffer){
                    Commander.getInstance().insertTo(tableName, value);
                }
            }
            
        });
        
    }
    @FXML void test(){
        MainApp.queryList.forEach(q->{
            Commander.getInstance().db.update(q);
        });
    }
    @FXML public void fillProducts(){
        ViewManager.getInstance().wrapResultTable(Commander.getInstance().getTable("SELECT * FROM labe2219.preke"), changeableTable2);
        
    }
    @FXML public void fillWorkersCombobox(){
        Platform.runLater(()->{
            ObservableList<String> workerList = FXCollections.observableArrayList();
            ResultTable table = Commander.getInstance().getTable("SELECT labe2219.darbuotojas.darbID FROM labe2219.darbuotojas");
            table.getRows().forEach(row ->{
                workerList.add(row.get(0).toString());
            });
            this.comboboxWorkers.setItems(workerList);
        });
    }
    @FXML public void fillProductsCombobox(){
        Platform.runLater(()->{ 
            ObservableList<String> productList = FXCollections.observableArrayList();
            ResultTable table = Commander.getInstance().getTable("SELECT labe2219.preke.PrekesID FROM labe2219.preke");
            table.getRows().forEach(row ->{
                productList.add(row.get(0).toString());
            });
            this.comboboxProducts.setItems(productList);
        });
    }
    @FXML public void logIn(){
        String selectedItem = this.comboboxWorkers.getSelectionModel().getSelectedItem().toString();
        ViewManager.getInstance().newWorker(selectedItem);
    }
    @FXML public void newAdminWindow(){
        ViewManager.getInstance().newAdminWindow();
    }
    @FXML public void commitOrder() throws SQLException{
        System.out.println("Order UP!");
        int amm = 0;
        amm = Commander.getInstance().getTable("SELECT * FROM labe2219.UZSAKYMAS").getRowCount();
        System.out.println("Passed 1");
        amm++;
        int amm2 = 2;
        try{
            amm2 = Integer.parseInt(this.ammount.getText().trim());
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        System.out.println(amm+"  "+amm2);
        ArrayList<String> workers = new ArrayList<>();
        workers.addAll(this.comboboxWorkers.getItems());
        int random = (int) ((Math.random()*workers.size()) % workers.size());
        String randomWorker = workers.get(random);
        String product = this.comboboxProducts.getSelectionModel().getSelectedItem().toString();
        try{
            PreparedStatement ps = Commander.getInstance().con.prepareStatement("INSERT INTO labe2219.uzsakymas VALUES(?,?,?,?,?,NULL,NULL,?,FALSE)");
            ps.setString(1, ""+amm);
            ps.setTimestamp(2, java.sql.Timestamp.from(Instant.now()));
            ps.setString(3, this.address.getText());
            ps.setInt(4, amm2);
            ps.setString(5, product);
            ps.setString(6, randomWorker);
            ps.execute();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
             
        String values = "'"+amm+"','"+Commander.getCurrentDate()+" "+Commander.getCurrentTime()+"','"+
                "Vilnius"+"',"+amm2+",'"+product+"',"+"NULL,"+"NULL,'"+randomWorker+"',FALSE";
        //Commander.getInstance().insertTo("labe2219.UZSAKYMAS", values);
    }
    
    
    
}
