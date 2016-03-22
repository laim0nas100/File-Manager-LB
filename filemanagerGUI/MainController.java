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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import static filemanagerLogic.TaskFactory.selectedList;
import java.util.Vector;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.scene.input.KeyEvent;
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
    //@FXML public Label sampleLabel;
    
    @FXML public TextField currentDirText;
    @FXML public Button buttonPrev;
    @FXML public Button buttonParent;
    @FXML public Button buttonForw;
    @FXML public Button buttonGo;
    @FXML public ContextMenu contextMenu;
    
    private ObservableList columns;
    
    private int currentView;
    
    
    public void setUp(String title,ExtFolder root,ExtFolder currentDir){
        selectedList = new ReadOnlyListWrapper<>();
        MC = new ManagingClass(root);
        this.title = title;
        this.changeToNewDir(currentDir);
    }
    public void closeWindow(){ 
        System.out.println("Closing internally " + title);
        ViewManager.getInstance().closeWindow(title); 
    }

    public void setTableView(){
        currentView = 0;
        TableColumn<ExtFile, String> nameCol = new TableColumn<>("Files");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ExtFile, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ExtFile, String> cellData) {
                return cellData.getValue().getPropertyName();
            }
        });
        
        tableView.setItems(MC.getCurrentContents());
        tableView.getColumns().setAll(nameCol);
        tableView.setOnMousePressed((MouseEvent event) -> {
            if(!tableView.getSelectionModel().isEmpty()){
                if(event.isPrimaryButtonDown()){
                    if(event.getClickCount() >1){   

                            ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                            if(file.getIdentity().equals("folder")){
                                changeToNewDir((ExtFolder) file);
                                updateCurrentView();
                            }else if(file.getIdentity().equals("file")){
                                try {
                                    file.doOnOpen();
                                } catch (IOException ex) {
                                    //TODO error handling
                                }                    
                            }
                        
                    }else{
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
                }
            }
        });
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setOnKeyPressed((KeyEvent keyEvent)->{
            if(keyEvent.isControlDown()){
                 if(!tableView.getSelectionModel().isEmpty()){
                        selectedList = tableView.getSelectionModel().getSelectedItems();
                    }
            }
        });
        
        tableView.setOnContextMenuRequested((eh)->{
            if(!tableView.getSelectionModel().isEmpty()){
                //Single item options
                if(selectedList.size()==1){
                    ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                    if(file.getIdentity().equals("folder")){
                        Log.write("Found folder",file.getAbsolutePath());
                    }else if(file.getIdentity().equals("file")){
                        try {
                            file.doOnOpen();
                        } catch (IOException ex) {
                            //TODO error handling
                        }                    
                    }
                //Multiple item options
                }else{
                }
           }
        });
        
        
        
        ViewManager.getInstance().setTableView(title, tableView);
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
                    changeToNewDir((ExtFolder) file);
                }
            
            });
            
            list.add(node);
            
            
        }
        flowView.getChildren().setAll(list);

        ViewManager.getInstance().setFlowView(title, flowViewScroll,flowView);

        flowView.setVisible(true);
    }
    public void updateCurrentView(){
        if(MC.currentDir.isRoot()){
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
        for(ExtFile file:TaskFactory.selectedList){
            Log.writeln(file.getAbsolutePath());
        }
        ExtTask task = TaskFactory.getInstance().moveFiles(TaskFactory.selectedList,MC.currentDir);
        ViewManager.getInstance().newProgressDialog(task);
    }
    public void changeToNewDir(ExtFolder dir){
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
                    ExtFolder folder = (ExtFolder) LocationAPI.getInstance().getFileByLocation(FolderForDevices, location);
                    changeToNewDir(folder);


            }
        } catch (Exception ex) {}
               
        
        updateCurrentView();
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
    
 
}
