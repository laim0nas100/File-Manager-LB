/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;


import filemanagerGUI.ViewManager;
import filemanagerLogic.ExtTask;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.snapshots.Entry;
import filemanagerLogic.snapshots.ExtEntry;
import filemanagerLogic.snapshots.Snapshot;
import filemanagerLogic.snapshots.SnapshotAPI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.util.Callback;
import utility.ExtStringUtils;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DirSyncController extends BaseDialog {

    @FXML public TextField directory0;
    @FXML public TextField directory1;
    @FXML public Text status0;
    @FXML public Text status1;
    @FXML public Text status;
    @FXML public TableView table;
    @FXML public DatePicker datePicker;
    @FXML public CheckBox checkShowAbsolutePath;
    @FXML public CheckBox checkNoDelete;
    @FXML public CheckBox checkPrioritizeBigger;
    @FXML public CheckBox checkShowOnlyDifferences;
    @FXML public CheckBox checkDeleteFirst;
    @FXML public CheckBox checkIgnoreFolderDate;
    @FXML public Button btnLoad;
    @FXML public Button btnCompare;
    @FXML public Button btnSync;
    @FXML public ComboBox syncMode;
    @FXML public ComboBox dateMode;

    private boolean cond0;
    private boolean cond1;
    private Snapshot snapshot0;
    private Snapshot snapshot1;
    private Snapshot result;
    private ExtFile file0;
    private ExtFile file1;
    private final SimpleBooleanProperty showAbsolutePath = new SimpleBooleanProperty();
    private final SimpleBooleanProperty prioritizeBigger = new SimpleBooleanProperty();
    private final SimpleBooleanProperty noDelete = new SimpleBooleanProperty();
    private final SimpleBooleanProperty onlyDifferences = new SimpleBooleanProperty();
    private final SimpleBooleanProperty deleteFirst = new SimpleBooleanProperty();
    private final SimpleBooleanProperty ignoreFolderDate = new SimpleBooleanProperty();
    private ObservableList<TableColumn<ExtEntry,String>> tableColumns; 
    
    public static final Comparator<ExtEntry> cmpAsc = new Comparator<ExtEntry>() {
        @Override
        public int compare(ExtEntry f1, ExtEntry f2) {
            return f1.relativePath.compareToIgnoreCase(f2.relativePath);
        }
    };

    
    
    @Override
    public void beforeShow(String title){
    super.beforeShow(title);
    Platform.runLater(()->{
        
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Bidirectional");
        options.add("Make B like A");
        options.add("Make A like B");
        syncMode.getItems().setAll(options);
        syncMode.getSelectionModel().selectFirst();
        
        
        ObservableList<String> dateModeOptions = FXCollections.observableArrayList();
        dateModeOptions.add("Ignore After");
        dateModeOptions.add("Ignore Before");
        dateMode.getItems().setAll(dateModeOptions);
        dateMode.getSelectionModel().selectFirst();
            
        Locale.setDefault(Locale.ROOT);
        datePicker.setValue(LocalDate.now().plusDays(1));
           
        tableColumns = table.getColumns();

        showAbsolutePath.bind(checkShowAbsolutePath.selectedProperty());
        noDelete.bind(checkNoDelete.selectedProperty());
        prioritizeBigger.bind(checkPrioritizeBigger.selectedProperty());
        onlyDifferences.bind(checkShowOnlyDifferences.selectedProperty());
        deleteFirst.bind(checkDeleteFirst.selectedProperty());
        ignoreFolderDate.bind(checkIgnoreFolderDate.selectedProperty());
        checkIgnoreFolderDate.setSelected(true);
        checkShowOnlyDifferences.setSelected(true);
        tableColumns.add(new TableColumn<>("Path"));
        tableColumns.get(0).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                String path =cellData.getValue().relativePath;
                SimpleStringProperty string = new SimpleStringProperty(path);
                if(showAbsolutePath.get()){
                    path = cellData.getValue().absolutePath;
                    string.set(path);
                }
                return string;
            }
        });
        tableColumns.add(new TableColumn<>("Condition"));
        tableColumns.get(1).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                
                SimpleStringProperty string = new SimpleStringProperty("No changes");
                String s="";
                    if(cellData.getValue().isNew){
                        s+=" new";
                    }else if(cellData.getValue().isMissing){
                        s+=" missing";
                    }else if(cellData.getValue().isModified){
                        s+=" modified";
                        if(cellData.getValue().isOlder){
                            s+= " older";
                        }else{
                            s+= " not older";
                        }
                        if(cellData.getValue().isBigger){
                            s+= " bigger";
                        }else{
                            s+= " not bigger";
                        }
                    }
                string.set(s);
                return string;
            }
        });
        tableColumns.add(new TableColumn<>("Last Modified"));
        tableColumns.get(2).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                SimpleStringProperty string = new SimpleStringProperty(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(cellData.getValue().lastModified))));
                return string;
            }
        });
        tableColumns.add(new TableColumn<>("Action"));
        tableColumns.get(3).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                
                return cellData.getValue().action;
            }
        });
        tableColumns.add(new TableColumn<>("Sync Complete"));
        tableColumns.get(4).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                return cellData.getValue().actionCompleted.asString();
            }
        });

        
    });
    }
    public void checkDirs(){
        btnSync.setDisable(true);
        btnLoad.setDisable(true);    
        String text0 = directory0.getText();
        String text1 = directory1.getText();
        cond0 = false;
        cond1 = false;
        status0.setText("BAD");
        status1.setText("BAD");
        
        file0 = LocationAPI.getInstance().getFileAndPopulate(text0);
        cond0 = (!file0.isAbsoluteRoot()&&file0.getIdentity().equals("folder"));

        file1 = LocationAPI.getInstance().getFileAndPopulate(text1);
        cond1 = (!file1.isAbsoluteRoot()&&file1.getIdentity().equals("folder"));

        if(cond0){
            status0.setText("OK");
        }
        if(cond1){
            status1.setText("OK");
        }
        if(cond1&&cond0){
            btnLoad.setDisable(false);
        }
    }
    public void setDirs(){
        if(!file0.isAbsoluteRoot()){
            directory0.setText(file0.getAbsoluteDirectory());
        }else{
            directory0.setText("");
        }
        if(!file1.isAbsoluteRoot()){
            directory1.setText(file1.getAbsoluteDirectory());
        }else{
            directory1.setText("");
        }
    }
    public void load(){
            setDirs();
            snapshot0 = SnapshotAPI.getEmptySnapshot();
            snapshot1 = SnapshotAPI.getEmptySnapshot();
            this.status.textProperty().set("Loading A");
            this.btnSync.setDisable(true);
            this.btnCompare.setDisable(true);
            if(cond0&&cond1){           
                Task<Snapshot> task0 = TaskFactory.getInstance().snapshotCreateTask((directory0.getText()));
                Task<Snapshot> task1 = TaskFactory.getInstance().snapshotCreateTask((directory1.getText()));
                task0.setOnSucceeded(eh ->{                  
                    snapshot0 = task0.getValue();
                    //Log.writeln(snapshot0);
                    this.status.textProperty().set("Loading B");
                    new Thread(task1).start();
                });
                task1.setOnSucceeded(eh ->{
                    snapshot1 = task1.getValue();
                    //Log.writeln(snapshot1);
                    btnCompare.setDisable(false);
                    this.status.textProperty().set("Done");
                });
                new Thread(task0).start();
            }       
    }
    
    public void compare(){
            ObservableList sortOrder = table.getSortOrder();
            
            this.status.textProperty().set("Comparing");
            Long date = Instant.now().toEpochMilli();

            try{
                date = datePicker.getValue().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                Log.writeln(date);
            }catch(Exception e){
            }
            result = SnapshotAPI.compareSnapshots(snapshot0, snapshot1);
            //Log.writeln(snapshot0,snapshot1);
            if(onlyDifferences.get()){
               result = SnapshotAPI.getOnlyDifferences(result);
            }
            
            int modeDate = dateMode.getSelectionModel().getSelectedIndex();
            ObservableList<ExtEntry> list = FXCollections.observableArrayList();
            Iterator<Entry> iterator = result.map.values().iterator();
            while(iterator.hasNext()){
                Entry next = iterator.next();
                boolean remove = false;
                
                if(modeDate == 0 && next.lastModified>date){
                   remove = true;
                }else if(modeDate == 1 && next.lastModified<date){
                    remove = true;
                }else{
                    if(ignoreFolderDate.get()){
                        if(next.isFolder){
                            if(next.isModified){
                                next.isModified = false;
                                remove = true;
                            }
                        }
                    }
                }
                if(!remove){
                    list.add(new ExtEntry(next));
                }
                
            }
            //Action Types
            //0 - no Action
            //1 - Missing file, copy here
            //2 - Replacable file
            //3 - New file, copy this
            //4 - Replacement file, copy this
            //5 - Delete this
            int mode = syncMode.getSelectionModel().getSelectedIndex();
            for(ExtEntry entry:list){
                entry.setAction(0);
                switch(mode){
                    case(0):{//Bidirectional
                        if(entry.isMissing){
                            entry.setAction(1);
                        }else if(entry.isNew){
                            entry.setAction(2);
                        }else{
                            if(entry.isModified){
                                if(this.prioritizeBigger.get()){
                                    if(entry.isBigger){
                                        entry.setAction(2);
                                    }else{
                                        entry.setAction(1);
                                    }
                                }else{
                                    if(entry.isOlder){
                                        entry.setAction(1);
                                    }else{
                                        entry.setAction(2);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case(1):{//A dominant
                        if(entry.isMissing){
                            if(!noDelete.get()){
                                entry.setAction(4);
                            }
                        }else if(entry.isNew){
                            entry.setAction(2);
                        }else{
                            if(entry.isModified){
                                entry.setAction(2);
                            }
                        }
                        break;
                    }
                    case(2):{//B dominant
                        if(entry.isMissing){
                            
                                entry.setAction(1);
                           
                        }else if(entry.isNew){
                            if(!noDelete.get()){
                                entry.setAction(3);
                            }
                        }else{
                            if(entry.isModified){
                                entry.setAction(1);
                            }
                        }
                        break;
                    }
                }
            }
            
            
        Platform.runLater(()->{
            table.setItems(list);
            table.getSortOrder().setAll(sortOrder);
            table.sort();
            this.status.textProperty().set("Done");
            this.btnSync.setDisable(false);
        });
    }
    
    public void synchronize(){
        this.btnSync.setDisable(true);
        Log.writeln("Syncronize!");
        ArrayList<ExtEntry> list = new ArrayList<>();
        ArrayList<ExtEntry> listDelete = new ArrayList<>();
        table.sort();
        for(Object object:table.getItems()){
            ExtEntry entry = (ExtEntry) object;
            if(entry.actionType.get()>2){
                listDelete.add(entry);
            }else{
                list.add(entry);
            }     
        }
        listDelete.sort(cmpAsc.reversed());
        list.sort(cmpAsc);
        ExtTask task;
        
        
        if(deleteFirst.get()){
            list.addAll(0, listDelete);
        }else{
            list.addAll(listDelete);
        }
        for(ExtEntry en:list){
            Log.write(en.toString());
        }
        task = TaskFactory.getInstance().syncronizeTask(snapshot0.folderCreatedFrom, snapshot1.folderCreatedFrom, list);

        task.setTaskDescription("Synchronization");
        task.childTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                status.textProperty().set("Synchronization");
                Log.writeln("Child task invoked");
                for(Object ob:table.getItems()){
                    ExtEntry entry = (ExtEntry) ob;
                    if(!entry.actionCompleted.get()){
                        result.reEvalueateFolder(entry.relativePath, null);
                    }
                    if(!entry.isModified){
                        if(snapshot1.folderCreatedFrom.equalsIgnoreCase(ExtStringUtils.replaceOnce(entry.absolutePath, snapshot1.folderCreatedFrom, ""))){
                            Files.setLastModifiedTime(Paths.get(entry.absolutePath), Files.getLastModifiedTime(Paths.get(snapshot0.folderCreatedFrom+entry.relativePath)));
                        }else{
                            Files.setLastModifiedTime(Paths.get(entry.absolutePath), Files.getLastModifiedTime(Paths.get(snapshot1.folderCreatedFrom+entry.relativePath)));                           
                        }
                    }
                    
                }
                btnCompare.setDisable(true);
                status.textProperty().set("Done");
                return null;
            }
        };
        ViewManager.getInstance().newProgressDialog(task);
        

    }
}
