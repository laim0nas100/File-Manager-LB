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
import filemanagerLogic.LocationAPI;
import filemanagerLogic.fileStructure.ExtLink;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;
import utility.DesktopApi;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends BaseController{
    /**
     * Initializes the controller class.
     */
    private ManagingClass MC;
    
    
    //@FXML private MenuItem menuItemClose;
    @FXML public TreeTableView treeTableView;
    @FXML public TreeView treeView;
    @FXML public AnchorPane left;
    @FXML public FlowPane flowView;
    @FXML public ScrollPane flowViewScroll;
    
    
    @FXML public TableView tableView;
    private ArrayList< TableColumn<ExtFile, String>> columns;
    private ObservableList<ExtFile> selectedList = FXCollections.observableArrayList();
    
    
    
    @FXML public TextField currentDirText;
    @FXML public Button buttonPrev;
    @FXML public Button buttonParent;
    @FXML public Button buttonForw;
    @FXML public Button buttonGo;

        
    private ContextMenu contextMenu;
    private MenuItem[] contextMenuItems;
    
    
    private int currentView;
    
    
    public void setUp(String title,ExtFolder root,ExtFolder currentDir){
        contextMenuItems = new MenuItem[10];
        contextMenu = new ContextMenu();
        loadDefaults();
        
        MC = new ManagingClass(root);
        this.title = title;
        this.changeToDir(currentDir);
        
    }
    
    public void reMount(){
        FileManagerLB.remount();
    }
    public void closeWindow(){ 
        System.out.println("Closing internally " + title);
        ViewManager.getInstance().closeWindow(title); 
    }

    public void setTableView(){
        currentView = 0;
        tableView.setItems(MC.getCurrentContents());
        tableView.getSortOrder().add(columns.get(0));
        tableView.getColumns().setAll(columns);
       
        
        
        
        
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
        for(ExtFile file:TaskFactory.markedList){
            Log.writeln(file.getAbsolutePath());
        }
        //ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.selectedList,MC.currentDir);
        //ViewManager.getInstance().newProgressDialog(task);
    }
    public void changeToDir(ExtFolder dir){
       MC.changeDirTo(dir);
       updateCurrentView();
    }
    
    public void openCustomDir(){
        try{
            String possibleDir = currentDirText.getText();
            if(possibleDir.equals(MC.currentDir.getAbsolutePath())){
                updateCurrentView();
            }else if(Files.isDirectory(Paths.get(possibleDir))){
                    LocationInRoot location = new LocationInRoot(possibleDir);
                    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(location);
                    changeToDir(folder);


            }
        } catch (Exception ex) {}
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
    
    private void loadDefaults(){
        //TABLE VIEW SETUP
        
        columns = new ArrayList<>();
        
        TableColumn<ExtFile, String> nameCol = new TableColumn<>("Files");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                return cellData.getValue().getPropertyName();
            }
        });
        nameCol.setSortType(TableColumn.SortType.ASCENDING);
        columns.add(nameCol);
        
        
        //ContextMenu
        contextMenuItems[0] = new MenuItem();
        contextMenuItems[0].setText("Create New Folder");
        contextMenuItems[0].setOnAction((event)->{
            Log.writeln("Create new folder");
            try {
                MC.createNewFolder();
                this.updateCurrentView();
                
            } catch (IOException ex) {
               //TODO error handling
            }
        });
        
        contextMenuItems[1] = new MenuItem();
        contextMenuItems[1].setText("Rename");
        contextMenuItems[1].setOnAction((event)->{
            Log.writeln("Invoke rename dialog");
            //Invoke text input dialog
        });
        
        contextMenuItems[2] = new MenuItem();
        contextMenuItems[2].setText("Delete");
        contextMenuItems[2].setOnAction((event)->{
            Log.writeln("Deleting");
            ViewManager.getInstance().newProgressDialog(TaskFactory.getInstance().deleteFiles(selectedList));
        });
        
        contextMenuItems[3] = new MenuItem();
        contextMenuItems[3].setText("Copy Here");
        contextMenuItems[3].setOnAction((event)->{
            Log.writeln("Copy Marked");
            ViewManager.getInstance().newProgressDialog(TaskFactory.getInstance().copyFiles(TaskFactory.markedList,MC.currentDir));
        });
        contextMenuItems[4] = new MenuItem();
        contextMenuItems[4].setText("Move Here");
        contextMenuItems[4].setOnAction((e)->{
            Log.writeln("Move Marked");
            ViewManager.getInstance().newProgressDialog(TaskFactory.getInstance().moveFiles(TaskFactory.markedList,MC.currentDir));
        
        });
        contextMenuItems[5] = new MenuItem();
        contextMenuItems[5].setText("Create new file");
        contextMenuItems[5].setOnAction((e)->{
            Log.writeln("Create new file");
            //Invoke text input dialog
        });
        contextMenuItems[6] = new MenuItem();
        contextMenuItems[6].setText("Add to marked");
        contextMenuItems[6].setOnAction((e)->{
            TaskFactory.markedList.addAll(selectedList);
        });
        contextMenuItems[7] = new MenuItem();
        contextMenuItems[7].setText("Clean marked list");
        contextMenuItems[7].setOnAction((e)->{
            TaskFactory.markedList.clear();
        });
        
        Menu submenuCreate = new Menu();
        submenuCreate.setText("Create...");
        submenuCreate.getItems().addAll(contextMenuItems[0],contextMenuItems[5]);
        
        
        Menu submenuMarked = new Menu();
        submenuMarked.setText("Marked...");
        submenuMarked.getItems().addAll(contextMenuItems[3],contextMenuItems[4],contextMenuItems[6],contextMenuItems[7]);
        
        contextMenu.getItems().add(0, submenuCreate);
        contextMenu.getItems().add(1, contextMenuItems[1]);
        contextMenu.getItems().add(2, contextMenuItems[2]);
        contextMenu.getItems().add(submenuMarked);
        
        
        
        tableView.setContextMenu(contextMenu);
        //TABLE VIEW ACTIONS
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setOnMousePressed((MouseEvent event) -> {
            if(!tableView.getSelectionModel().isEmpty()){
                if(event.isPrimaryButtonDown()){
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
       
        /*
        tableView.setOnKeyPressed((KeyEvent keyEvent)->{
            if(keyEvent.isControlDown()){
                 if(!tableView.getSelectionModel().isEmpty()){
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
            }
        });
        */
        
    }
    
 
}
