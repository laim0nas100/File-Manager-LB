/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.ExtFolder;
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
import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerGUI.customUI.FileAddressField;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.DATA_SIZE;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.SimpleTask;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Collection;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;
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
    private ObservableList<ExtPath> selectedList = FXCollections.observableArrayList();
    
    
    
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
    
    @FXML public MenuItem miAdvancedRenameFolder;
    @FXML public MenuItem miAdvancedRenameMarked;
    @FXML public MenuItem miDuplicateFinderFolder;
    @FXML public MenuItem miDuplicateFinderMarked;
    
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
    private Task searchTask;
    public ExtTableView extTableView;
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
        
        
        
        selectedSize = Bindings.size(this.selectedList);
        Bindings.bindContentBidirectional(selectedList, this.tableView.getSelectionModel().getSelectedItems());
        extTableView = new ExtTableView(tableView);
        extTableView.sortable = true;
        extTableView.prepareChangeListeners();
        
        changeToDir(currentDir);
        TableColumn typeCol = (TableColumn) tableView.getColumns().get(1);
        TableColumn nameCol = (TableColumn) tableView.getColumns().get(0);
        typeCol.setSortType(TableColumn.SortType.DESCENDING);
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        tableView.getSortOrder().add(typeCol);
        tableView.getSortOrder().add(nameCol);

        update();
    }
    
    @Override
    public void exit(){ 
        ViewManager.getInstance().closeFrame(windowID); 
    }

    @Override
    public void update(){
        
        Platform.runLater(()->{
            Iterator<String> iterator = TaskFactory.getInstance().markedList.iterator();
            while(iterator.hasNext()){
                try{
                    if(!Files.exists(Paths.get(iterator.next()))){
                        iterator.remove();
                    }
                }catch(Exception e){}
            }
            this.buttonForw.setDisable(!MC.hasForward());
            this.buttonPrev.setDisable(!MC.hasPrev());
            this.buttonParent.setDisable(!MC.hasParent());
            this.miAdvancedRenameFolder.setDisable(!MC.currentDir.isAbsoluteRoot.get() || MC.currentDir.equals(FileManagerLB.VirtualFolders));
            this.miDuplicateFinderFolder.setDisable(!MC.currentDir.isAbsoluteRoot.get() || MC.currentDir.equals(FileManagerLB.VirtualFolders));
            this.miAdvancedRenameMarked.disableProperty().bind(TaskFactory.getInstance().propertyMarkedSize.isEqualTo(0));
            this.miDuplicateFinderMarked.disableProperty().bind(TaskFactory.getInstance().propertyMarkedSize.isEqualTo(0));
            

            
            if(MC.currentDir.isAbsoluteRoot.get()){
                fileAddress.field.setText(FileManagerLB.ROOT_NAME);
            }else if(MC.currentDir.getIdentity().equals(Identity.VIRTUAL)){
                fileAddress.field.setText(MC.currentDir.propertyName.get());
            }else{
                fileAddress.field.setText(MC.currentDir.getAbsoluteDirectory());
            }
            fileAddress.field.positionCaret(fileAddress.field.getLength());
            
            fileAddress.folder = MC.currentDir;
            fileAddress.f = null;

            
            
            propertyDeleteCondition.bind(MC.currentDir.isAbsoluteRoot.not().and(selectedSize.greaterThan(0)));
            propertyRenameCondition.bind(MC.currentDir.isAbsoluteRoot.not().and(selectedSize.isEqualTo(1)));
            Platform.runLater(()->{
                extTableView.updateContentsAndSort(MC.getCurrentContents());
            });
        });
        
    }
    public void closeAllWindows(){
        FileManagerLB.doOnExit();
    }
    public void createNewWindow(){
        ViewManager.getInstance().newWindow(FileManagerLB.ArtificialRoot,MC.currentDir);
    }
    public void advancedRenameFolder(){
        if(!MC.currentDir.isAbsoluteRoot()){
            VirtualFolder folder = new VirtualFolder(MC.currentDir.getAbsoluteDirectory());
            folder.addAll(MC.currentDir.getFilesCollection());
            ViewManager.getInstance().newAdvancedRenameDialog(folder);
        }
    }
    public void advancedRenameMarked(){
        if(!MC.currentDir.isAbsoluteRoot()){
            Collection<ExtPath> populateExtPathList = TaskFactory.getInstance().populateExtPathList(TaskFactory.getInstance().markedList);
            VirtualFolder folder = new VirtualFolder("Marked Files");
            folder.addAll(populateExtPathList);
            ViewManager.getInstance().newAdvancedRenameDialog(folder);
        }
    }
    public void duplicateFinderMarked(){
        if(!MC.currentDir.isAbsoluteRoot()){
            Collection<ExtPath> populateExtPathList = TaskFactory.getInstance().populateExtPathList(TaskFactory.getInstance().markedList);
            VirtualFolder folder = new VirtualFolder("Marked Files");
            folder.addAll(populateExtPathList);
            ViewManager.getInstance().newDuplicateFinderDialog(folder);
        }
    }
    public void duplicateFinderFolder(){
        if(!MC.currentDir.isAbsoluteRoot()){
            VirtualFolder folder = new VirtualFolder(MC.currentDir.getAbsoluteDirectory());
            folder.addAll(MC.currentDir.getFilesCollection());
            ViewManager.getInstance().newAdvancedRenameDialog(folder);
        }    
    }
    public void mediaPlayer(){
        ViewManager.getInstance().newMusicPlayer(TaskFactory.getInstance().markedList);

    }
    public void test() throws IOException{
        Log.write("TEST");
        ViewManager.getInstance().newMusicPlayer(new ArrayList<>(TaskFactory.getInstance().markedList));
    }

    
    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }    
    public void changeToParent(){
        MC.changeToParent();
        update();
    }
    public void changeToPrevious(){
        MC.changeToPrevious();
        update();
        
    }
    public void changeToForward(){
        MC.changeToForward();
        update();
    }
    public void changeToDir(ExtFolder dir){
       MC.changeDirTo(dir);
       Thread t = new Thread(TaskFactory.getInstance().populateRecursiveParallel(dir,FileManagerLB.DEPTH));
       t.start();
       update();
      
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
            searchTask = new Task<Void>(){
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
                        for(ExtPath file:FileManagerLB.ArtificialRoot.getFilesCollection()){
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
            ErrorReport.report(new Exception("Cannot create stapshots at "+ FileManagerLB.ROOT_NAME));
            return;
        }
        this.snapshotView.getItems().add("Creating snapshot at "+MC.currentDir.getAbsoluteDirectory());
        String possibleSnapshot = this.snapshotCreateField.getText().trim();
        File file = new File(TaskFactory.resolveAvailablePath(MC.currentDir, possibleSnapshot));
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
    public void commandWindow(){
        ViewManager.getInstance().newCommandDialog();
    }
    
    private void selectInverted(MultipleSelectionModel sm){
        ObservableList<Integer> selected = sm.getSelectedIndices();
        ArrayList<Integer> array = new ArrayList<>();
        array.addAll(selected);
        sm.selectAll();
        array.stream().forEach(sm::clearSelection);
    }
    private void handleOpen(ExtPath file){
        if(file instanceof ExtFolder){
            Log.write("Change to dir "+file.getAbsoluteDirectory());
            changeToDir((ExtFolder) file);
        }else {
                            
            try{
                if(file.getIdentity().equals(Enums.Identity.LINK)){
                    ExtLink link = (ExtLink) file;
                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                    if(link.isPointsToDirectory()){
                        changeToDir((ExtFolder) LocationAPI.getInstance().getFileByLocation(location));
                    }else{
                        DesktopApi.open(LocationAPI.getInstance().getFileByLocation(location).toPath().toFile());
                    }
                }else if(file.getIdentity().equals(Enums.Identity.FILE)){
                    DesktopApi.open(file.toPath().toFile());
                }
            }catch(Exception x){
                ErrorReport.report(x);
            }
        }
    }
    private void changeToCustomDir(String possibleDir){
        try{
            if(possibleDir.equalsIgnoreCase(FileManagerLB.ROOT_NAME)||possibleDir.isEmpty()){
                changeToDir(FileManagerLB.ArtificialRoot);
            }else{
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(possibleDir);
                if(fileAndPopulate!=null){
                    this.changeToDir(fileAndPopulate);
                }else{
                    update();
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
                MainController.this.update();
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
            
            SimpleTask task = TaskFactory.getInstance().copyFiles(TaskFactory.getInstance().markedList,MC.currentDir);
            task.setTaskDescription("Copy marked files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[4] = new MenuItem("Move Here");
        contextMenuItems[4].setOnAction((eh)->{
            Log.writeln("Move Marked");
            SimpleTask task = TaskFactory.getInstance().moveFiles(TaskFactory.getInstance().markedList,MC.currentDir);
            task.setTaskDescription("Move marked files");
            ViewManager.getInstance().newProgressDialog(task);
        
        });
        contextMenuItems[5] = new MenuItem("Create New File");
        contextMenuItems[5].setOnAction((eh)->{
            Log.writeln("Create new file");
            try {
                MC.createNewFile();
                MainController.this.update();
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
            String selectedItem = (String) markedView.getSelectionModel().getSelectedItem();
            TaskFactory.getInstance().markedList.remove(selectedItem);
        });
        
        contextMenuItems[9] = new MenuItem("Delete all marked");
        contextMenuItems[9].setOnAction(eh ->{
            SimpleTask task = TaskFactory.getInstance().deleteFiles(
                    TaskFactory.getInstance().markedList);
            task.setTaskDescription("Delete marked files");
            ViewManager.getInstance().newProgressDialog(task);
            
        });
        contextMenuItems[10] = new MenuItem("Move here selected");
        contextMenuItems[10].setOnAction(eh ->{   
            TaskFactory.getInstance().actionList.clear();
            TaskFactory.getInstance().actionList.addAll(TaskFactory.getInstance().dragList);
            SimpleTask task = TaskFactory.getInstance().moveFiles(TaskFactory.getInstance().populateStringFileList(TaskFactory.getInstance().actionList),MC.currentDir);
            task.setTaskDescription("Move Dragged files");
            boolean paused = false;
            if(TaskFactory.dragInitWindowID.equals(MediaPlayerController.ID)){
                    paused = true;
            }
            ViewManager.getInstance().newProgressDialog(task,paused);
        });
        contextMenuItems[11] = new MenuItem("Copy here selected");
        contextMenuItems[11].setOnAction(eh ->{        
            //Log.writeln("Copy Dragger");
             TaskFactory.getInstance().actionList.clear();
            TaskFactory.getInstance().actionList.addAll(TaskFactory.getInstance().dragList);
            SimpleTask task = TaskFactory.getInstance().copyFiles(TaskFactory.getInstance().populateStringFileList(TaskFactory.getInstance().actionList),MC.currentDir);
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
            ViewManager.getInstance().newWindow(FileManagerLB.ArtificialRoot, (ExtFolder) tableView.getSelectionModel().getSelectedItem());
        });
        contextMenuItems[18] = new MenuItem("Open");
        contextMenuItems[18].setOnAction(eh ->{
            ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
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
                list.addAll(searchView.getSelectionModel().getSelectedItems());
                SimpleTask markFiles = TaskFactory.getInstance().markFiles(list);
                new Thread(markFiles).start();
            });
                       
        });
        contextMenuItems[24] = new MenuItem("Copy Absolute Path");
        contextMenuItems[24].setOnAction(eh ->{
            String absolutePath = this.selectedList.get(0).getAbsolutePath();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
        });
        
        contextMenuItems[25] = new MenuItem("Toggle Enable/Disable");
        contextMenuItems[25].setOnAction(eh ->{
            Platform.runLater(()->{
                
                this.selectedList.stream().forEach(c ->{
                    c.isDisabled.setValue(c.isDisabled.not().get());
                });
            });
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
        
        TableColumn<ExtPath, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtPath, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtPath, String> cellData) {
                return cellData.getValue().propertyName;
            }
        });
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        
        TableColumn<ExtPath, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyType);

        TableColumn<ExtPath, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(this.propertyUnitSizeName);
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtPath, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtPath, String> cellData) {
                
                if(cellData.getValue().getIdentity().equals(Enums.Identity.FOLDER)){
                    return new SimpleStringProperty("");
                }else if(propertyUnitSizeAuto.get()){
                    return cellData.getValue().propertySizeAuto;
                }else{
                    SimpleStringProperty string = new SimpleStringProperty();
                    Double get = cellData.getValue().propertySize.divide((double)propertyUnitSize.get()).get();
                    if(get>0.001){
                        string.set(ExtStringUtils.extractNumber(get));
                    }else{
                        string.set("0");
                    }
                    return string;
                }
            }
        });
        sizeCol.setComparator(ExtPath.COMPARE_SIZE_STRING);
        TableColumn<ExtPath, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().propertyDate);
        TableColumn<ExtPath, String> disabledCol = new TableColumn<>("Disabled");
        disabledCol.setCellValueFactory((TableColumn.CellDataFeatures<ExtPath, String> cellData) -> cellData.getValue().isDisabled.asString());
        tableView.getColumns().addAll(nameCol,typeCol,sizeCol,dateCol,disabledCol);

        
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
                        ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
                        if(file instanceof ExtFolder){
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
                                contextMenuItems[6],        //Add to marked
                                contextMenuItems[3],        //Copy
                                contextMenuItems[4],        //Move
                                contextMenuItems[9]        //Delete marked
                                
                        );
                    } else if (markedSize1>0 && itemCount1 == 0) {
                        submenuMarked.getItems().setAll(
                                //contextMenuItems[6]       //Add to marked
                                contextMenuItems[3],        //Copy
                                contextMenuItems[4],        //Move
                                contextMenuItems[9]        //Delete marked
                                
                        );
                    }else{
                        con = true;
                    }

                    if (itemCount1 == 1) {
                        
                        tableContextMenu.getItems().addAll(
//                                submenuCreate,
                                contextMenuItems[1],    //Rename dialog
                                contextMenuItems[2],     //Delete dialog
                                contextMenuItems[25]        //Toggle Enable
                        );
                    } else if (itemCount1 > 1) {
                        tableContextMenu.getItems().addAll(
//                                submenuCreate,
                                //contextMenuItems[1],  //Rename dialog
                                contextMenuItems[2],     //Delete dialog
                                contextMenuItems[25]        //Toggle Enable
                        );
                    }
                    tableContextMenu.getItems().addAll(submenuCreate,submenuSelectTable,submenuMarked);
                    if(con)
                        tableContextMenu.getItems().remove(submenuMarked);
                }
            } else if(event.isPrimaryButtonDown()){
                if(!tableView.getSelectionModel().isEmpty()){
                    if(event.getClickCount() >1){
                        ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
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
            TaskFactory.dragInitWindowID = this.windowID;
            if(this.extTableView.recentlyResized.get()){
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
            if(this.windowID.equals(TaskFactory.dragInitWindowID)){
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
            if(this.windowID.equals(TaskFactory.dragInitWindowID)){
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
                for(ExtPath f:TaskFactory.getInstance().dragList){
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
                            if(!t.getPropertyName().get().equals(FileManagerLB.ROOT_NAME)){
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
                   this.update();
                });
            }
        }
        Platform.runLater(()->{
            setSizeAuto();
//            tableView.getColumns().setAll(columns);
        });
        
    }
    private void setSizeAuto(){
        this.propertyUnitSizeAuto.set(true);
        this.propertyUnitSizeName.set("Size Auto");
        this.update();
    }
    
    private void delete(){
        if(this.propertyDeleteCondition.not().get()){
            return;
        }
        Log.writeln("Deleting");
        SimpleTask task = TaskFactory.getInstance().deleteFiles(TaskFactory.getInstance().populateStringFileList(selectedList));
        task.setTaskDescription("Delete selected files");
        ViewManager.getInstance().newProgressDialog(task);
    }
    private void rename(){
        if(this.propertyRenameCondition.not().get()){
            return;
        }   
        
        Log.writeln("Invoke rename dialog");
        ExtPath path = (ExtPath)tableView.getSelectionModel().getSelectedItem();
        ViewManager.getInstance().newRenameDialog(MC.currentDir,path);
        //Invoke text input dialog
        
    }
}
