/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.ManagingClass;
import filemanagerLogic.TaskFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.util.Callback;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import static filemanagerGUI.FileManagerLB.errorLog;
import static filemanagerGUI.FileManagerLB.links;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.fileStructure.ExtLink;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import utility.DesktopApi;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.Finder;
import LibraryLB.Log;
import static filemanagerGUI.FileManagerLB.ArtificialRoot;
import filemanagerGUI.customUI.FileAddressField;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.DATA_SIZE;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Collection;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import utility.ExtStringUtils;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends BaseController{

    
    @FXML public CheckMenuItem autoClose;
    @FXML public SplitPane splitPane;
    
    @FXML public TableView tableView;
    private ArrayList< TableColumn<ExtFile, String>> columns;
    private ObservableList<ExtFile> selectedList = FXCollections.observableArrayList();
    
    
    
    @FXML public CheckBox useRegex;
    @FXML public Label itemCount;
    @FXML public ListView searchView;
    @FXML public TextField searchField;
    @FXML public Text searchStatus;
    
    @FXML public ListView markedView;
    @FXML public Text markedSize;
    
    @FXML public ListView linkView;
    @FXML public ListView errorView;
    
    @FXML public TextField currentDirText;
    @FXML public Button buttonPrev;
    @FXML public Button buttonParent;
    @FXML public Button buttonForw;
    @FXML public Button buttonGo;
    
    @FXML public TextField snapshotLoadField;
    @FXML public TextField snapshotCreateField;
    @FXML public Text snapshotTextDate;
    @FXML public Text snapshotTextFolder;
    @FXML public ListView snapshotView;
    
    @FXML public Menu menuSizeUnits;
    @FXML public MenuItem miAdvancedRename;
    
    @FXML public MenuItem menuItemAbout;
    @FXML public MenuItem menuItemTest;
    
    
    private ContextMenu tableContextMenu;
    private ContextMenu markedContextMenu;
    private ContextMenu tableDragContextMenu;
    private ContextMenu searchContextMenu;
    private ContextMenu linksContextMenu;
    private ContextMenu errorContextMenu;
    private MenuItem[] contextMenuItems;
    private Menu submenuMarked;
    private Menu submenuCreate;
    private Menu submenuSelectSearch;
    private Menu submenuSelectTable;
    
    
    private FileAddressField fileAddress;
    private ManagingClass MC;
    private Finder finder;
    private DATA_SIZE unitSize;
    private SimpleStringProperty propertyUnitSizeName;
    private SimpleLongProperty propertyUnitSize;
    private SimpleBooleanProperty propertyUnitSizeAuto;
    private SimpleBooleanProperty propertyReadyToSearch;
    private SimpleBooleanProperty propertyDeleteCondition;
    private SimpleBooleanProperty propertyRenameCondition;
    private IntegerBinding selectedSize;
    private ExtTask searchTask;
    public void beforeShow(String title,ExtFolder root,ExtFolder currentDir){
        
        super.beforeShow(title);
        
        menuItemTest.visibleProperty().bind(FileManagerLB.DEBUG);
        unitSize = DATA_SIZE.KB;
        propertyUnitSizeName = new SimpleStringProperty(unitSize.sizename);
        propertyUnitSize = new SimpleLongProperty(unitSize.size);
        propertyUnitSizeAuto = new SimpleBooleanProperty(false);
        propertyReadyToSearch = new SimpleBooleanProperty(true);
        propertyDeleteCondition = new SimpleBooleanProperty(false);
        propertyRenameCondition = new SimpleBooleanProperty(false);
        autoClose.selectedProperty().bindBidirectional(ViewManager.getInstance().autoCloseProgressDialogs);

        finder = new Finder("",useRegex.selectedProperty());
        Bindings.bindContentBidirectional(finder.list, searchView.getItems());
        finder.isCanceled.bind(this.propertyReadyToSearch);

        itemCount.textProperty().bind(Bindings.size(finder.list).asString());
        MC = new ManagingClass(root);
        
        LOAD();
        fileAddress = new FileAddressField(currentDirText);
        
        
        changeToDir(currentDir);
        selectedSize = Bindings.size(this.selectedList);
        Bindings.bindContentBidirectional(selectedList, this.tableView.getSelectionModel().getSelectedItems());
        


    }
    
    @Override
    public void exit(){ 
        Log.write("Closing internally " + windowID);
        ViewManager.getInstance().closeFrame(windowID); 
    }

    public void updateCurrentView(){
        
        Platform.runLater(()->{
            
            this.buttonForw.setDisable(!MC.hasForward());
            this.buttonPrev.setDisable(!MC.hasPrev());
            this.buttonParent.setDisable(MC.currentDir.isAbsoluteRoot());
            this.miAdvancedRename.setDisable(MC.currentDir.isAbsoluteRoot());
            if(MC.currentDir.isAbsoluteRoot()){
                fileAddress.field.setText("ROOT");
            }else{
                fileAddress.field.setText(MC.currentDir.getAbsoluteDirectory());
            }
            fileAddress.field.positionCaret(fileAddress.field.getLength());
            
            fileAddress.folder = MC.currentDir;
            fileAddress.f = null;

            Iterator<String> iterator = TaskFactory.getInstance().markedList.iterator();
            while(iterator.hasNext()){
                try{
                    if(!Files.exists(Paths.get(iterator.next()))){
                        iterator.remove();
                    }
                }catch(Exception e){}
            }
            
            propertyDeleteCondition.bind(MC.currentDir.isAbsoluteRoot.not().and(selectedSize.greaterThan(0)));
            propertyRenameCondition.bind(MC.currentDir.isAbsoluteRoot.not().and(selectedSize.isEqualTo(1)));
            MC.currentDir.update();
           
            tableView.setItems(FXCollections.observableArrayList(MC.getCurrentContents()));
            //Workaround to update table
            TableColumn get = (TableColumn) tableView.getColumns().get(0);
            get.setVisible(false);
            get.setVisible(true);
            
        });
        
        
        
    }
    public void closeAllWindows(){
        FileManagerLB.doOnExit();
    }
    public void createNewWindow(){
        ViewManager.getInstance().newWindow(ArtificialRoot,MC.currentDir);
    }
    public void advancedRenameFolder(){
        if(!MC.currentDir.isAbsoluteRoot()){
            ArrayList<String> list = new ArrayList<>();
            list.add(MC.currentDir.getAbsolutePath());
            ViewManager.getInstance().newAdvancedRenameDialog(list);
        }
    }
    public void advancedRenameMarked(){
        if(!MC.currentDir.isAbsoluteRoot()){
            ArrayList<String> list = new ArrayList<>();
            this.markedView.getItems().forEach(file ->{
                list.add(file.toString());
            });
            ViewManager.getInstance().newAdvancedRenameDialog(list);
        }
    }
    
    public void test(){
        try{
            Log.writeln(FileManagerLB.getRootSet());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    
    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }    
    public void changeToParent(){
        MC.changeToParent();
        updateCurrentView();
    }
    public void changeToPrevious(){
        MC.changeToPrevious();
        updateCurrentView();
        
    }
    public void changeToForward(){
        MC.changeToForward();
        updateCurrentView();
    }
    public void changeToDir(ExtFolder dir){
       MC.changeDirTo(dir);
       new Thread(TaskFactory.getInstance().populateRecursiveParallel(dir,FileManagerLB.DEPTH)).start();
       Platform.runLater(()->{
            updateCurrentView();
       });
      
    }
    public void searchTyped(){
        if(!this.useRegex.isSelected()){
            search();
        }
    }
    public void search(){
        String pattern = this.searchField.getText();
        this.propertyReadyToSearch.set(true);
        finder.list.clear();
        searchView.getItems().clear();
        if(pattern.length()>1){
            this.propertyReadyToSearch.set(false);
            
            this.searchStatus.setText("Searching");
            searchTask = new ExtTask(){
                @Override
                protected Void call() throws Exception {
                finder.newTask(pattern);
                
                if(!MC.currentDir.isAbsoluteRoot()){
                    try {
                        Files.walkFileTree(MC.currentDir.toPath(), finder);
                        
                    } catch (Exception ex) {
                        ErrorReport.report(ex);
                    }
                }else{
                    try {
                        for(ExtFile file:ArtificialRoot.getFilesCollection()){
                            Files.walkFileTree(file.toPath(), finder);
                        }
                    } catch (Exception ex) {
                        ErrorReport.report(ex);
                    }
                }
                    return null;
                };
            };
            searchTask.setOnSucceeded(eh ->{
                searchStatus.setText("Waiting");
            });
            new Thread(searchTask).start();
            
       
        }   
    }
    public void loadSnapshot(){
        try{
            String possibleSnapshot = this.snapshotLoadField.getText().trim();
            
            File file = new File(possibleSnapshot);
           
            if(file.exists()){
                new Thread(TaskFactory.getInstance().snapshotLoadTask(this.windowID,MC.currentDir,file)).start(); 
            }else{
                ErrorReport.report (new Exception("No such File:"+file.getAbsolutePath()));
            }
        }catch(Exception ex){
            ErrorReport.report(ex);
        }
    }
    public void createSnapshot(){
        if(MC.currentDir.isAbsoluteRoot()){
            ErrorReport.report(new Exception("Cannot create stapshots at ROOT"));
            return;
        }
        this.snapshotView.getItems().add("Creating snapshot at "+MC.currentDir.getAbsoluteDirectory());
        String possibleSnapshot = this.snapshotCreateField.getText().trim();
        File file = new File(TaskFactory.resolveAvailableName(MC.currentDir, possibleSnapshot));
        new Thread(TaskFactory.getInstance().snapshotCreateWriteTask(windowID,MC.currentDir, file)).start();
        
    }
    public void dirSync(){
        ViewManager.getInstance().newDirSyncDialog();
        
    }
    public void regexHelp(){
        Platform.runLater(()->{
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.Regex);
        });
    }
    public void aboutPage(){
        Platform.runLater(()->{
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        });
    }
    public void duplicateFind(){
        ViewManager.getInstance().newDuplicateFinderDialog(MC.currentDir);
    }
    
    private void selectInverted(MultipleSelectionModel sm){
        ObservableList<Integer> selected = sm.getSelectedIndices();
        ArrayList<Integer> array = new ArrayList<>();
        array.addAll(selected);
        sm.selectAll();
        array.stream().forEach(sm::clearSelection);
    }
    private void handleOpen(ExtFile file){
        if(file.getIdentity().equals(Enums.Identity.FOLDER)){
            changeToDir((ExtFolder) file);
        }else {
                            
            try{
                if(file.getIdentity().equals(Enums.Identity.LINK)){
                    ExtLink link = (ExtLink) file.getTrueForm();
                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                    if(link.isPointsToDirectory()){
                        changeToDir((ExtFolder) LocationAPI.getInstance().getFileByLocation(location));
                    }else{
                        DesktopApi.open(LocationAPI.getInstance().getFileByLocation(location));
                    }
                }else if(file.getIdentity().equals(Enums.Identity.FILE)){
                    DesktopApi.open(file);
                }
            }catch(Exception x){
                ErrorReport.report(x);
            }
        }
    }
    private void changeToCustomDir(String possibleDir){
        try{
            if(possibleDir.equalsIgnoreCase("ROOT")||possibleDir.isEmpty()){
                changeToDir(ArtificialRoot);
            }else{
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(possibleDir);
                if(fileAndPopulate!=null){
                    this.changeToDir(fileAndPopulate);
                }else{
                    updateCurrentView();
                }
                
            }
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }
    private Stage getStage(){
        return ViewManager.getInstance().getFrame(windowID).getStage();
    }
    private void hideAllContextMenus(){
        tableContextMenu.hide();
        markedContextMenu.hide();
        linksContextMenu.hide();
        tableDragContextMenu.hide();
        errorContextMenu.hide();
        searchContextMenu.hide();
    }
    private void setUpContextMenus(){
        contextMenuItems = new MenuItem[30];
        tableDragContextMenu = new ContextMenu();
        tableContextMenu = new ContextMenu();
        markedContextMenu = new ContextMenu();
        searchContextMenu = new ContextMenu();
        linksContextMenu = new ContextMenu();
        errorContextMenu = new ContextMenu();
        contextMenuItems[0] = new MenuItem("Create New Folder");
        contextMenuItems[0].setOnAction((eh) -> {
            Log.writeln("Create new folder");
            try {
                MC.createNewFolder();
                MainController.this.updateCurrentView();
            }catch (Exception ex) {
                ErrorReport.report(ex);
            }
        });
        
        contextMenuItems[1] = new MenuItem("Rename");
        contextMenuItems[1].setOnAction((eh)->{
            rename();
        });
        
        contextMenuItems[2] = new MenuItem("Delete");
        contextMenuItems[2].setOnAction((eh)->{
            delete();
        });
        
        contextMenuItems[3] = new MenuItem("Copy Here");
        contextMenuItems[3].setOnAction((eh)->{
            Log.writeln("Copy Marked");
            
            ExtTask task = TaskFactory.getInstance().copyFiles(TaskFactory.getInstance().markedList,MC.currentDir);
            task.setTaskDescription("Copy marked files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[4] = new MenuItem("Move Here");
        contextMenuItems[4].setOnAction((eh)->{
            Log.writeln("Move Marked");
            ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.getInstance().markedList,MC.currentDir);
            task.setTaskDescription("Move marked files");
            ViewManager.getInstance().newProgressDialog(task);
        
        });
        contextMenuItems[5] = new MenuItem("Create New File");
        contextMenuItems[5].setOnAction((eh)->{
            Log.writeln("Create new file");
            try {
                MC.createNewFile();
                MainController.this.updateCurrentView();
            }catch (IOException ex) {
                ErrorReport.report(ex);
            }
        });
        contextMenuItems[6] = new MenuItem("Add to marked");
        contextMenuItems[6].setOnAction((eh)->{
            selectedList.stream().forEach((file) -> {
                TaskFactory.getInstance().addToMarked(file.getAbsoluteDirectory());
            });  
        });
        contextMenuItems[7] = new MenuItem("Clean this list");
        contextMenuItems[7].setOnAction((e)->{
            //TaskFactory.getInstance().markedList.clear();
            this.markedView.getItems().clear();
        });
        
        contextMenuItems[8] = new MenuItem("Remove this item");
        contextMenuItems[8].setOnAction(eh ->{        
            ExtFile selectedItem = (ExtFile) markedView.getSelectionModel().getSelectedItem();
            TaskFactory.getInstance().markedList.remove(selectedItem.getAbsoluteDirectory());
        });
        
        contextMenuItems[9] = new MenuItem("Delete all marked");
        contextMenuItems[9].setOnAction(eh ->{
            ExtTask task = TaskFactory.getInstance().deleteFiles(
                    TaskFactory.getInstance().markedList);
            task.setTaskDescription("Delete marked files");
            ViewManager.getInstance().newProgressDialog(task);
            
        });
        contextMenuItems[10] = new MenuItem("Move here selected");
        contextMenuItems[10].setOnAction(eh ->{   
            TaskFactory.getInstance().actionList.clear();
            TaskFactory.getInstance().actionList.addAll(TaskFactory.getInstance().dragList);
            ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.getInstance().populateStringFileList(TaskFactory.getInstance().actionList),MC.currentDir);
            task.setTaskDescription("Move Dragged files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[11] = new MenuItem("Copy here selected");
        contextMenuItems[11].setOnAction(eh ->{        
            //Log.writeln("Copy Dragger");
             TaskFactory.getInstance().actionList.clear();
            TaskFactory.getInstance().actionList.addAll(TaskFactory.getInstance().dragList);
            ExtTask task = TaskFactory.getInstance().copyFiles(TaskFactory.getInstance().populateStringFileList(TaskFactory.getInstance().actionList),MC.currentDir);
            task.setTaskDescription("Copy Dragged files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[12] = new MenuItem("Go to");
        contextMenuItems[12].setOnAction((ActionEvent eh) -> {
            String selectedItem = (String) searchView.getSelectionModel().getSelectedItem();
            try{
                Path path = Paths.get(selectedItem);
                if(Files.isDirectory(path)){
                    changeToCustomDir(selectedItem);
                }else{
                    changeToCustomDir(path.getParent().toString());
                }
            }catch(Exception ex){
                ErrorReport.report(ex);
            }
        });
        
        contextMenuItems[13] = new MenuItem("Add current directory as link");
        contextMenuItems[13].setOnAction(eh ->{
            String dir = MC.currentDir.getAbsolutePath();
            FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(),dir);
            links.add(link);
            
        });
        contextMenuItems[14] = new MenuItem("Remove this link");
        contextMenuItems[14].setOnAction(eh ->{
            links.remove(linkView.getSelectionModel().getSelectedIndex());
        });
        contextMenuItems[15] = new MenuItem("Remove this entry");
        contextMenuItems[15].setOnAction(eh ->{
            errorLog.remove(errorView.getSelectionModel().getSelectedIndex());
        });
        contextMenuItems[16] = new MenuItem("Clean this list");
        contextMenuItems[16].setOnAction(eh ->{
            errorLog.clear();
        });
        contextMenuItems[17] = new MenuItem("Open in new window");
        contextMenuItems[17].setOnAction(eh ->{
            ViewManager.getInstance().newWindow(ArtificialRoot, (ExtFolder) tableView.getSelectionModel().getSelectedItem());
        });
        contextMenuItems[18] = new MenuItem("Open");
        contextMenuItems[18].setOnAction(eh ->{
            ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
            this.handleOpen(file);
        });
        
        //For search View
        contextMenuItems[19] = new MenuItem("All");
        contextMenuItems[19].setOnAction(eh ->{
            searchView.getSelectionModel().selectAll();
        });
        contextMenuItems[20] = new MenuItem("Invert Selection");
        contextMenuItems[20].setOnAction(eh ->{
            this.selectInverted(searchView.getSelectionModel());
        });
        //For table View
        contextMenuItems[21] = new MenuItem("All");
        contextMenuItems[21].setOnAction(eh ->{
            tableView.getSelectionModel().selectAll();
        });
        contextMenuItems[22] = new MenuItem("Invert Selection");
        contextMenuItems[22].setOnAction(eh ->{
            
            this.selectInverted(tableView.getSelectionModel());
        });
        contextMenuItems[23] = new MenuItem("Mark selected");
        contextMenuItems[23].setOnAction(eh ->{
            Platform.runLater(()->{
                ArrayList<String> list = new ArrayList<>();
                list.addAll(searchView.getItems());
                ExtTask markFiles = TaskFactory.getInstance().markFiles(list);
                new Thread(markFiles).start();
            });
                       
        });
        contextMenuItems[24] = new MenuItem("Copy Absolute Path");
        contextMenuItems[24].setOnAction(eh ->{
            String absolutePath = this.selectedList.get(0).getAbsolutePath();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
        });
        
        submenuSelectSearch = new Menu("Select...");
        submenuSelectSearch.getItems().setAll(
                contextMenuItems[19],
                contextMenuItems[20]
        );
        submenuSelectTable = new Menu("Select...");
        submenuSelectTable.getItems().setAll(
                contextMenuItems[21],
                contextMenuItems[22],
                contextMenuItems[24]
        );
        submenuMarked = new Menu("Marked...");
        submenuCreate = new Menu("Create...");
        submenuCreate.getItems().setAll(
                contextMenuItems[0],
                contextMenuItems[5]
        );
        
        searchContextMenu.getItems().setAll(
                contextMenuItems[12]
        );
        tableDragContextMenu.getItems().setAll(
                contextMenuItems[10],
                contextMenuItems[11]
        );
        
    }
    private void setUpTableView(){
        //TABLE VIEW SETUP
        columns = new ArrayList<>();
        
        TableColumn<ExtFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                return cellData.getValue().propertyName;
            }
        });
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        
        TableColumn<ExtFile, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtFile, String> cellData) -> cellData.getValue().propertyType);

        TableColumn<ExtFile, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(this.propertyUnitSizeName);
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                
                if(cellData.getValue().getIdentity().equals(Enums.Identity.FOLDER)){
                    return new SimpleStringProperty("");
                }else if(propertyUnitSizeAuto.get()){
                    return cellData.getValue().propertySizeAuto;
                }else{
                    SimpleStringProperty string = new SimpleStringProperty();
                    Double get = cellData.getValue().propertySize.divide((double)propertyUnitSize.get()).get();
                    if(get>0.001){
//                        String doubleString = String.valueOf(get);
//                        int indexOf = doubleString.indexOf('.');
//                        doubleString = doubleString.substring(0, Math.min(doubleString.length(),indexOf+4));
                        string.set(ExtStringUtils.extractNumber(get));
                    }else{
                        string.set("0");
                    }
                    return string;
                }
            }
        });
        sizeCol.setComparator(ExtFile.COMPARE_SIZE_STRING);
        TableColumn<ExtFile, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtFile, String> cellData) -> cellData.getValue().propertyDate);
        columns.add(nameCol);
        columns.add(typeCol);
        columns.add(sizeCol);
        columns.add(dateCol);
        

        
        //TABLE VIEW ACTIONS
        
        tableView.setContextMenu(tableContextMenu);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setOnMousePressed((MouseEvent event) -> {
            hideAllContextMenus();
            tableContextMenu.getItems().clear();
            int itemCount1 = tableView.getSelectionModel().getSelectedItems().size();
            int markedSize1 = TaskFactory.getInstance().markedList.size();
            if (event.isSecondaryButtonDown()) {
                if (itemCount1 == 1) {
                        tableContextMenu.getItems().add(contextMenuItems[18]);  //Open
                        ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                        if(file.getIdentity().equals(Enums.Identity.FOLDER)){
                            tableContextMenu.getItems().add(contextMenuItems[17]);  //Open in new window
                        }
                        
                }
                boolean con = false;
                if (!MC.currentDir.isAbsoluteRoot()) {
                    
                    if (markedSize1==0 && itemCount1 >= 1) {
                        submenuMarked.getItems().setAll(
                                contextMenuItems[6]         //Add to marked
                                //contextMenuItems[3],      //Copy
                                //contextMenuItems[4],      //Move
                                //contextMenuItems[9]       //Delete marked
                        );
                    } else if (markedSize1>0 && itemCount1 >= 1) {
                        submenuMarked.getItems().setAll(
                                contextMenuItems[6],      //Add to marked
                                contextMenuItems[3],      //Copy
                                contextMenuItems[4],      //Move
                                contextMenuItems[9]       //Delete marked
                        );
                    } else if (markedSize1>0 && itemCount1 == 0) {
                        submenuMarked.getItems().setAll(
                                //contextMenuItems[6]         //Add to marked
                                contextMenuItems[3],      //Copy
                                contextMenuItems[4],      //Move
                                contextMenuItems[9]       //Delete marked
                        );
                    }else{
                        con = true;
                    }

                    if (itemCount1 == 1) {
                        
                        tableContextMenu.getItems().addAll(
//                                submenuCreate,
                                contextMenuItems[1],    //Rename dialog
                                contextMenuItems[2]     //Delete dialog
                        );
                    } else if (itemCount1 > 1) {
                        tableContextMenu.getItems().addAll(
//                                submenuCreate,
                                //contextMenuItems[1],  //Rename dialog
                                contextMenuItems[2]     //Delete dialog
                        );
                    }
                    tableContextMenu.getItems().addAll(submenuCreate,submenuSelectTable,submenuMarked);
                    if(con)
                        tableContextMenu.getItems().remove(submenuMarked);
                }
            } else if(event.isPrimaryButtonDown()){
                if(!tableView.getSelectionModel().isEmpty()){
                    if(event.getClickCount() >1){
                        ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                        handleOpen(file);
                    }else{
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
                } 
            }
        });  
        tableView.setOnKeyReleased(eh->{
            KeyCode code = eh.getCode();
            if(code.equals(KeyCode.DELETE)){
                delete();
            }else if(code.equals(KeyCode.F2)){
                rename();
            }
        });
        tableView.setOnDragDetected((MouseEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            // drag was detected, start drag-and-drop gesture
            TaskFactory.getInstance().dragList = selectedList;
            if(!TaskFactory.getInstance().dragList.isEmpty()){
                Dragboard db = tableView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                    content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag

        tableView.setOnDragOver((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                
                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        tableView.setOnDragDropped((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!TaskFactory.getInstance().dragList.isEmpty()) {
                
                tableDragContextMenu.show(tableView,event.getScreenX(),event.getScreenY());
                tableDragContextMenu.getOwnerNode().requestFocus();
                tableDragContextMenu.getOwnerWindow().requestFocus();
                //ViewManager.getInstance().windows.get(title).getStage().requestFocus();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    private void setUpListViews(){
        
        //***************************************
        //Marked View
        markedView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        markedView.setItems(TaskFactory.getInstance().markedList);
        IntegerBinding size = Bindings.size(markedView.getItems());
        this.markedSize.textProperty().bind(size.asString());
        markedView.setContextMenu(markedContextMenu);
        markedView.setOnMousePressed((eh) ->{ 
            if((markedView.getSelectionModel().getSelectedItem()!= null)&&(TaskFactory.getInstance().markedList.size()>0)){
                    markedContextMenu.getItems().setAll(
                        contextMenuItems[7],
                        contextMenuItems[8]
                    );
            }else{
               markedContextMenu.getItems().clear();
            }
        });
        markedView.setOnDragDetected((MouseEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            // drag was detected, start drag-and-drop gesture
            TaskFactory.getInstance().dragList = markedView.getSelectionModel().getSelectedItems();
            if(!TaskFactory.getInstance().dragList.isEmpty()){
                Dragboard db = tableView.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag
        
        markedView.setOnDragOver((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            // data is dragged over the target
            Dragboard db = event.getDragboard();
            if (event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.MOVE);
                
                //Log.writeln(event.getDragboard().getString());
            }
            event.consume();
        });

        markedView.setOnDragDropped((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!TaskFactory.getInstance().dragList.isEmpty()) {
                for(ExtFile f:TaskFactory.getInstance().dragList){
                    TaskFactory.getInstance().addToMarked(f.getAbsoluteDirectory());
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        
        //***************************************
        //Search View
        searchView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchView.getItems().setAll(finder.list);
        searchView.setContextMenu(searchContextMenu);
        searchView.setOnMousePressed((MouseEvent eh) ->{
            searchContextMenu.getItems().clear();
            if(!searchView.getItems().isEmpty()){
                searchContextMenu.getItems().addAll(
                        submenuSelectSearch,
                        contextMenuItems[23]);
                if(searchView.getSelectionModel().getSelectedItems().size()==1){
                    searchContextMenu.getItems().add(contextMenuItems[12]);
                }
            }
        });
        //***************************************
        //Link View
        
        linkView.setContextMenu(linksContextMenu);
        linkView.setItems(FileManagerLB.links);
        linkView.setCellFactory(new Callback<ListView<FavouriteLink>, ListCell<FavouriteLink>>(){
            @Override
            public ListCell<FavouriteLink> call(ListView<FavouriteLink> p) {  
                ListCell<FavouriteLink> cell = new ListCell<FavouriteLink>(){
                    @Override
                    protected void updateItem(FavouriteLink t, boolean bln) {
                        super.updateItem(t, bln);
                        if (t != null) {
                            setText(t.getPropertyName().get());
                            if(!t.getPropertyName().get().equals("ROOT")){
                                setTooltip(t.getToolTip());
                            }
                        }else{
                            setText("");
                            this.tooltipProperty().unbind();
                        }
                    }
                };
                
                return cell;
            }
        });
        linkView.setOnMousePressed((MouseEvent eh) ->{
            int itemcount = linkView.getItems().size();
            linksContextMenu.getItems().clear();
            if(itemcount !=0){
                
                if((linkView.getSelectionModel().getSelectedIndex()!=0)&&(!linkView.getSelectionModel().isEmpty())){
                    linksContextMenu.getItems().setAll(contextMenuItems[14]);
                }
            }
            if(!MC.currentDir.isAbsoluteRoot()){
                linksContextMenu.getItems().add(0,contextMenuItems[13]);
            }
            if(eh.isPrimaryButtonDown()){
                if(eh.getClickCount()>1){
                    FavouriteLink selectedItem = (FavouriteLink) linkView.getSelectionModel().getSelectedItem();
                    changeToCustomDir(selectedItem.getDirectory());
                }
            }
        });
        
        //***************************************
        //Error View
        errorView.setItems(errorLog);
        errorView.setContextMenu(errorContextMenu);
        errorView.setCellFactory(new Callback<ListView<ErrorReport>, ListCell<ErrorReport>>(){
            @Override
            public ListCell<ErrorReport> call(ListView<ErrorReport> p) {  
                ListCell<ErrorReport> cell = new ListCell<ErrorReport>(){
                    
                    @Override
                    protected void updateItem(ErrorReport t, boolean bln) {
                        Platform.runLater(()->{
                            super.updateItem(t, bln);
                        if (t != null) {
                            setText(t.getErrorName().get());
                            setTooltip(t.getTooltip());
                        }else{
                            setText("");
                            this.tooltipProperty().unbind();
                        }
                        });
                        
                    }
                };
                
                return cell;
            }
        });
        
        errorView.setOnMousePressed((MouseEvent eh) ->{
            int itemcount = errorView.getItems().size();
            errorContextMenu.getItems().clear();
            if(itemcount !=0){
                errorContextMenu.getItems().setAll(contextMenuItems[15],contextMenuItems[16]);
            }
        });
        
        
    }
    private void LOAD(){
        setUpContextMenus();
        setUpTableView();
        setUpListViews();
        
        getStage().getScene().setOnMouseClicked((eh) -> {
            markedView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().clearSelection();
            searchView.getSelectionModel().clearSelection();
            errorView.getSelectionModel().clearSelection();
            linkView.getSelectionModel().clearSelection();  
        });
        
        for(MenuItem item:menuSizeUnits.getItems()){
            String sizeType = item.getText();
            
            if("Auto".equalsIgnoreCase(sizeType)){
                item.setOnAction(eh ->{
                   setSizeAuto();
                });
            }else{  
                item.setOnAction(eh ->{
                   this.propertyUnitSizeAuto.set(false);
                   this.unitSize = DATA_SIZE.valueOf(sizeType);
                   
                   Log.writeln(unitSize);
                   this.propertyUnitSizeName.set("Size "+unitSize.sizename);
                   this.propertyUnitSize.set(unitSize.size);
                   this.updateCurrentView();
                });
            }
        }
        Platform.runLater(()->{
            setSizeAuto();
            tableView.getColumns().setAll(columns);
        });
        
    }
    private void setSizeAuto(){
        this.propertyUnitSizeAuto.set(true);
        this.propertyUnitSizeName.set("Size Auto");
        this.updateCurrentView();
    }
    
    private void delete(){
        if(this.propertyDeleteCondition.not().get()){
            return;
        }
        Log.writeln("Deleting");
        ExtTask task = TaskFactory.getInstance().deleteFiles(TaskFactory.getInstance().populateStringFileList(selectedList));
        task.setTaskDescription("Delete selected files");
        ViewManager.getInstance().newProgressDialog(task);
    }
    private void rename(){
        if(this.propertyRenameCondition.not().get()){
            return;
        }   
        
        Log.writeln("Invoke rename dialog");
        ExtFile path = (ExtFile)tableView.getSelectionModel().getSelectedItem();
        ViewManager.getInstance().newRenameDialog(MC.currentDir,path);
        //Invoke text input dialog
        
    }
}
