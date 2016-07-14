/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import java.util.Collection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 *
 * @author Laimonas Beniušis
 */
public class CosmeticsFX {
    public static class ExtFileTableView extends javafx.scene.control.TableView{
        public int sortByColumn;
        
        private static void defaultValues(){
            
        }
        public ExtFileTableView(javafx.scene.control.TableView table){
            defaultValues();
        }
        public ExtFileTableView(){
            defaultValues();
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
