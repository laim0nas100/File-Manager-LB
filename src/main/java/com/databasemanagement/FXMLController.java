package com.databasemanagement;

import com.databasemanagement.sqlParsing.Commander;
import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
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
            try{
            Commander.getInstance().db.update("DROP TABLE "+tables[i] +" CASCADE");
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }
    @FXML public void createAdditionalThings() throws IOException{
        ArrayList<String> list = Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\additionalThings.sql");
        list.forEach(s ->{
            try{
            Commander.getInstance().db.update(s);
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        });
        list.clear();
        String com1="";
        String com2 ="";
        list = Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\trigger1.sql");
        for(String s:list){
            com1+=s;
        }
        list.clear();
        list = Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\trigger2.sql");
        for(String s:list){
            com2+=s;
        }
        try{
            Commander.getInstance().db.update(com1);
        }catch(Exception e){
            System.out.println(e);
        }
        try{
            Commander.getInstance().db.update(com2);
        }catch(Exception e){
            System.out.println(e);
        }
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
            this.fillProducts();
            this.fillProductsCombobox();
            this.fillWorkersCombobox();
            
        });
        
    }
    @FXML public void fillProducts(){
        Platform.runLater(()->{
            ViewManager.getInstance().wrapResultTable(Commander.getInstance().getTable("SELECT * FROM labe2219.preke"), changeableTable2);
        });
        
    }
    @FXML public void fillWorkersCombobox(){
            this.comboboxWorkers.getItems().clear();
            this.comboboxWorkers.getItems().addAll(getWorkersList());
            this.comboboxWorkers.getSelectionModel().selectFirst();
    }
    @FXML public void fillProductsCombobox(){
            this.comboboxProducts.getItems().clear();
            ObservableList<String> productList = FXCollections.observableArrayList();
            ResultTable table = Commander.getInstance().getTable("SELECT labe2219.preke.PrekesID FROM labe2219.preke");
            table.getRows().forEach(row ->{
                productList.add(row.get(0).toString());
            });
            
            this.comboboxProducts.getItems().addAll(productList);
            this.comboboxProducts.getSelectionModel().selectFirst();
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
//        int amm = 0;
//        amm = Commander.getInstance().getTable("SELECT * FROM labe2219.UZSAKYMAS").getRowCount();
//        amm++;
        
        int amm2 = -1;
        try{
            amm2 = Integer.parseInt(this.ammount.getText().trim());
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        ArrayList<String> workers = new ArrayList<>();
        workers.addAll(this.getWorkersList());
        String randomWorker;
        int random;
        boolean t = true;
        do{
            random = (int) ((Math.random()*workers.size()) % workers.size());
            randomWorker = workers.get(random);
            try{
                CallableStatement prepareCall = Commander.getInstance().con.prepareCall("SELECT * FROM labe2219.darbuotojas WHERE (darbuotojas.darbID= '"+randomWorker +"')");
                ResultSet query = prepareCall.executeQuery();
                query.next();
                String eligibleForWork = query.getString(4);
                if(!eligibleForWork.startsWith("direktorius")){
                    t = false;
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
                return;
            }
        }while(t);
        String product = this.comboboxProducts.getSelectionModel().getSelectedItem().toString();
        Double price;
        try{
            CallableStatement prepareCall = Commander.getInstance().con.prepareCall("SELECT * FROM labe2219.preke WHERE (preke.prekesID = '"+product +"')");
            ResultSet query = prepareCall.executeQuery();
            query.next();
            price = query.getDouble(2);
            price *=amm2;
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        try{
            PreparedStatement ps = Commander.getInstance().con.prepareStatement("INSERT INTO labe2219.uzsakymas VALUES(?,?,?,?,?,?)");
            int i = 1;
            //ps.setString(i++, String.valueOf(amm));
            ps.setTimestamp(i, java.sql.Timestamp.valueOf(LocalDateTime.now()));i++;
            ps.setString(i, this.address.getText());i++;
            ps.setInt(i, amm2);i++;
            ps.setString(i, product);i++;
            ps.setDouble(i, price);i++;
            ps.setString(i, randomWorker);i++;
            
            ps.execute();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    public ObservableList<String> getWorkersList(){
        ObservableList<String> workerList = FXCollections.observableArrayList();
        ResultTable table = Commander.getInstance().getTable("SELECT labe2219.darbuotojas.darbID FROM labe2219.darbuotojas");
        table.getRows().forEach(row ->{
        workerList.add(row.get(0).toString());
        });
        return workerList;
    }

}
