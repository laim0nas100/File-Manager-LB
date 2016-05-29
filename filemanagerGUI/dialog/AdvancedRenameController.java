/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;
import filemanagerGUI.MainController;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import utility.Log;

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
@FXML public Button buttonApply;


private long startingNumber;
private ObservableList<TableItemObject> tableList;
private ArrayList<ExtFolder> folders;
private ArrayList<ExtFile> files;

public void setUp(String title,ArrayList<String> fileList){
    super.setUp(title);    
    this.setNumber();
    this.tableList = FXCollections.observableArrayList();
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
    sizeCol.setComparator(MainController.compareSizeAsString);
    
    columns.add(nameCol1);
    columns.add(nameCol2);
    columns.add(sizeCol);
    columns.add(dateCol);
    this.table.getColumns().setAll(columns);

    this.table.setItems(tableList);
    fileList.forEach(file ->{
        ExtFile fileAndPopulate = LocationAPI.getInstance().getFileAndPopulate(file);
        if(fileAndPopulate.getIdentity().equals("folder")){
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
                if(this.includeFolders.selectedProperty().get()){
                    locArray.add(file.getAbsolutePath());
                }else{
                    if(!file.getIdentity().equals("folder")){
                        locArray.add(file.getAbsolutePath());
                    }
                }
            }
            if(this.includeFolders.selectedProperty().get()){
                locArray.remove(0);
            }
        }else{
            for(ExtFile file:folder.getFilesCollection()){
                if(this.includeFolders.selectedProperty().get()){
                    locArray.add(file.getAbsolutePath());
                }else{
                    if(!file.getIdentity().equals("folder")){
                        locArray.add(file.getAbsolutePath());
                    }
                }
            } 
        }
        array.addAll(locArray);
    }
    tableList.clear();
    for(String s:array){
       tableList.add(new TableItemObject(s));
    }

    buttonApply.setDisable(true);
}
public void previewSetting(){
    if(this.tbNumerize.isSelected()){
        String filter = this.tfFilter.getText();
        setNumber();
        
        long number = startingNumber;
        for(Object s:table.getItems()){
            try {
                TableItemObject object = (TableItemObject) s;
                object.newName(ExtStringUtils.parseFilter(object.name1.get(), filter, number++));
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
    }else{
        String strRegex = this.tfStrReg.getText();
        String replacement =""+ this.tfReplaceWith.getText();
        if(useRegex.isSelected()){
            for(Object s:table.getItems()){
                TableItemObject object = (TableItemObject) s;
                object.newName(ExtStringUtils.parseRegex(object.name1.get(), strRegex, replacement)); 
            }
        }else{
           for(Object s:table.getItems()){
                TableItemObject object = (TableItemObject) s;
                object.newName(ExtStringUtils.parseSimple(object.name1.get(), strRegex, replacement)); 
           } 
        }
    }
    buttonApply.setDisable(false);
}
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
            TaskFactory.getInstance().renameTo(ob.path1.get(),ob.name2.get());
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }
    update();
}
private static class TableItemObject{
    public SimpleStringProperty path1;
    public SimpleStringProperty name1;
    public SimpleStringProperty path2;
    public SimpleStringProperty name2;
    public SimpleStringProperty date;
    public SimpleLongProperty size;
    
    
    public TableItemObject(String s){
        LocationInRoot mapping = LocationAPI.getInstance().getLocationMapping(s);
        this.date = new SimpleStringProperty(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(LocationAPI.getInstance().getFileByLocation(mapping).lastModified()))));
        this.size = new SimpleLongProperty(LocationAPI.getInstance().getFileByLocation(mapping).propertySize.get());
        this.path1 = new SimpleStringProperty(s);
        this.name1 = new SimpleStringProperty(mapping.getName());
        this.path2 = new SimpleStringProperty(s);
        this.name2 = new SimpleStringProperty(mapping.getName());
    }
    public void newName(String s){
        String oldName = name2.get();
        name2.set(s);
        int length = oldName.length();
        String newPath = path2.get().substring(0, path2.get().length()-length);
        path2.set(newPath+s);
        Log.write(path2,"  ",name2);
    }
}
    
}
