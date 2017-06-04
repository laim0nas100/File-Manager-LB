/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.FX.FXTask;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import utility.DesktopApi;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.Finder;
import LibraryLB.Log;
import LibraryLB.Threads.ExtTask;
import LibraryLB.Threads.TimeoutTask;
import filemanagerGUI.customUI.CosmeticsFX;
import filemanagerGUI.customUI.CosmeticsFX.ExtTableView;
import filemanagerGUI.customUI.FileAddressField;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.DATA_SIZE;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    @FXML public CheckMenuItem pinDialogs;
    @FXML public CheckMenuItem pinTextInput;
    @FXML public SplitPane splitPane;
    
    @FXML public TableView tableView;
    @FXML public TextField localSearch;
    
    private ObservableList<ExtPath> selectedList = FXCollections.observableArrayList();
    
    
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static ObservableList<ExtPath> dragList;
    public static ObservableList<ExtPath> markedList;
    public static IntegerBinding propertyMarkedSize;
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
    
    private FileAddressField fileAddress;
    private ManagingClass MC;
    private Finder finder;
    private DATA_SIZE unitSize = DATA_SIZE.KB;
    private SimpleStringProperty  propertyUnitSizeName = new SimpleStringProperty(unitSize.sizename);
    private SimpleLongProperty    propertyUnitSize = new SimpleLongProperty(unitSize.size);
    private SimpleBooleanProperty propertyUnitSizeAuto = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyReadyToSearch = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty propertyDeleteCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyRenameCondition = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty propertyIsVirtualFolders = new SimpleBooleanProperty(false);
    
    private SimpleIntegerProperty propertySelectedSize = new SimpleIntegerProperty(0);
    private SimpleIntegerProperty propertyMarkedSelectedSize = new SimpleIntegerProperty(0);
    private SimpleBooleanProperty selectedIsFolder = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty writeableFolder = new SimpleBooleanProperty(false);
    private Task searchTask;
    public ExtTableView extTableView;
    public ArrayDeque<Future> deq = new ArrayDeque<>();
    private ExecutorService localSearchExecutor = Executors.newSingleThreadExecutor();

    private TimeoutTask localSearchTask = new TimeoutTask(1000,10,() ->{
        Platform.runLater(()->{
           localSearch();       
        });
        
    });
    private boolean firstTime = true;
    private TimeoutTask searchTimeoutTask = new TimeoutTask(500,100,()->{
        Platform.runLater(() ->{
            search();
        });
    });
    
    
    
    public void beforeShow(String title,ExtFolder currentDir){
        
        super.beforeShow(title);
        MC = new ManagingClass(currentDir);

    }
    
    @Override
    public void afterShow(){
        menuItemTest.visibleProperty().bind(FileManagerLB.DEBUG);
       
        autoClose.selectedProperty().bindBidirectional(ViewManager.getInstance().autoCloseProgressDialogs);
        autoStart.selectedProperty().bindBidirectional(ViewManager.getInstance().autoStartProgressDialogs);
        pinDialogs.selectedProperty().bindBidirectional(ViewManager.getInstance().pinProgressDialogs);
        pinTextInput.selectedProperty().bindBidirectional(ViewManager.getInstance().pinTextInputDialogs);

        propertyDeleteCondition.bind(writeableFolder.and(propertySelectedSize.greaterThan(0)));
        propertyRenameCondition.bind(writeableFolder.and(propertySelectedSize.isEqualTo(1)));
        

        finder = new Finder("",useRegex.selectedProperty());
        Bindings.bindContentBidirectional(finder.list, searchView.getItems());
        finder.isCanceled.bind(this.propertyReadyToSearch);

        itemCount.textProperty().bind(Bindings.size(finder.list).asString());
        
        
        LOAD();
        fileAddress = new FileAddressField(currentDirText);
        propertyIsVirtualFolders.bind(MC.isAbsoluteRoot.not().and(miAdvancedRenameFolder.disableProperty()));
        propertySelectedSize.bind(Bindings.size(selectedList));
        propertyMarkedSelectedSize.bind(Bindings.size(markedView.getSelectionModel().getSelectedItems()));
        Bindings.bindContentBidirectional(selectedList, this.tableView.getSelectionModel().getSelectedItems());
        extTableView = new ExtTableView(tableView);
        extTableView.sortable = true;
        extTableView.prepareChangeListeners();
        
        
        
        //default sort order
        
        Platform.runLater(() ->{
            
            
            TableColumn typeCol = (TableColumn) tableView.getColumns().get(1);
            TableColumn nameCol = (TableColumn) tableView.getColumns().get(0);
            typeCol.setSortType(TableColumn.SortType.DESCENDING);
            nameCol.setSortType(TableColumn.SortType.ASCENDING);
            tableView.getSortOrder().clear();
            tableView.getSortOrder().add(typeCol);
            tableView.getSortOrder().add(nameCol);
//            tableView.getItems().add(MC.currentDir);
//            try {
//                //            changeToDir(MC.currentDir).start();
//                Thread.sleep(4000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
//            }
            update();
        });
        
//        localSearchExecutor.setPriorityRunnerSize(0);
//        localSearchExecutor.setRunnerSize(1);        

    }
    @Override
    public void exit(){ 
        ViewManager.getInstance().closeFrame(windowID);
//        this.localSearchExecutor.cancel();
    }

    @Override
    public void update(){
        Platform.runLater(()->{
            if(firstTime){
                firstTime = false;
            }else{
                localSearch();
            }
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
            this.miAdvancedRenameFolder.setDisable(MC.currentDir.isNotWriteable());
            this.miDuplicateFinderFolder.disableProperty().bind(miAdvancedRenameFolder.disableProperty());
            this.miAdvancedRenameMarked.disableProperty().bind(Bindings.size(MainController.markedList).isEqualTo(0));
            this.miDuplicateFinderMarked.disableProperty().bind(miAdvancedRenameMarked.disableProperty());
            this.writeableFolder.set(!MC.currentDir.isNotWriteable());

            
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
        });
        
    }
    public void closeAllWindows(){
        FileManagerLB.doOnExit();
        System.exit(0);
    }
    public void createNewWindow(){
        ViewManager.getInstance().newWindow(MC.currentDir);
    }
    public void restart() throws InterruptedException{
        FileManagerLB.restart();
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
        ViewManager.getInstance().newMediaPlayer();

    }
    public void test() throws Exception{
        Log.print("TEST");
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
//        ViewManager.getInstance().newProgressDialog(copyFiles);

//        CodeSource codeSource = FileManagerLB.class.getProtectionDomain().getCodeSource();
//        File jarFile = new File(codeSource.getLocation().toURI().getPath());       
//        System.out.println(jarFile.getAbsolutePath());


//        ExtPath path = new ExtPath("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]\\Unsorted\\2 Senses - Found.mp3");
//        LocationInRoot folder = new LocationInRoot("E:\\FileZZZ\\General Music Folder\\[CHILLSTEP]");
//        LocationInRoot file = new LocationInRoot(path.getAbsoluteDirectory());
//        ExtPath closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("Closest",closestFileByLocation);   
//        LocationAPI.getInstance().putByLocation(file, path);
//        closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("folder",LocationAPI.getInstance().existByLocation(folder));
//        Log.write("Closest",closestFileByLocation);
//        Log.write("file",LocationAPI.getInstance().existByLocation(file));
//        LocationAPI.getInstance().removeByLocation(file);
//        closestFileByLocation = LocationAPI.getInstance().getFileByLocation(file);
//        Log.write("file",LocationAPI.getInstance().existByLocation(file));
//        Log.write("Closest",closestFileByLocation);
//        Log.println(1,2,3);
//            Thread t = new Thread(TaskFactory.getInstance().populateRecursiveParallel(MC.currentDir,FileManagerLB.DEPTH));
//            t.start();
            TaskFactory.getInstance().populateRecursiveParallelContained(MC.currentDir, 4);
        Log.print("END TEST");
    }

    
    public void openCustomDir() {
        changeToCustomDir(fileAddress.field.getText().trim());
    }
    private void changeToCustomDir(String possibleDir){
        try{
            if(possibleDir.equals(FileManagerLB.ROOT_NAME)||possibleDir.isEmpty()){
                changeToDir(FileManagerLB.ArtificialRoot).start();
            } 
            else{
                ExtFolder fileAndPopulate = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(possibleDir);
                if(!MC.currentDir.equals(fileAndPopulate)){
                    this.changeToDir(fileAndPopulate).start();
                }else{
                    update();
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
    public Thread changeToDir(ExtFolder dir){
        return new Thread( ()->{
            
            TaskFactory.getInstance().populateRecursiveParallelContained(dir,FileManagerLB.DEPTH);
//            t.start();
            if(!localSearch.getText().isEmpty()){
                localSearch.clear();
            }
            Platform.runLater(() ->{
                MC.changeDirTo(dir);
                update();  
            });
              
            
        });

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
                }else if(!MC.currentDir.isNotWriteable() && !MC.currentDir.isAbsoluteRoot.get()){
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
        ObservableList<ExtPath> newList = FXCollections.observableArrayList();
        deq.forEach(action ->{
            action.cancel(true);
        });
        deq.clear();
        final ExtTask asynchronousSortTask = extTableView.asynchronousSortTask(newList);
        Thread toThread = asynchronousSortTask.toThread();
        ExtFolder folderInitiated = MC.currentDir;
        ExtTask r = new ExtTask() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() ->{
                    extTableView.saveSortPrefereces();
                    toThread.start();                    
                });
                
                if(canceled.get()){
                    Log.print("Canceled from task before start");
                    return null;
                }

                SimpleBooleanProperty can = new SimpleBooleanProperty(canceled.get());
                can.bind(canceled);
                folderInitiated.update(newList,can);
                if(canceled.get()){
                    Log.print("Canceled from task");
                    return null;
                }
                newList.setAll(folderInitiated.files.values());
                String lookFor = localSearch.getText().trim();
                if(!lookFor.isEmpty()){
                    ArrayList<ExtPath> list = new ArrayList<>();
                    newList.forEach(item ->{
                        ExtPath path = (ExtPath) item;
                        String name = path.propertyName.get();
                        if(ExtStringUtils.containsIgnoreCase(name,lookFor)){
                            list.add( path);
                        }
                    });
                    newList.setAll(list);
                }            
                return null;
            }
        };
        deq.addFirst(r);
        r.setOnCancelled(event ->{   
            Log.print("Actually canceled");
        });
        r.setOnDone(event ->{
            asynchronousSortTask.setOnDone(handle ->{
                if(r.canceled.get()){
                    return;
                }
                final int viewSize = extTableView.table.getItems().size();
                final int neededSize = newList.size();
                Log.print("View size",viewSize,"Needed size",neededSize); 
                if(viewSize!=neededSize){
                    Platform.runLater(() ->{
                        extTableView.updateContentsAndSort(newList);
                    });
                }
            });
            asynchronousSortTask.cancel(); 
        });
        localSearchExecutor.submit(r);
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
    
    
    
    
    private void handleOpen(ExtPath file){
        if(file instanceof ExtFolder){
            Log.print("Change to dir "+file.getAbsoluteDirectory());
            changeToDir((ExtFolder) file).start();
        }else {
                            
            try{
                if(file.getIdentity().equals(Enums.Identity.LINK)){
                    ExtLink link = (ExtLink) file;
                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                    if(link.isPointsToDirectory()){
                        changeToDir((ExtFolder) LocationAPI.getInstance().getFileIfExists(location)).start();
                    }else{
                        DesktopApi.open(LocationAPI.getInstance().getFileIfExists(location).toPath().toFile());
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
        tableDragContextMenu = new ContextMenu();
        tableContextMenu = new ContextMenu();
        markedContextMenu = new ContextMenu();
        searchContextMenu = new ContextMenu();
        linksContextMenu = new ContextMenu();
        errorContextMenu = new ContextMenu();
        
        
        Menu submenuCreate = new Menu("Create...");
        submenuCreate.getItems().setAll(
            CosmeticsFX.simpleMenuItem("Create New Folder",
                event -> {
                    Log.print("Create new folder");
                    try {
                        ExtPath createNewFolder = MC.createNewFolder();
                        ViewManager.getInstance().newRenameDialog(MC.currentDir, createNewFolder);
                        MainController.this.update();
                    }catch (Exception ex) {
                        ErrorReport.report(ex);
                    }
                }, MC.isVirtual.not()),
            CosmeticsFX.simpleMenuItem("Create New File",
                event -> {
                    Log.print("Create new file");
                    try {
                        ExtPath createNewFile = MC.createNewFile();
                        ViewManager.getInstance().newRenameDialog(MC.currentDir, createNewFile);
                        MainController.this.update();
                    }catch (IOException ex) {
                        ErrorReport.report(ex);
                    }
                }, MC.isVirtual.not()));       
        
        searchContextMenu.getItems().setAll(CosmeticsFX.simpleMenuItem("Go to",
                event ->{
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
                }, Bindings.size(searchView.getSelectionModel().getSelectedItems()).isEqualTo(1)),
            CosmeticsFX.simpleMenuItem("Mark selected",
                event -> {
                    Platform.runLater(()->{
                        ArrayDeque<String> list = new ArrayDeque<>(searchView.getSelectionModel().getSelectedItems());
                        FXTask markFiles = TaskFactory.getInstance().markFiles(list);
                        new Thread(markFiles).start();
                    });
                }, Bindings.size(searchView.getSelectionModel().getSelectedItems()).greaterThan(0))
        );
        
        
        
        errorContextMenu.getItems().setAll(
            CosmeticsFX.simpleMenuItem("Remove this entry",
                event -> {
                    errorLog.remove(errorView.getSelectionModel().getSelectedIndex());
                }, Bindings.size(errorView.getSelectionModel().getSelectedItems()).greaterThan(0)),
            CosmeticsFX.simpleMenuItem("Clean this list",
                event -> {
                    errorLog.clear();
                }, Bindings.size(errorView.getSelectionModel().getSelectedItems()).greaterThan(0))
        
        );
        
        linksContextMenu.getItems().setAll(
            CosmeticsFX.simpleMenuItem("Add current directory as link",
                event -> {
                    FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(),MC.currentDir);
                    if(!links.contains(link)){
                       links.add(link); 
                    }
                }, null),
            CosmeticsFX.simpleMenuItem("Remove this link",
                event -> {
                    links.remove(linkView.getSelectionModel().getSelectedIndex());
                }, Bindings.size(this.linkView.getSelectionModel().getSelectedItems()).greaterThan(0))
        );
        
        
        markedContextMenu.getItems().setAll(CosmeticsFX.simpleMenuItem("Clean this list",
                event -> {
                    markedView.getItems().clear();
                }, propertyMarkedSize.greaterThan(0)),
            CosmeticsFX.simpleMenuItem("Remove selected",
                event -> {
                    ObservableList selectedItems = markedView.getSelectionModel().getSelectedItems();
                    MainController.markedList.removeAll(selectedItems);
                }, propertyMarkedSize.greaterThan(0).and(Bindings.size(
                        markedView.getSelectionModel().getSelectedItems()).greaterThan(0))),
            CosmeticsFX.simpleMenuItem("Delete selected",
                event -> {
                    FXTask task = TaskFactory.getInstance().deleteFiles(markedView.getSelectionModel().getSelectedItems());
                    task.setTaskDescription("Delete selected marked files");
                    ViewManager.getInstance().newProgressDialog(task); 
                }, propertyMarkedSize.greaterThan(0).and(Bindings.size(
                        markedView.getSelectionModel().getSelectedItems()).greaterThan(0)))
        );
        
        
        
        tableDragContextMenu.getItems().setAll(CosmeticsFX.simpleMenuItem("Move here selected",
                event -> {
                    MainController.actionList.clear();
                    MainController.actionList.addAll(MainController.dragList);
                    FXTask task = TaskFactory.getInstance().moveFiles(MainController.actionList,MC.currentDir);
                    task.setTaskDescription("Move Dragged files");
                    ViewManager.getInstance().newProgressDialog(task);  
                }, null),
            CosmeticsFX.simpleMenuItem("Copy here selected",
                event -> {
                    MainController.actionList.clear();
                    MainController.actionList.addAll(MainController.dragList);
                    FXTask task = TaskFactory.getInstance().copyFiles(MainController.actionList,MC.currentDir,null);
                    task.setTaskDescription("Copy Dragged files");
                    ViewManager.getInstance().newProgressDialog(task); 
                }, null)
        );

        
        Menu submenuMarked = new Menu("Marked...");
        submenuMarked.getItems().setAll(CosmeticsFX.simpleMenuItem("Copy here marked", 
                event -> {
                    Log.print("Copy Marked");
                    FXTask task = TaskFactory.getInstance().copyFiles(markedList,MC.currentDir,null);
                    task.setTaskDescription("Copy marked files");
                    ViewManager.getInstance().newProgressDialog(task);
                }, propertyMarkedSize.greaterThan(0).and(MC.isVirtual.not())),
            CosmeticsFX.simpleMenuItem("Move here marked",
                event ->{
                    Log.print("Move Marked");
                    FXTask task = TaskFactory.getInstance().moveFiles(markedList,MC.currentDir);
                    task.setTaskDescription("Move marked files");
                    ViewManager.getInstance().newProgressDialog(task);
                }, propertyMarkedSize.greaterThan(0).and(MC.isVirtual.not())),
            CosmeticsFX.simpleMenuItem("Add to marked", 
                event -> {
                    selectedList.stream().forEach((file) -> {
                        TaskFactory.getInstance().addToMarked(file);
                    });  
                }, miDuplicateFinderFolder.disableProperty().not().and(propertySelectedSize.greaterThan(0))),
            CosmeticsFX.simpleMenuItem("Delete all marked", 
                event -> {
                    FXTask task = TaskFactory.getInstance().deleteFiles(markedList);
                    task.setTaskDescription("Delete marked files");
                    ViewManager.getInstance().newProgressDialog(task); 
                }, propertyMarkedSize.greaterThan(0)),
            CosmeticsFX.simpleMenuItem("Add Marked to Virtual Folder", 
                event -> {
                    Platform.runLater(()->{
                        MainController.markedList.forEach((f) -> {
                            MC.currentDir.files.put(f.propertyName.get(), f);
                        });
                        update();
                    });   
                }, propertyMarkedSize.greaterThan(0).and(writeableFolder).and(MC.isVirtual))
        );
        
        
        tableContextMenu.getItems().setAll(
            CosmeticsFX.simpleMenuItem("Open", 
                event -> {
                    handleOpen((ExtPath) tableView.getSelectionModel().getSelectedItem());
                }, propertySelectedSize.isEqualTo(1)),
            CosmeticsFX.simpleMenuItem("Open in new window", 
                event -> {
                    ViewManager.getInstance().newWindow((ExtFolder) tableView.getSelectionModel().getSelectedItem());
                }, selectedIsFolder),
                
            CosmeticsFX.simpleMenuItem("Rename",
                event -> {
                   rename(); 
                }, propertyRenameCondition),
            
            CosmeticsFX.simpleMenuItem("Delete",
                event -> {
                    delete();
                }, propertyDeleteCondition),
            CosmeticsFX.simpleMenuItem("Copy path",
                event -> {
                    String absolutePath = selectedList.get(0).getAbsolutePath();
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(absolutePath), null);
                }, propertyRenameCondition),
            CosmeticsFX.simpleMenuItem("Toggle Enable/Disable",
                event -> {
                    Platform.runLater(()->{
                        selectedList.stream().forEach(c ->{
                            c.isDisabled.setValue(c.isDisabled.not().get());
                        });
                    });
                }, propertyDeleteCondition),
            CosmeticsFX.simpleMenuItem("Create Virtual Folder",
                event -> {
                    Platform.runLater(()->{
                        VirtualFolder.createVirtualFolder();
                        update();
                    });
                }, propertyIsVirtualFolders),
            CosmeticsFX.simpleMenuItem("Remove selected from Virtual Folder",
                event -> {
                    Platform.runLater(()->{
                        ObservableList<ExtPath> selectedItems = tableView.getSelectionModel().getSelectedItems();
                        selectedItems.forEach((item) -> {
                            ExtPath remove = MC.currentDir.files.remove(item.propertyName.get());
                            if(remove instanceof ExtFolder){
                                ExtFolder folder = (ExtFolder) remove;
                                folder.files.clear();
                            }
                        });
                        update();
                    });
                }, MC.isVirtual.and(propertySelectedSize.greaterThan(0))),
                submenuCreate,
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

        tableView.setContextMenu(tableContextMenu);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(tableView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(tableView.getContextMenu());
        //TABLE VIEW ACTIONS
        
        

        
        tableView.setOnMousePressed((MouseEvent event) -> {
            hideAllContextMenus();
            
            if(event.isPrimaryButtonDown()){
                if(!tableView.getSelectionModel().isEmpty()){
                    if(event.getClickCount() >1){
                        ExtPath file = (ExtPath) tableView.getSelectionModel().getSelectedItem();
                        handleOpen(file);
                    }else{
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
                } 
            }else{
                selectedIsFolder.set(tableView.getSelectionModel().getSelectedItem() instanceof ExtFolder);
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
            if(MC.currentDir.isNotWriteable()){
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
            if(MC.currentDir.isNotWriteable()){
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
//            Log.print("Drag dropped");
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
            }else{
                Log.print("Drag list is empty");
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
        markedSize.textProperty().bind(size.asString());
        markedView.setContextMenu(markedContextMenu);
        markedView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(markedView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(markedView.getContextMenu());
        
        markedView.setOnDragDetected((MouseEvent event) -> {
            TaskFactory.dragInitWindowID = "MARKED";
            if(MC.currentDir.isNotWriteable()){
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
            if(MC.currentDir.isNotWriteable()){
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
            if(MC.currentDir.isNotWriteable()){
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
        searchView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(searchView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(searchView.getContextMenu());

        //***************************************
        //Link View
        
        linkView.setContextMenu(linksContextMenu);
        linkView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(linkView.getSelectionModel()));

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
                    changeToDir((ExtFolder) selectedItem.location).start();
                }
            }
        });
        
        //***************************************
        //Error View
        errorView.setItems(errorLog);
        errorView.setContextMenu(errorContextMenu);
        errorView.getContextMenu().getItems().add(CosmeticsFX.wrapSelectContextMenu(errorView.getSelectionModel()));
        CosmeticsFX.simpleMenuBindingWrap(errorView.getContextMenu());

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
                   
                   Log.print(unitSize);
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
        if(this.propertyDeleteCondition.get()){
            Log.print("Deleting");
            FXTask task = TaskFactory.getInstance().deleteFiles(selectedList);
            task.setTaskDescription("Delete selected files");
            ViewManager.getInstance().newProgressDialog(task);
        }
        
    }
    private void rename(){
        if(this.propertyRenameCondition.get()){
            Log.print("Invoke rename dialog");
            ExtPath path = (ExtPath)tableView.getSelectionModel().getSelectedItem();
            ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileOptimized(path.getParent(1));
            ViewManager.getInstance().newRenameDialog(parent,path);
        }
    }
}
