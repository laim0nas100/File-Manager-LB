/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.databasemanagement;



import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import org.dalesbred.result.ResultTable;
import org.dalesbred.result.ResultTable.ResultRow;

/**
 *
 * @author lemmin
 */
public class ViewManager {
    private final String WINDOW_TITLE = "Pagrindinis Langas ";
    private final String WORKER_TITLE ="Darbuotojas ";
    private final String ADMIN_TITLE ="Admin ";
    private final String TABLE_TITLE="Lentele ";
    private static final ViewManager INSTANCE = new ViewManager();
    private ViewManager(){
        this.windows = new HashMap<>();
    };
    public HashMap<String,Frame> windows;

    public static ViewManager getInstance(){
        return INSTANCE;
    }
    private int findSmallestAvailable(HashMap<String,Frame> map,String title){
        int i =1;
        while(true){
            if(map.containsKey(title + i)){
                i++;
            }else{
                return i;
            }
        }
    }
    
// WINDOW ACTIONS
    public void newWindow(){
        try {
            int index = findSmallestAvailable(windows,WINDOW_TITLE);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mainWindow.fxml"));
            Parent root = loader.load();
            FXMLController controller = loader.<FXMLController>getController();
            Stage stage = new Stage();
            stage.setTitle(WINDOW_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest((WindowEvent we) -> {
                System.exit(0);
                controller.exit();
                
            });
            Frame frame = new Frame(stage,controller);
            windows.put(frame.getTitle(),frame);
            controller.setUp(stage.getTitle());
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    public void newWorker(String workerID){
        String newTitle = WORKER_TITLE+" "+workerID;
        
        if(this.windows.containsKey(newTitle)){
                return;
        }
        try {
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Worker.fxml"));
           
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(newTitle);
            stage.setScene(new Scene(root));
            WorkerController controller = loader.<WorkerController>getController();
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });  
            controller.setUp(newTitle,workerID);
            Frame frame = new Frame(stage,controller);
            windows.put(frame.getTitle(),frame);
            stage.show();
            System.out.println("<"+newTitle+">");
        } catch (Exception ex) {
           ex.printStackTrace();
        }
        
    }
    
    public void newAdminWindow(){
        try {
            int index = findSmallestAvailable(windows,ADMIN_TITLE);
            
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminWindow.fxml"));
            Parent root = loader.load();
            AdminWindowController controller = loader.<AdminWindowController>getController();
            Stage stage = new Stage();
            stage.setTitle(ADMIN_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });
            Frame frame = new Frame(stage,controller);
            windows.put(frame.getTitle(),frame);
            stage.show();
            controller.setUp(stage.getTitle());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    public void newTableWindow(ResultTable table){
        try {
            int index = findSmallestAvailable(windows,TABLE_TITLE);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TableWindow.fxml"));
            Parent root = loader.load();
            TableWindowController controller = loader.<TableWindowController>getController();
            Stage stage = new Stage();
            stage.setTitle(TABLE_TITLE+index);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest((WindowEvent we) -> {
                controller.exit();
            });
            Frame frame = new Frame(stage,controller);
            windows.put(frame.getTitle(),frame);
            stage.show();
            controller.setUp(stage.getTitle(),table);
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    public void closeWindow(String title){
        windows.get(title).getStage().close();
        windows.remove(title);
        if(windows.isEmpty()){
            Platform.exit();
        }
    }
    public void closeAllWindows(){
        String[] keys = windows.keySet().toArray(new String[0]);
        for(String s:keys){
            closeWindow(s);
        }
    }
    public void wrapResultTable(ResultTable resultTable,TableView table){
        table.getColumns().clear();
        table.getItems().clear();
        ObservableList<ObservableList> data = FXCollections.observableArrayList();
        int i=0;
        for(String s:resultTable.getColumnNames()){
            final int i2 = i;
            TableColumn col = new TableColumn(s);
            col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList,String>,ObservableValue<String>>(){                   
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {                                                                                             
                        return new SimpleStringProperty(param.getValue().get(i2).toString());                       
                    }                   
                });
            table.getColumns().add(col);
            i++;
        }
        for(ResultRow row:resultTable.getRows()){
            List<Object> asList = row.asList();
            ObservableList<String> r = FXCollections.observableArrayList();
            
            for(Object o:asList){
                String s = "NULL";
                try{
                    s = o.toString();
                }catch(Exception ex){}
                r.add(s);
                //System.out.println("Adding "+o.toString());
            }
            data.add(r);
        }
        table.setItems(data);
    }

}
