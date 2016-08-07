/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

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
    public static class ExtFileTableView{
        
            
        public int sortByColumn;
        public TableView table;
        
        public ExtFileTableView(TableView table){
            defaultValues();
            this.table = table;
        }
        public ExtFileTableView(){
            defaultValues();
        }
        private void defaultValues(){
            sortByColumn = 0;
        }
        
        
    }
    
    public static ExtFileTableView wrapExFileTable(TableView table, Collection<ExtFile> files){
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
        return new ExtFileTableView(table);
    }
}
