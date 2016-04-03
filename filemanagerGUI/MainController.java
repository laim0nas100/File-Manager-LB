/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

//import filemanagerLogic.ExtFolder;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.ManagingClass;
import filemanagerLogic.TaskFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import static filemanagerGUI.FileManagerLB.FolderForDevices;
import static filemanagerGUI.FileManagerLB.links;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.fileStructure.ExtLink;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utility.DesktopApi;
import utility.FavouriteLink;
import utility.Finder;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends BaseController{
    
    
    
    @FXML public TreeTableView treeTableView;
    @FXML public TreeView treeView;
    @FXML public AnchorPane left;
    @FXML public FlowPane flowView;
    @FXML public ScrollPane flowViewScroll;
    @FXML public CheckMenuItem autoClose;
    
    @FXML public TableView tableView;
    private ArrayList< TableColumn<ExtFile, String>> columns;
    private ObservableList<ExtFile> selectedList = FXCollections.observableArrayList();
    
    @FXML public ListView listView;
    @FXML public ListView linkView;
    
    
    @FXML public Label itemCount;
    @FXML public ListView searchView;
    @FXML public TextField searchField;
    
    @FXML public TextField currentDirText;
    @FXML public Button buttonPrev;
    @FXML public Button buttonParent;
    @FXML public Button buttonForw;
    @FXML public Button buttonGo;

        
    private ContextMenu tableContextMenu;
    private ContextMenu listContextMenu;
    private ContextMenu tableDragContextMenu;
    private ContextMenu searchContextMenu;
    private ContextMenu linksContextMenu;
    private MenuItem[] contextMenuItems;
    private Menu submenuMarked;
    private Menu submenuCreate;
    
    private ManagingClass MC;
    private Finder finder;
    
    private int currentView;
    
    
    public void setUp(String title,ExtFolder root,ExtFolder currentDir){
        super.setUp(title);
        autoClose.selectedProperty().bindBidirectional(ViewManager.getInstance().autoCloseProgressDialogs);
        contextMenuItems = new MenuItem[14];
        tableDragContextMenu = new ContextMenu();
        tableContextMenu = new ContextMenu();
        listContextMenu = new ContextMenu();
        searchContextMenu = new ContextMenu();
        linksContextMenu = new ContextMenu();
        columns = new ArrayList<>();
        finder = new Finder("",this.searchView.getItems());
       
        MC = new ManagingClass(root);
        
        
        
        LOAD(); 
        
        this.changeToDir(currentDir);
    }
    public void mountDirectory(){
        ViewManager.getInstance().newMountDirectoryDialog();
    }
    public void reMount(){
        FileManagerLB.remount();
    }
    @Override
    public void exit(){ 
        System.out.println("Closing internally " + title);
        ViewManager.getInstance().closeWindow(title); 
    }

    public void setTableView(){
        currentView = 0;
        tableView.setItems(MC.getCurrentContents());
        tableView.getColumns().setAll(columns);
        tableView.getSortOrder().add(columns.get(0));
        
        //ViewManager.getInstance().setTableView(title, tableView);
        tableView.setVisible(true);
    }
    public void setFlowView(){
        currentView = 1;
        Collection<Node> list = new ArrayList<>();
        for(ExtFile file:MC.getCurrentContents()){
            String name = file.getName();
            
            Node node = new Button(name);
            node.setOnMouseReleased((MouseEvent event) ->{
                if(file.getIdentity().equals("folder")){
                    changeToDir((ExtFolder) file);
                }
            
            });
            
            list.add(node);
            
            
        }
        flowView.getChildren().setAll(list);

        ViewManager.getInstance().setFlowView(title, flowViewScroll,flowView);

        flowView.setVisible(true);
    }
    public void updateCurrentView(){

            if(MC.currentDir.isAbsoluteRoot()){
                currentDirText.setText("MOUNT POINT");
            }else{
                currentDirText.setText(MC.currentDir.getAbsolutePath());
            }
            MC.currentDir.update();
            Iterator<ExtFile> iterator = TaskFactory.getInstance().markedList.iterator();
            while(iterator.hasNext()){
                if(!Files.exists(iterator.next().toPath())){
                    iterator.remove();
                }
            }
            
            switch(currentView){
                case(0):{
                    setTableView();
                    break;
                }
                case(1):{
                    setFlowView();
                    break;
                }
            }
        
    }
    public void closeAllWindows(){
        ViewManager.getInstance().closeAllWindows();
    }
    public void createNewWindow(){
        ViewManager.getInstance().newWindow(FolderForDevices,MC.currentDir);
    }
    
    public void test(){
        Log.writeln("Selected list:");
        for(ExtFile file:TaskFactory.getInstance().markedList){
            Log.writeln(file.getAbsolutePath());
        }
        //ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.selectedList,MC.currentDir);
        //ViewManager.getInstance().newProgressDialog(task);
    }

    public void openCustomDir(){
        changeToCustomDir(currentDirText.getText());
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
       updateCurrentView();
    }
    public void search(){
        String pattern = this.searchField.getText();
        if(pattern.length()>1){
            finder.newTask(pattern);
            this.searchView.getItems().clear();
            if(!MC.currentDir.isAbsoluteRoot()){
                Platform.runLater(()-> {
                    try {
                        Files.walkFileTree(MC.currentDir.toPath(), finder);
                        itemCount.setText(finder.list.size()+"");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });   
            }else{
                Platform.runLater(()-> {
                    try {
                        for(ExtFile file:FolderForDevices.getFilesCollection()){
                            Files.walkFileTree(file.toPath(), finder);
                        }
                        itemCount.setText(finder.list.size()+"");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });   
            }
       
        }   
    }
    private void changeToCustomDir(String possibleDir){
        try{
            if(possibleDir.equals(MC.currentDir.getAbsolutePath())){
                updateCurrentView();
            }else if(Files.isDirectory(Paths.get(possibleDir))){
                    LocationInRoot location = new LocationInRoot(possibleDir);
                    if(!LocationAPI.getInstance().existByLocation(location)){
                    
                        ExtFolder folder = new ExtFolder(possibleDir);
                        Log.writeln("put by Location Recursive: "+location);
                        LocationAPI.getInstance().putByLocationRecursive(location, folder);
                    }
                    changeToDir((ExtFolder) LocationAPI.getInstance().getFileByLocation(location));
            }
        } catch (Exception ex) {}
    }
    private Stage getStage(){
        return ViewManager.getInstance().windows.get(this.title).getStage();
    }
    private void hideAllContextMenus(){
        tableContextMenu.hide();
        listContextMenu.hide();
        tableDragContextMenu.hide();
    }
    private void setUpContextMenus(){
            //ContextMenu
        contextMenuItems[0] = new MenuItem("Create New Folder");
        contextMenuItems[0].setOnAction((eh) -> {
            Log.writeln("Create new folder");
            try {
                MC.createNewFolder();
                MainController.this.updateCurrentView();
            }catch (IOException ex) {
                //TODO error handling
            }
        });
        
        contextMenuItems[1] = new MenuItem("Rename");
        contextMenuItems[1].setOnAction((eh)->{
            Log.writeln("Invoke rename dialog");
            ExtFile path = (ExtFile)tableView.getSelectionModel().getSelectedItem();
            ExtFile fileCopy = new ExtFile(path.getAbsolutePath());
            ViewManager.getInstance().newRenameDialog(tableView.getItems(),fileCopy);
            //Invoke text input dialog
        });
        
        contextMenuItems[2] = new MenuItem("Delete");
        contextMenuItems[2].setOnAction((eh)->{
            Log.writeln("Deleting");
            ExtTask task = TaskFactory.getInstance().deleteFiles(selectedList);
            task.setTaskDescription("Delete selected files");
            ViewManager.getInstance().newProgressDialog(task);
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
                //TODO error handling
            }
        });
        contextMenuItems[6] = new MenuItem("Add to marked");
        contextMenuItems[6].setOnAction((eh)->{
            for(ExtFile file:selectedList){
                if(!TaskFactory.getInstance().markedList.contains(file)){
                    TaskFactory.getInstance().markedList.add(file);
                }  
            }  
        });
        contextMenuItems[7] = new MenuItem("Clean this list");
        contextMenuItems[7].setOnAction((e)->{
            TaskFactory.getInstance().markedList.clear();
        });
        
        contextMenuItems[8] = new MenuItem("Remove this item");
        contextMenuItems[8].setOnAction(eh ->{        
            ExtFile selectedItem = (ExtFile) listView.getSelectionModel().getSelectedItem();
            TaskFactory.getInstance().markedList.remove(selectedItem);
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
            TaskFactory.getInstance().prepareActionList(TaskFactory.getInstance().dragList);
            ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.getInstance().actionList,MC.currentDir);
            task.setTaskDescription("Move Dragged files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[11] = new MenuItem("Copy here selected");
        contextMenuItems[11].setOnAction(eh ->{        
            //Log.writeln("Copy Dragger");
            TaskFactory.getInstance().prepareActionList(TaskFactory.getInstance().dragList);
            ExtTask task = TaskFactory.getInstance().copyFiles(TaskFactory.getInstance().actionList,MC.currentDir);
            task.setTaskDescription("Copy Dragged files");
            ViewManager.getInstance().newProgressDialog(task);
        });
        contextMenuItems[12] = new MenuItem("Go to");
        contextMenuItems[12].setOnAction((ActionEvent eh) -> {
            String selectedItem = (String) MainController.this.searchView.getSelectionModel().getSelectedItem();
            Path path = Paths.get(selectedItem);
            if(Files.isDirectory(path)){
                changeToCustomDir(selectedItem);
            }else if(Files.isRegularFile(path)){
                changeToCustomDir(path.getParent().toString());
            }else if(Files.isSymbolicLink(path)){
                //TODO
            }
        });
        
        contextMenuItems[13] = new MenuItem("Add this directory");
        contextMenuItems[13].setOnAction(eh ->{
            String dir = MC.currentDir.getAbsolutePath();
            FavouriteLink link = new FavouriteLink(MC.currentDir.propertyName.get(),dir);
            links.add(link);
            
        });
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

        TableColumn<ExtFile, String> sizeCol = new TableColumn<>("Size ("+FileManagerLB.DataSize.sizename+")");
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                if(cellData.getValue().getIdentity().equals("folder")){
                    return new SimpleStringProperty("");
                }else{
                    return cellData.getValue().propertySize.divide(FileManagerLB.DataSize.size).asString();
                }
            }
        });
        
        columns.add(nameCol);
        columns.add(typeCol);
        columns.add(sizeCol);
        

        
        //TABLE VIEW ACTIONS
        
       

        tableView.setContextMenu(tableContextMenu);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        tableView.setOnMousePressed((MouseEvent event) -> {
            hideAllContextMenus();
            if (event.isSecondaryButtonDown()) {
                if (!MC.currentDir.isAbsoluteRoot()) {
                    int itemCount1 = tableView.getSelectionModel().getSelectedItems().size();
                    int markedSize = TaskFactory.getInstance().markedList.size();
                    if (markedSize==0 && itemCount1 >= 1) {
                        submenuMarked.getItems().setAll(
                                contextMenuItems[6]         //Add to marked
                                //contextMenuItems[3],      //Copy
                                //contextMenuItems[4],      //Move
                                //contextMenuItems[9]       //Delete marked
                        );
                    } else if (markedSize>0 && itemCount1 >= 1) {
                        submenuMarked.getItems().setAll(
                                contextMenuItems[6],      //Add to marked
                                contextMenuItems[3],      //Copy
                                contextMenuItems[4],      //Move
                                contextMenuItems[9]       //Delete marked
                        );
                    } else if (markedSize>0 && itemCount1 == 0) {
                        submenuMarked.getItems().setAll(
                                //contextMenuItems[6]         //Add to marked
                                contextMenuItems[3],      //Copy
                                contextMenuItems[4],      //Move
                                contextMenuItems[9]       //Delete marked
                        );
                    }
                    if (itemCount1 == 1) {
                        tableContextMenu.getItems().setAll(
                                submenuCreate,
                                contextMenuItems[1],    //Rename dialog
                                contextMenuItems[2]     //Delete dialog
                        );
                    } else if (itemCount1 > 1) {
                        tableContextMenu.getItems().setAll(
                                submenuCreate,
                                //contextMenuItems[1],  //Rename dialog
                                contextMenuItems[2]     //Delete dialog
                        );
                    } else {
                        tableContextMenu.getItems().setAll(
                                submenuCreate
                                //contextMenuItems[1],      //Rename dialog
                                //contextMenuItems[2]     //Delete dialog
                        );
                    }
                    tableContextMenu.getItems().add(submenuMarked);
                }
            } else if(event.isPrimaryButtonDown()){
                if(!tableView.getSelectionModel().isEmpty()){
                    if(event.getClickCount() >1){
                        ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                        if(file.getIdentity().equals("folder")){
                            changeToDir((ExtFolder) file);
                        }else {
                            
                            try{
                                if(file.getIdentity().equals("link")){
                                    ExtLink link = (ExtLink) file.getTrueForm();
                                    LocationInRoot location = new LocationInRoot(link.getTargetDir());
                                    if(link.isPointsToDirectory()){
                                        changeToDir((ExtFolder) LocationAPI.getInstance().getFileByLocation(location));
                                    }else{
                                        DesktopApi.open(LocationAPI.getInstance().getFileByLocation(location));
                                    }
                                }else if(file.getIdentity().equals("file")){
                                    DesktopApi.open(file);
                                }
                            }catch(Exception x){
                                //TODO error handling
                            }
                        }
                        
                    }else{
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
                } 
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
        listView.setItems(TaskFactory.getInstance().markedList);
        listView.setContextMenu(listContextMenu);
        listView.setOnMousePressed((eh) ->{ 
            if((listView.getSelectionModel().getSelectedItem()!= null)&&(TaskFactory.getInstance().markedList.size()>0)){
                    listContextMenu.getItems().setAll(
                        contextMenuItems[7],
                        contextMenuItems[8]
                    );
            }else{
               listContextMenu.getItems().clear();
            }
        });
        listView.setOnDragDetected((MouseEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            // drag was detected, start drag-and-drop gesture
            TaskFactory.getInstance().dragList = listView.getSelectionModel().getSelectedItems();
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

        listView.setOnDragOver((DragEvent event) -> {
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

        listView.setOnDragDropped((DragEvent event) -> {
            if(MC.currentDir.isAbsoluteRoot()){
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (!TaskFactory.getInstance().dragList.isEmpty()) {
                for(ExtFile f:TaskFactory.getInstance().dragList){
                    if(!TaskFactory.getInstance().markedList.contains(f)){
                        TaskFactory.getInstance().markedList.add(f);
                    }
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        searchView.getItems().setAll(finder.list);
        searchView.setContextMenu(searchContextMenu);
        
        //SOME ITEMS
        
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
                            setText(t.propertyName.get());
                            if(!t.propertyName.get().equals("ROOT")){
                                setTooltip(t.getToolTip());
                            }
                        }
                    }
                };
                
                return cell;
            }
        });
        linkView.setOnMousePressed((MouseEvent eh) ->{
            if(!MC.currentDir.isAbsoluteRoot()){
                linksContextMenu.getItems().setAll(contextMenuItems[13]);
            }else{
                linksContextMenu.getItems().clear();
            }
            if(eh.isPrimaryButtonDown()){
                if(eh.getClickCount()>1){
                    FavouriteLink selectedItem = (FavouriteLink) linkView.getSelectionModel().getSelectedItem();
                    changeToCustomDir(selectedItem.getDirectory());
                }
            }
        });
        
    }
    private void LOAD(){
        setUpContextMenus();
        setUpTableView();
        setUpListViews();
        

        getStage().getScene().setOnMouseClicked((eh) -> {
            listView.getSelectionModel().clearSelection();
            tableView.getSelectionModel().clearSelection();
            searchView.getSelectionModel().clearSelection();
        });
    }
}
