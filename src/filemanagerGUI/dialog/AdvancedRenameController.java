/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;
import filemanagerLogic.Enums;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import utility.ErrorReport;
import utility.ExtStringUtils;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class AdvancedRenameController extends BaseDialog {

    
@FXML public Tab tbSpecificRename;
@FXML public Tab tbNumerize;
    
@FXML public TextField tfStrReg;
@FXML public TextField tfReplaceWith;
@FXML public TextField tfFilter;
@FXML public TextField tfStartingNumber;

@FXML public TableView table;

@FXML public CheckBox useRegex;
@FXML public CheckBox showFullPath;
@FXML public CheckBox recursive;
@FXML public CheckBox includeFolders;
@FXML public CheckBox showOnlyDifferences;
@FXML public Button buttonApply;


private long startingNumber;
private LinkedList<TableItemObject> tableList;
private ArrayList<ExtFolder> folders;
private ArrayList<ExtFile> files;

public void beforeShow(String title,ArrayList<String> fileList){
    super.beforeShow(title);    
    this.setNumber();
    this.tableList = new LinkedList<>();
    this.files = new ArrayList<>();
    this.folders = new ArrayList<>();
    ArrayList<TableColumn> columns = new ArrayList<>();
    TableColumn<TableItemObject, String> nameCol1 = new TableColumn<>("Current Name");
    TableColumn<TableItemObject, String> nameCol2 = new TableColumn<>("Rename To");
    TableColumn<TableItemObject, String> sizeCol = new TableColumn<>("Size");    
    TableColumn<TableItemObject, String> dateCol = new TableColumn<>("Last Modified");
    nameCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            SimpleStringProperty result;
            if(showFullPath.selectedProperty().get()){
                result = cellData.getValue().path1;
            }else{
                result = cellData.getValue().name1;
            }
            return result;
        }
    });
    nameCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            SimpleStringProperty result;
            if(showFullPath.selectedProperty().get()){
                result = cellData.getValue().path2;
            }else{
                result = cellData.getValue().name2;
            }
            return result;
        }
    });
    sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            return cellData.getValue().size.asString();
        }
    });
    dateCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            return cellData.getValue().date;
        }
    });
    sizeCol.setComparator(ExtFile.COMPARE_SIZE_STRING);
    
    columns.add(nameCol1);
    columns.add(nameCol2);
    columns.add(sizeCol);
    columns.add(dateCol);
    this.table.getColumns().setAll(columns);

    this.table.getItems().addAll(tableList);
    fileList.forEach(file ->{
        ExtFile fileAndPopulate = LocationAPI.getInstance().getFileAndPopulate(file);
        if(fileAndPopulate.getIdentity().equals(Enums.Identity.FOLDER)){
            folders.add((ExtFolder) fileAndPopulate);
        }else{
            files.add(fileAndPopulate);
        }
    });
    updateLists();
}
public void updateLists(){
    
    {
    Iterator<ExtFolder> iterator = folders.iterator();
    while(iterator.hasNext()){
        ExtFolder next = iterator.next();
        if(!Files.exists(next.toPath())){
            iterator.remove();
        }else{
            next.update();
        }
    }}
    
    Iterator<ExtFile> iterator = files.iterator();
    while(iterator.hasNext()){
        ExtFile next = iterator.next();
        if(!Files.exists(next.toPath())){
            iterator.remove();
        }else{
            ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(next.getMapping().getParentLocation());
            folder.update();
        }
    }
    ArrayList<String> array = new ArrayList<>();
    for(ExtFolder folder:folders){
        ArrayList<String> locArray = new ArrayList<>();
        if(recursive.selectedProperty().get()){
            for(ExtFile file:folder.getListRecursive()){
                locArray.add(file.getAbsolutePath());                
            }
        locArray.remove(0);
        }else{
            for(ExtFile file:folder.getFilesCollection()){
                locArray.add(file.getAbsolutePath()); 
            } 
        }
        array.addAll(locArray);
    }
    tableList.clear();
    for(String s:array){
       tableList.add(new TableItemObject(s));
    }
    setTableItems(tableList);
    buttonApply.setDisable(true);
}
public void previewSetting(){
    tableList.clear();
    tableList.addAll(table.getItems());
    if(this.tbNumerize.isSelected()){
        String filter = this.tfFilter.getText();
        setNumber();
        long number = startingNumber;
        
        for(TableItemObject object:this.tableList){          
            try {
                object.newName(ExtStringUtils.parseFilter(object.name1.get(), filter, number++));
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
    }else{
        String strRegex = this.tfStrReg.getText();
        String replacement =""+ this.tfReplaceWith.getText();
        if(useRegex.isSelected()){
            for(TableItemObject object:this.tableList){
                object.newName(ExtStringUtils.parseRegex(object.name1.get(), strRegex, replacement));
            }
        }else{
           for(TableItemObject object:this.tableList){
                object.newName(ExtStringUtils.parseSimple(object.name1.get(), strRegex, replacement)); 
           } 
        }
    }
    setTableItems(applyFilters(tableList));
    
    buttonApply.setDisable(false);
}
@Override
public void update(){
    updateLists();
    
}

public void setNumber(){
    try{
        startingNumber = Integer.parseInt(this.tfStartingNumber.getText());
    }catch(Exception ex){
        startingNumber = 0;
        this.tfStartingNumber.setText(startingNumber+"");
        //reportError(ex);
    }
}
public void apply(){
    for(Object object:table.getItems()){
        TableItemObject ob = (TableItemObject) object;
        try {
            ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileByLocation(new LocationInRoot(ob.path1.get()).getParentLocation());
            String fallback = TaskFactory.resolveAvailableName(parent, ob.name1.get());
            fallback = ExtStringUtils.replaceOnce(fallback, parent.getAbsoluteDirectory(), "");
            TaskFactory.getInstance().renameTo(ob.path1.get(),ob.name2.get(),fallback);
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }
    update();
    Platform.runLater(()->{
        update();
    });
    
}
private static class TableItemObject{
    public SimpleStringProperty path1;
    public SimpleStringProperty name1;
    public SimpleStringProperty path2;
    public SimpleStringProperty name2;
    public SimpleStringProperty date;
    public SimpleLongProperty size;
    public boolean excludeMe;
    public boolean isFolder;
    
    
    public TableItemObject(String s){
        LocationInRoot mapping = LocationAPI.getInstance().getLocationMapping(s);
        ExtFile file = LocationAPI.getInstance().getFileByLocation(mapping);
        this.date = new SimpleStringProperty(file.propertyDate.get());
        this.size = new SimpleLongProperty(file.propertySize.get());
        this.path1 = new SimpleStringProperty(file.getAbsoluteDirectory());
        this.name1 = new SimpleStringProperty(file.propertyName.get());
        this.path2 = new SimpleStringProperty(file.getAbsoluteDirectory());
        this.name2 = new SimpleStringProperty(file.propertyName.get());
        this.isFolder = file.getIdentity().equals(Enums.Identity.FOLDER);
    }
    public void newName(String s){
        String oldName = name2.get();
        name2.set(s);
        int length = oldName.length();
        String newPath = path2.get().substring(0, path2.get().length()-length);
        path2.set(newPath+s);
        //Log.write(path2,"  ",name2);
    }
}
private LinkedList<TableItemObject> applyFilters(LinkedList<TableItemObject> items){
    LinkedList<TableItemObject> list = new LinkedList<>();
    for(TableItemObject object:items){
        if(!this.includeFolders.selectedProperty().get()){
            if(object.isFolder){
                object.excludeMe = true;
            }
        }
        if(this.showOnlyDifferences.selectedProperty().get()){
            if(object.name1.get().equals(object.name2.get())){
                object.excludeMe = true;
            }
        }
        list.add(object);
    }
    return list;
}
private void setTableItems(LinkedList<TableItemObject> items){
    
    table.getItems().clear();
    for(TableItemObject object:items){
        if(!object.excludeMe){
            table.getItems().add(object);
        }
    }
    TableColumn get = (TableColumn) table.getColumns().get(0);
    get.setVisible(false);
    get.setVisible(true);
}    
}
