/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;


import LibraryLB.Threads.ExtTask;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.snapshots.Entry;
import filemanagerLogic.snapshots.ExtEntry;
import filemanagerLogic.snapshots.Snapshot;
import filemanagerLogic.snapshots.SnapshotAPI;
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
import LibraryLB.Log;
import LibraryLB.Threads.TaskExecutor;
import LibraryLB.Threads.TimeoutTask;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import filemanagerLogic.Enums;
import javafx.scene.control.MenuItem;
import utility.ErrorReport;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DirSyncController extends BaseController {

    @FXML public TextField directory0;
    @FXML public TextField directory1;
    @FXML public Text status0;
    @FXML public Text status1;
    @FXML public Text status;
    @FXML public TableView table;
    @FXML public DatePicker datePicker;
    @FXML public CheckBox checkShowAbsolutePath;
    @FXML public CheckBox checkPrioritizeBigger;
    @FXML public CheckBox checkShowOnlyDifferences;
    @FXML public CheckBox checkIgnoreFolderDate;
    @FXML public CheckBox checkNoDelete;
    @FXML public CheckBox checkNoCopy;
    @FXML public CheckBox checkIgnoreModified;
    @FXML public CheckBox checkDeleteFirst;
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
    private ExtPath file0;
    private ExtPath file1;
    private ObservableList<TableColumn<ExtEntry,String>> tableColumns;

    private TimeoutTask directoryCheckTask = new TimeoutTask(1000,100,()->{
        Platform.runLater(()->{
            checkDirs();
        });
    });
    
    
    public static final Comparator<ExtEntry> cmpAsc = new Comparator<ExtEntry>() {
        @Override
        public int compare(ExtEntry f1, ExtEntry f2) {
            return f1.relativePath.compareToIgnoreCase(f2.relativePath);
        }
    };

    
    
    @Override
    public void beforeShow(String title){
    Platform.runLater(()->{
        
        this.directoryCheckTask.addOnUpdate(() ->{
            this.btnLoad.setDisable(true);
            this.btnCompare.setDisable(true);
            this.btnSync.setDisable(true);
        });
        this.btnLoad.setDisable(true);
        this.btnCompare.setDisable(true);
        this.btnSync.setDisable(true);
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

        checkIgnoreFolderDate.setSelected(true);
        checkShowOnlyDifferences.setSelected(true);
        tableColumns.add(new TableColumn<>("Path"));
        tableColumns.get(0).setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtEntry, String> cellData) {
                String path = cellData.getValue().relativePath;
                SimpleStringProperty string = new SimpleStringProperty(path);
                if(checkShowAbsolutePath.selectedProperty().get()){
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
        MenuTree menuTree = new MenuTree(null);
        for(int i=0;i<5;i++){
            final int action = i;
            MenuItem item = new MenuItem("Set " +ExtEntry.getActionDescription(action));
            item.setOnAction(eh ->{
                ObservableList selectedItems = table.getSelectionModel().getSelectedItems();
                for(Object ob:selectedItems){
                    ExtEntry entry = (ExtEntry) ob;
                    entry.setAction(action);
                }
            });
            menuTree.addMenuItem(item, item.getText());
        }
        this.table.setContextMenu(menuTree.constructContextMenu());

        
    });
    }
    @Override
    public void afterShow() {
        super.afterShow();
        
//        this.directoryCheckTask.doneCheck.bind(this.isGone);
        
        this.directoryCheckTask.addOnUpdate(()->{
            this.btnCompare.setDisable(true);
        });
        this.directory0.textProperty().addListener(onChange ->{
            this.directoryCheckTask.update();
        });
        this.directory1.textProperty().addListener(onChange ->{
            this.directoryCheckTask.update();
        });
//        new Thread(this.directoryCheckTask).start();
        
                
        
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
        cond0 = file0.getIdentity().equals(Enums.Identity.FOLDER);

        file1 = LocationAPI.getInstance().getFileAndPopulate(text1);
        cond1 = file1.getIdentity().equals(Enums.Identity.FOLDER);

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
    public void setDirs() throws Exception{
        if(!file0.isVirtual.get()){
            directory0.setText(file0.getAbsoluteDirectory());
        }else{
            throw new Exception("Bad directory setup");
        }
        if(!file1.isVirtual.get()){
            directory1.setText(file1.getAbsoluteDirectory());
        }else{
            throw new Exception("Bad directory setup");
        }
    }
    public void load(){
        try {
            setDirs();
        } catch (Exception ex) {
            ErrorReport.report(ex);
            return;
        }
            
        snapshot0 = SnapshotAPI.getEmptySnapshot();
        snapshot1 = SnapshotAPI.getEmptySnapshot();
        this.status.textProperty().set("Populating directories:\n");
        this.btnSync.setDisable(true);
        this.btnCompare.setDisable(true);
        if(cond0&&cond1){           
            Task<Snapshot> task0 = TaskFactory.getInstance().snapshotCreateTask(file0.getAbsolutePath());
            Task<Snapshot> task1 = TaskFactory.getInstance().snapshotCreateTask(file1.getAbsolutePath());
            task0.setOnSucceeded(eh ->{                  
                snapshot0 = task0.getValue();
                status.setText(status.getText().concat(snapshot0.folderCreatedFrom+"\n"));
            });
            task1.setOnSucceeded(eh ->{
                snapshot1 = task1.getValue();                
                status.setText(status.getText().concat(snapshot1.folderCreatedFrom+"\n"));

            });
            
            TaskExecutor executor = new TaskExecutor(2,5);
            executor.addTask(task0);
            executor.addTask(task1);
            executor.neverStop = false;
            executor.setOnSucceeded(eh ->{
                btnCompare.setDisable(false);
            });
            new Thread(executor).start();
        }       
    }
    
    public void compare(){
        Runnable r = () ->{
            ObservableList sortOrder = table.getSortOrder();
            
            this.status.textProperty().set("Comparing");
            Long date = Instant.now().toEpochMilli();
            try{
                date = datePicker.getValue().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
                Log.print(date);
            }catch(Exception e){}
            result = SnapshotAPI.compareSnapshots(snapshot0, snapshot1);
            //Log.writeln(snapshot0,snapshot1);
            if(checkShowOnlyDifferences.selectedProperty().get()){
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
                    if(checkIgnoreFolderDate.selectedProperty().get()){
                        if(next.isFolder){
                            if(next.isModified){
                                next.isModified = false;
                                remove = true;
                            }
                        }
                    }
                    if(checkIgnoreModified.selectedProperty().get()){
                        if(next.isModified){
                            remove = true;
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
                            if(entry.isModified && checkIgnoreModified.selectedProperty().not().get()){
                                if(this.checkPrioritizeBigger.selectedProperty().get()){
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
                            entry.setAction(4);
                        }else if(entry.isNew){  
                            entry.setAction(2);
                        }else{
                            if(entry.isModified && checkIgnoreModified.selectedProperty().not().get()){
                                entry.setAction(2);
                            }
                        }
                        break;
                    }
                    case(2):{//B dominant
                        if(entry.isMissing){
                            entry.setAction(1);
                        }else if(entry.isNew){
                            entry.setAction(3);
                        }else{
                            if(entry.isModified && checkIgnoreModified.selectedProperty().not().get()){
                                entry.setAction(1);
                            }
                        }
                        break;
                    }
                }
                int actionType = entry.actionType.get();
                if((actionType == 3 || actionType == 4)&& checkNoDelete.selectedProperty().get()){
                   entry.setAction(0);
                }else if((actionType == 1 || actionType == 2)&& checkNoCopy.selectedProperty().get()){
                   entry.setAction(0);
                }
            }
            
            
            Platform.runLater(()->{
                table.setItems(list);
                table.getSortOrder().setAll(sortOrder);
                table.sort();
                this.status.textProperty().set("Done");
                this.btnSync.setDisable(false);
            });
        };
        new Thread(r).start();
        
        
    }
    
    public void synchronize(){
        this.btnSync.setDisable(true);
        Log.print("Syncronize!");
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
        
        
        if(checkDeleteFirst.selectedProperty().get()){
            list.addAll(0, listDelete);
        }else{
            list.addAll(listDelete);
        }
        for(ExtEntry en:list){
            Log.print(en.toString());
        }
        
        task = TaskFactory.getInstance().syncronizeTask(this.snapshot0.folderCreatedFrom,this.snapshot1.folderCreatedFrom,list);

        task.setTaskDescription("Synchronization: "+"\n"+
                "Source:"+this.snapshot0.folderCreatedFrom +"\n"+
                "Compared:"+this.snapshot1.folderCreatedFrom);
        
        ViewManager.getInstance().newProgressDialog(task);
        

    }

    @Override
    public void update(){ 
    }
    @Override
    public void exit(){
//        this.directoryCheckTask.shutdown();
        super.exit();
    }
}
