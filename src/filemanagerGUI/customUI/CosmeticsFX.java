/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import LibraryLB.Log;
import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class CosmeticsFX {
    public static class MenuTree{
        //comment
        //more comment
        HashMap<String,MenuTree> leafs;
        LinkedList<String> myMapping;
        MenuItem data;
        public boolean hidden;
        public MenuTree(MenuItem data,String...mapping){
            leafs = new HashMap<>();
            myMapping = new LinkedList<>();
            myMapping.addAll(Arrays.asList(mapping));
            this.data = data;

        }
        public void addMenuItem(MenuItem data,String...mapping){
            MenuTree Leaf = new MenuTree(data,mapping);
            MenuTree correctLeaf = this;
            LinkedList<String> getToLeaf = new LinkedList<>();
            getToLeaf.addAll(Leaf.myMapping);
            String removeLast = getToLeaf.removeLast();
            for(String s:getToLeaf){
                correctLeaf = correctLeaf.leafs.get(s);
            }
            correctLeaf.leafs.put(removeLast, Leaf);

        }
        @Override
        public String toString(){
            String s = "";
            s+=this.myMapping.toString()+"item:"+data.getText()+"\n";
            for(MenuTree mp:this.leafs.values()){
                s+=mp.toString();
            }
            return s;
        }
        public ContextMenu constructContextMenu(){
            ContextMenu cm = new ContextMenu();

            for(MenuTree mapping:this.leafs.values()){
                MenuItem item = constructMenuFromTree(mapping);
                if(item!= null){
                    cm.getItems().add(item);
                }

            }
            return cm;
        }
        public void setHidden(boolean hidden,String...mapping){
            MenuTree correctLeaf = this;
            LinkedList<String> getToLeaf = new LinkedList<>();
            getToLeaf.addAll(Arrays.asList(mapping));
            for(String s:getToLeaf){
                correctLeaf = correctLeaf.leafs.get(s);
            }
            correctLeaf.hidden = hidden;

        }
        private MenuItem constructMenuFromTree(MenuTree leaf){
            if(leaf.hidden){
                return null;
            }
            if(leaf.leafs.isEmpty()){
                return leaf.data;
            }else{
                Menu menu = (Menu) leaf.data;
                for(MenuTree mapping:leaf.leafs.values()){
                    MenuItem item = constructMenuFromTree(mapping);
                    if(item!= null){
                        menu.getItems().add(item);
                    }
                }
                return menu;
            }
        }

    }
    public static class ExtTableView{
        public final long resizeTimeout = 500;
        public SortType sortType;
        public TableColumn sortColumn;
        public int sortByColumn;
        public TableView table;
        public SimpleBooleanProperty recentlyResized;
        public ExtTableView(TableView table){
           
            this.table = table; 
            defaultValues();
        }
        public ExtTableView(){
            defaultValues();
        }
        private void defaultValues(){
            sortByColumn = 0;
            recentlyResized = new SimpleBooleanProperty();
            
        }
        public void prepareChangeListeners(){
            try{
                    table.getColumns().forEach(col ->{
                   
                    TableColumn c = (TableColumn) col;
                    changeListener(c);
                });
            }catch(Exception e){
                ErrorReport.report(e);
            }
        }
        public void changeListener(final TableColumn listerColumn) {
        listerColumn.widthProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
//                System.out.print(listerColumn.getText() + "  ");
//                System.out.println(t1);
                recentlyResized.set(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(resizeTimeout);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        recentlyResized.set(false);
                        
                    }
                }).start();
            }
        });
    }
        public void saveSortPrefereces(){
            
            if (!table.getSortOrder().isEmpty()) {
                sortColumn = (TableColumn) table.getSortOrder().get(0);
                sortType = sortColumn.getSortType();
            }
        }
        public void setSortPreferences(){
            if (sortColumn != null) {
                table.getSortOrder().add(sortColumn);
                sortColumn.setSortType(sortType);
                sortColumn.setSortable(true);
            }
        }
        public void updateContents(Collection collection){
            table.setItems(FXCollections.observableArrayList(collection));
            //Workaround to update table
            TableColumn get = (TableColumn) table.getColumns().get(0);
            get.setVisible(false);
            get.setVisible(true);
        }
        public void updateContentsAndSort(Collection collection){
            saveSortPrefereces();
            updateContents(collection);
            setSortPreferences();
        }

        
        
        
    }
    
    public static ExtTableView wrapExFileTable(TableView table, Collection<ExtFile> files){
        TableColumn<ExtFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                return cellData.getValue().propertyName;
            }
        });
        
        TableColumn<ExtFile, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtFile, String> cellData) -> cellData.getValue().propertyType);

        TableColumn<ExtFile, String> sizeCol = new TableColumn<>("Size Auto");
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                if(cellData.getValue().getIdentity().equals(Enums.Identity.FOLDER)){
                    return new SimpleStringProperty("");
                }else{
                    return cellData.getValue().propertySizeAuto;
            }
     
        }});
        
        sizeCol.setComparator(ExtFile.COMPARE_SIZE_STRING);
        TableColumn<ExtFile, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtFile, String> cellData) -> cellData.getValue().propertyDate);
        table.getColumns().clear();
        table.getColumns().addAll(nameCol,typeCol,sizeCol,dateCol);
        return new ExtTableView(table);
    }
}
