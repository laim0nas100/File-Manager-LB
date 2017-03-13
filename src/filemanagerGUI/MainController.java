/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.Threads.ExtTask;
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
import utility.DesktopApi;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.Finder;
import LibraryLB.Log;
import LibraryLB.Threads.TimeoutTask;
import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerGUI.customUI.FileAddressField;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.DATA_SIZE;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.LocationInRootNode;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
    @FXML public CheckMenuItem autoStart;
    @FXML public SplitPane splitPane;
    
    @FXML public TableView tableView;
    @FXML public TextField localSearch;
    
    private ObservableList<ExtPath> selectedList = FXCollections.observableArrayList();
    
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static ObservableList<ExtPath> dragList;
    public static ObservableList<ExtPath> markedList;
    public static ArrayList<ExtPath> actionList;
    
    
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
    private SimpleBooleanProperty propertyIsVirtualFolders;
    private SimpleIntegerProperty propertyMarkedSize;
    private SimpleIntegerProperty propertySelectedSize;
    private SimpleIntegerProperty propertyMarkedSelectedSize;
    private Task searchTask;
   
    private TimeoutTask localSearchTask = new TimeoutTask(1000,10,() ->{
        Platform.runLater(()->{
           localSearch();  
        });
    });
    
    private TimeoutTask searchTimeoutTask = new TimeoutTask(500,100,()->{
        Platform.runLater(() ->{
            search();
        });
    });
    
    public ExtTableView extTableView;
    public void beforeShow(String title,ExtFolder currentDir){
        
        super.beforeShow(title);
        MC = new ManagingClass(currentDir);
        
        menuItemTest.visibleProperty().bind(FileManagerLB.DEBUG);
        unitSize = DATA_SIZE.KB;
        propertyUnitSizeName = new SimpleStringProperty(unitSize.sizename);
        propertyUnitSize = new SimpleLongProperty(unitSize.size);
        propertyUnitSizeAuto = new SimpleBooleanProperty(false);
        propertyReadyToSearch = new SimpleBooleanProperty(true);
        propertyDeleteCondition = new SimpleBooleanProperty(false);
        propertyRenameCondition = new SimpleBooleanProperty(false);
        propertyIsVirtualFolders = new SimpleBooleanProperty(false);
        propertyMarkedSelectedSize = new SimpleIntegerProperty(0);
        propertyMarkedSize = new SimpleIntegerProperty(0);
        propertySelectedSize = new SimpleIntegerProperty(0);
        autoClose.selectedProperty().bindBidirectional(ViewManager.getInstance().autoCloseProgressDialogs);
        autoStart.selectedProperty().bindBidirectional(ViewManager.getInstance().autoStartProgressDialogs);
        propertyDeleteCondition.bind(MC.isVirtual.not().and(propertySelectedSize.greaterThan(0)));
        propertyRenameCondition.bind(MC.isVirtual.not().and(propertySelectedSize.isEqualTo(1)));
        

        finder = new Finder("",useRegex.selectedProperty());
        Bindings.bindContentBidirectional(finder.list, searchView.getItems());
        finder.isCanceled.bind(this.propertyReadyToSearch);

        itemCount.textProperty().bind(Bindings.size(finder.list).asString());
        
        
        LOAD();
        fileAddress = new FileAddressField(currentDirText);
        propertyMarkedSize.bind(Bindings.size(MainController.markedList));
        propertyIsVirtualFolders.bind(MC.isAbsoluteRoot.not().and(miAdvancedRenameFolder.disableProperty()));
        propertySelectedSize.bind(Bindings.size(selectedList));
        propertyMarkedSelectedSize.bind(Bindings.size(markedView.getSelectionModel().getSelectedItems()));
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
    public void afterShow(){
        
    }
    @Override
    public void exit(){ 
//        this.localSearchTask.shutdown();
        ViewManager.getInstance().closeFrame(windowID); 
    }

    @Override
    public void update(){
        
        Platform.runLater(()->{
            Iterator<ExtPath> iterator = MainController.markedList.iterator();
            while(iterator.hasNext()){
                try{
                    Path toPath = iterator.next().toPath();
                    if(!Files.exists(toPath)){
                        iterator.remove();
                    }
                }catch(Exception e){
                    iterator.remove();
                }
            }
            this.buttonForw.setDisable(!MC.hasForward());
            this.buttonPrev.setDisable(!MC.hasPrev());
            this.buttonParent.setDisable(!MC.hasParent());
            this.miAdvancedRenameFolder.setDisable(MC.currentDir.isAbsoluteOrVirtualFolders());
            this.miDuplicateFinderFolder.disableProperty().bind(miAdvancedRenameFolder.disableProperty());
            this.miAdvancedRenameMarked.disableProperty().bind(Bindings.size(MainController.markedList).isEqualTo(0));
            this.miDuplicateFinderMarked.disableProperty().bind(miAdvancedRenameMarked.disableProperty());
            

            
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

            localSearch();

        });
        
    }
    public void closeAllWindows(){
        FileManagerLB.doOnExit();
    }
    public void createNewWindow(){
        ViewManager.getInstance().newWindow(MC.currentDir);
    }
    public void restart(){
        Platform.runLater(() ->{
            FileManagerLB.reInit();
        });
    }
    public void advancedRenameFolder(){
        if(!MC.currentDir.isAbsoluteRoot.get()){
            ViewManager.getInstance().newAdvancedRenameDialog(MC.currentDir);
        }
    }
    public void advancedRenameMarked(){
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.getInstance().newAdvancedRenameDialog(folder);
        
    }
    public void duplicateFinderMarked(){
        VirtualFolder folder = new VirtualFolder("Marked Files");
        folder.addAll(markedList);
        ViewManager.getInstance().newDuplicateFinderDialog(folder);
    }
    public void duplicateFinderFolder(){
        if(!MC.currentDir.isAbsoluteRoot.get()){
            VirtualFolder folder = new VirtualFolder(MC.currentDir.getAbsoluteDirectory());
            folder.addAll(MC.currentDir.getFilesCollection());
            ViewManager.getInstance().newDuplicateFinderDialog(folder);
        }    
    }
    public void mediaPlayer(){
        ViewManager.getInstance().newMusicPlayer();

    }
    public void test() throws Exception{
        Log.write("TEST");
//        LocationInRootNode root = new LocationInRootNode("",-1);
//        int i = 0;
//        
//        ArrayList<Sfor(ExtPath item:this.MC.currentDir.getListRecursive()){
//            root.add(new LocationInRoot(item.getAbsoluteDirectory(),false),i++);
//        }tring> resolve = LocationInRootNode.nodeFromFile(LibraryLB.FileManaging.FileReader.readFromFile("Here.txt")).resolve(true);
//        resolve.forEach(file ->{
//            Log.write(file);
//        });
////        LibraryLB.FileManaging.FileReader.writeToFile("Raw.txt", resolve);
//        Log.write(root.specialString());
//        LibraryLB.FileManaging.FileReader.writeToFile("Here.txt", Arrays.asList(new String[]{root.specialString()}));
//        Log.write("RESOLVED");
//        for(String p:root.resolve(true)){
//            Log.write(p);
//        }
//        
//        Log.write(ViewManager.getInstance().frames.keySet());
//        Log.write(FileManagerLB.getRootSet());
//        Log.write(Log.getInstance().list);
//        Log.writeln(MainController.class.getFields());
//        Log.write("Try to get field");
//        Field f = this.getClass().getField("autoClose");
//        CheckMenuItem get = (CheckMenuItem) f.get(ViewManager.getInstance().getFrame(windowID).getController());
//        get.selectedProperty().set(true);

        
//        ExtTask copyFiles = TaskFactory.getInstance().copyFiles(MC.currentDir.getListRecursiveFolders(true), LocationAPI.getInstance().getFileOptimized("E:\\Dev\\dest"),
//                LocationAPI.getInstance().getFileOptimized(MC.currentDir.getPathCommands().getParent(1)));
//            
//        ViewManager.getInstance().newProgressDialog(copyFiles);

        Log.write("END TEST");
    }

    
    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }
    private void changeToCustomDir(String possibleDir){
        try{
            if(possibleDir.equals(FileManagerLB.ROOT_NAME)||possibleDir.isEmpty()){
                changeToDir(FileManagerLB.ArtificialRoot);
            } 
            else{
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(possibleDir);
                if(!MC.currentDir.equals(fileAndPopulate)){
                    this.changeToDir(fileAndPopulate);
                }
                
            }
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }    
    public void changeToParent(){
        MC.changeToParent();
        this.localSearch.clear();
        update();
    }
    public void changeToPrevious(){
        MC.changeToPrevious();
        this.localSearch.clear();
        update();
        
    }
    public void changeToForward(){
        MC.changeToForward();
        this.localSearch.clear();
        update();
    }
    public void changeToDir(ExtFolder dir){
       MC.changeDirTo(dir);
       Thread t = new Thread(TaskFactory.getInstance().populateRecursiveParallel(dir,FileManagerLB.DEPTH));
       t.start();
       localSearch.clear();
       update();
      
    }
    
    public void searchTyped(){
        if(!this.useRegex.isSelected()){
            this.searchTimeoutTask.update();
        }
    }
    public void search(){
        String pattern = this.searchField.getText();
        this.propertyReadyToSearch.set(true);
        finder.list.clear();
        searchView.getItems().clear();
        if(searchTask!=null){
            searchTask.cancel();
        }
        if(pattern.length()>1){
            this.propertyReadyToSearch.set(false);
            
            this.searchStatus.setText("Searching");
            searchTask = new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                finder.newTask(pattern);
                
                if(!MC.currentDir.isVirtual.get()){
                    try {
                        Files.walkFileTree(MC.currentDir.toPath(), finder);
                        
                    } catch (Exception ex) {
                        ErrorReport.report(ex);
                    }
                }else if(!MC.currentDir.isAbsoluteOrVirtualFolders() && !MC.currentDir.isAbsoluteRoot.get()){
                    try {
                        for(ExtPath file:MC.currentDir.getFilesCollection()){
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
    public void localSearchTask(){
        localSearchTask.update();
    }
    public void localSearch(){
        Collection<ExtPath> currentContents = MC.getCurrentContents();
        ObservableList<ExtPath> newList = FXCollections.observableArrayList();
        String lookFor = localSearch.getText().trim();
        if(lookFor.length()==0){
            extTableView.updateContentsAndSort(currentContents);
            
        }else{
            currentContents.forEach(item ->{
                String name = item.propertyName.get();
                if(ExtStringUtils.containsIgnoreCase(name,lookFor)){
                    newList.add(item);
                }
            });
            extTableView.updateContentsAndSort(newList);
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
        if(MC.currentDir.isVirtual.get()){
            ErrorReport.report(new Exception("Cannot create stapshots from virtual folders"));
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
        contextMenuItems[0].visibleProperty().bind(MC.isVirtual.not());
        
        contextMenuItems[1] = new MenuItem("Rename");
        contextMenuItems[1].setOnAction((eh)->{
            rename();
        });
        contextMenuItems[1].visibleProperty().bind(propertyRenameCondition);
        
        contextMenuItems[2] = new MenuItem("Delete");
        contextMenuItems[2].setOnAction((eh)->{
            delete();
        });
        contextMenuItems[2].visibleProperty().bind(propertyDeleteCondition);
                
                
        contextMenuItems[3] = new MenuItem("Copy Here");
        contextMenuItems[3].setOnAction((eh)->{
            Log.writeln("Copy Marked");
            
            ExtTask task = TaskFactory.getInstance().copyFiles(markedList,MC.currentDir,null);
            task.setTaskDescription("Copy marked files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[3].visibleProperty().bind(propertyMarkedSize.greaterThan(0).and(MC.isVirtual.not()));
                
        contextMenuItems[4] = new MenuItem("Move Here");
        contextMenuItems[4].setOnAction((eh)->{
            Log.writeln("Move Marked");
            ExtTask task = TaskFactory.getInstance().moveFiles(markedList,MC.currentDir);
            task.setTaskDescription("Move marked files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[4].visibleProperty().bind(contextMenuItems[3].visibleProperty());

        
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
        contextMenuItems[5].visibleProperty().bind(MC.isVirtual.not());
        
        
        contextMenuItems[6] = new MenuItem("Add to marked");
        contextMenuItems[6].setOnAction((eh)->{
            selectedList.stream().forEach((file) -> {
                TaskFactory.getInstance().addToMarked(file);
            });  
        });
        contextMenuItems[6].visibleProperty().bind(miDuplicateFinderFolder.disableProperty().not().and(propertySelectedSize.greaterThan(0)));
        
        contextMenuItems[7] = new MenuItem("Clean this list");
        contextMenuItems[7].setOnAction((e)->{
            //MainController.markedList.clear();
            this.markedView.getItems().clear();
        });
        
        contextMenuItems[7].visibleProperty().bind(propertyMarkedSize.greaterThan(0));
        
        contextMenuItems[8] = new MenuItem("Remove selected");
        contextMenuItems[8].setOnAction(eh ->{        
            ObservableList selectedItems = markedView.getSelectionModel().getSelectedItems();
            MainController.markedList.removeAll(selectedItems);
        });
        contextMenuItems[8].visibleProperty().bind(this.propertyMarkedSize.greaterThan(0).and(Bindings.size(markedView.getSelectionModel().getSelectedItems()).greaterThan(0)));
        
        
        contextMenuItems[9] = new MenuItem("Delete all marked");
        contextMenuItems[9].setOnAction(eh ->{
            ExtTask task = TaskFactory.getInstance().deleteFiles(markedList);
            task.setTaskDescription("Delete marked files");
            ViewManager.getInstance().newProgressDialog(task);
            
        });
        contextMenuItems[9].visibleProperty().bind(this.propertyMarkedSize.greaterThan(0));
        
        
        //DRAG
        contextMenuItems[10] = new MenuItem("Move here selected");
        contextMenuItems[10].setOnAction(eh ->{   
            MainController.actionList.clear();
            MainController.actionList.addAll(MainController.dragList);
            ExtTask task = TaskFactory.getInstance().moveFiles(MainController.actionList,MC.currentDir);
            task.setTaskDescription("Move Dragged files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[11] = new MenuItem("Copy here selected");
        contextMenuItems[11].setOnAction(eh ->{        
            //Log.writeln("Copy Dragger");
             MainController.actionList.clear();
            MainController.actionList.addAll(MainController.dragList);
            ExtTask task = TaskFactory.getInstance().copyFiles(MainController.actionList,MC.currentDir,null);
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
        contextMenuItems[12].visibleProperty().bind(Bindings.size(searchView.getSelectionModel().getSelectedItems()).isEqualTo(1));
        
        contextMenuItems[13] = new MenuItem("Add current directory as link");
        contextMenuItems[13].setOnAction(eh ->{
            FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(),MC.currentDir);
            if(!links.contains(link)){
               links.add(link); 
            }
            
            
        });
//        contextMenuItems[13].visibleProperty().bind(Bindings.size(this.linkView.getSelectionModel().getSelectedIndices()).greaterThan(0));
        
        contextMenuItems[14] = new MenuItem("Remove this link");
        contextMenuItems[14].setOnAction(eh ->{
            links.remove(linkView.getSelectionModel().getSelectedIndex());
        });
        contextMenuItems[14].visibleProperty().bind(Bindings.size(this.linkView.getSelectionModel().getSelectedItems()).greaterThan(0));

        contextMenuItems[15] = new MenuItem("Remove this entry");
        contextMenuItems[15].setOnAction(eh ->{
            errorLog.remove(errorView.getSelectionModel().getSelectedIndex());
        });
        contextMenuItems[15].visibleProperty().bind(Bindings.size(errorView.getSelectionModel().getSelectedItems()).greaterThan(0));

        
        contextMenuItems[16] = new MenuItem("Clean this list");
        contextMenuItems[16].setOnAction(eh ->{
            errorLog.clear();
        });
        contextMenuItems[16].visibleProperty().bind(contextMenuItems[15].visibleProperty());
        
        //Manual
        contextMenuItems[17] = new MenuItem("Open in new window");
        contextMenuItems[17].setOnAction(eh ->{
            ViewManager.getInstance().newWindow((ExtFolder) tableView.getSelectionModel().getSelectedItem());
        });
        
        contextMenuItems[18] = new MenuItem("Open");
        contextMenuItems[18].setOnAction(eh ->{
            ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
            handleOpen(file);
        });
        contextMenuItems[18].visibleProperty().bind(propertySelectedSize.isEqualTo(1));
        
        contextMenuItems[19] = new MenuItem("Delete selected");
        contextMenuItems[19].setOnAction(eh ->{
            ExtTask task = TaskFactory.getInstance().deleteFiles(
                    TaskFactory.getInstance().populateStringFileList(markedView.getSelectionModel().getSelectedItems()));
            task.setTaskDescription("Delete marked files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[19].visibleProperty().bind(contextMenuItems[8].visibleProperty());
        
        //For search View
        contextMenuItems[20] = new MenuItem("Invert Selection");
        contextMenuItems[20].setOnAction(eh ->{
            selectInverted(searchView.getSelectionModel());
        });
        contextMenuItems[20].visibleProperty().bind(Bindings.size(searchView.getSelectionModel().getSelectedItems()).greaterThan(0));
        
        //For table View
        contextMenuItems[22] = new MenuItem("Invert Selection");
        contextMenuItems[22].setOnAction(eh ->{
            selectInverted(tableView.getSelectionModel());
        });
        contextMenuItems[22].visibleProperty().bind(propertySelectedSize.greaterThan(0));
        
        contextMenuItems[23] = new MenuItem("Mark selected");
        contextMenuItems[23].setOnAction(eh ->{
            Platform.runLater(()->{
                ArrayList<String> list = new ArrayList<>();
                list.addAll(searchView.getSelectionModel().getSelectedItems());
                ExtTask markFiles = TaskFactory.getInstance().markFiles(list);
                new Thread(markFiles).start();
            });
                       
        });
        contextMenuItems[23].visibleProperty().bind(Bindings.size(searchView.getSelectionModel().getSelectedItems()).greaterThan(0));
        
        contextMenuItems[24] = new MenuItem("Copy Absolute Path");
        contextMenuItems[24].setOnAction(eh ->{
            String absolutePath = selectedList.get(0).getAbsolutePath();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
        });
        contextMenuItems[24].visibleProperty().bind(this.propertyRenameCondition);
        
        contextMenuItems[25] = new MenuItem("Toggle Enable/Disable");
        contextMenuItems[25].setOnAction(eh ->{
            Platform.runLater(()->{
                selectedList.stream().forEach(c ->{
                    c.isDisabled.setValue(c.isDisabled.not().get());
                });
            });
        });
        contextMenuItems[25].visibleProperty().bind(this.propertyDeleteCondition);
        
        contextMenuItems[26] = new MenuItem("Create Virtual Folder");
        contextMenuItems[26].setOnAction(eh ->{
            Platform.runLater(()->{
                VirtualFolder.createVirtualFolder();
                update();
            });
        });
        contextMenuItems[26].visibleProperty().bind(propertyIsVirtualFolders);

          
        contextMenuItems[27] = new MenuItem("Add Marked to Virtual Folder");
        contextMenuItems[27].setOnAction(eh ->{
            Platform.runLater(()->{
                for(ExtPath f:MainController.markedList){
                        MC.currentDir.files.put(f.propertyName.get(), f);
                    }
                update();
            });
        });
        
        contextMenuItems[27].visibleProperty().bind(this.propertyMarkedSize.greaterThan(0).and(propertyIsVirtualFolders.not()).and(MC.isAbsoluteRoot.not()).and(MC.isVirtual));
        
        
        contextMenuItems[28] = new MenuItem("Remove selected from Virtual Folder");
        contextMenuItems[28].setOnAction(eh ->{
            Platform.runLater(()->{
                ObservableList<ExtPath> selectedItems = tableView.getSelectionModel().getSelectedItems();
                for(ExtPath item:selectedItems){
                    MC.currentDir.files.remove(item.propertyName.get());
                }
                update();
            });
        });
        contextMenuItems[28].visibleProperty().bind(MC.isVirtual.and(propertySelectedSize.greaterThan(0)).and(MC.isAbsoluteRoot.not()));
        
        
        
        submenuMarked = new Menu("Marked...");
        submenuMarked.getItems().setAll(
                contextMenuItems[6],        //Add to marked
                contextMenuItems[3],        //Copy
                contextMenuItems[4],        //Move
                contextMenuItems[9],        //Delete marked
                contextMenuItems[27]        //Add Marked to virtual
        );
//        submenuMarked.visibleProperty().bind(this.propertyMarkedSize.greaterThan(0).or(this.propertySelectedSize.greaterThan(0)));
        submenuMarked.visibleProperty().bind(contextMenuItems[6].visibleProperty().or(
                contextMenuItems[3].visibleProperty()).or(contextMenuItems[4].visibleProperty()).or(
                        contextMenuItems[9].visibleProperty()).or(contextMenuItems[27].visibleProperty()));
        submenuCreate = new Menu("Create...");
        submenuCreate.getItems().setAll(
                contextMenuItems[0],
                contextMenuItems[5]
        );
        submenuCreate.visibleProperty().bind(MC.isVirtual.not());
         
        
       
        searchContextMenu.getItems().setAll(
                contextMenuItems[12],
                contextMenuItems[23],
                contextMenuItems[20]
        );
        
        errorContextMenu.getItems().setAll(
                contextMenuItems[15],
                contextMenuItems[16]
        
        );
        linksContextMenu.getItems().setAll(
                contextMenuItems[13],
                contextMenuItems[14]
        );
        markedContextMenu.getItems().setAll(
                contextMenuItems[7],
                contextMenuItems[8],
                contextMenuItems[19]
        
        );
        tableDragContextMenu.getItems().setAll(
                contextMenuItems[10],
                contextMenuItems[11]
        );
        tableContextMenu.getItems().setAll(
                contextMenuItems[18],       //Open
                contextMenuItems[17],       //Open in new window
                submenuCreate,
                contextMenuItems[1],    //Rename dialog
                contextMenuItems[2],     //Delete dialog
                contextMenuItems[22],
                contextMenuItems[24],
                contextMenuItems[25],
                contextMenuItems[26],
                contextMenuItems[28],
                submenuMarked
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
                
                if(cellData.getValue() instanceof ExtFolder){
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
            if (event.isSecondaryButtonDown()) {
                if(propertySelectedSize.greaterThan(0).get()){
                    ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
                    contextMenuItems[17].setVisible(file instanceof ExtFolder && propertySelectedSize.isEqualTo(1).get());
                }else{
                    contextMenuItems[17].setVisible(false);
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
            if(MC.currentDir.isAbsoluteOrVirtualFolders()){
                return;
            }
            TaskFactory.dragInitWindowID = this.windowID;
            if(this.extTableView.recentlyResized.get()){
                return;
            }
            // drag was detected, start drag-and-drop gesture
            MainController.dragList = selectedList;
            if(!MainController.dragList.isEmpty()){
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
            if(MC.currentDir.isAbsoluteOrVirtualFolders()){
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
            if(MC.currentDir.isVirtual.get()){
                return;
            }
            if(this.windowID.equals(TaskFactory.dragInitWindowID)){
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {
                
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
        markedView.setItems(MainController.markedList);
        IntegerBinding size = Bindings.size(markedView.getItems());
        this.markedSize.textProperty().bind(size.asString());
        markedView.setContextMenu(markedContextMenu);
        markedView.setOnDragDetected((MouseEvent event) -> {
            TaskFactory.dragInitWindowID = "MARKED";
            if(MC.currentDir.isAbsoluteOrVirtualFolders()){
                return;
            }
            
            // drag was detected, start drag-and-drop gesture
            dragList = markedView.getSelectionModel().getSelectedItems();
            if(!MainController.dragList.isEmpty()){
                Dragboard db = markedView.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                //Log.writeln("Drag detected:"+selected.getAbsolutePath());
                content.putString("Ready");
                //content.putString(selected.getAbsolutePath());
                db.setContent(content);
                event.consume();
            }
        }); //drag
        
        markedView.setOnDragOver((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteOrVirtualFolders()){
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
            if(MC.currentDir.isAbsoluteOrVirtualFolders()){
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!MainController.dragList.isEmpty()) {
                for(ExtPath f:MainController.dragList){
                    TaskFactory.getInstance().addToMarked(f);
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
        //***************************************
        //Link View
        
        linkView.setContextMenu(linksContextMenu);
        linkView.setItems(links);
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
            if(eh.isPrimaryButtonDown()){
                if(eh.getClickCount()>1){
                    FavouriteLink selectedItem = (FavouriteLink) linkView.getSelectionModel().getSelectedItem();
                    changeToDir((ExtFolder) selectedItem.location);
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
        ExtTask task = TaskFactory.getInstance().deleteFiles(selectedList);
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
