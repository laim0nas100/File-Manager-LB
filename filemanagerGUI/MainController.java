/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

//import filemanagerLogic.ExtFolder;
import com.sun.glass.ui.Window;
import static filemanagerGUI.FileManagerLB.rootDirectory;
import filemanagerLogic.ExtFile;
import filemanagerLogic.ExtFolder;
import filemanagerLogic.ExtTask;
import filemanagerLogic.LocationInRoot;
import filemanagerLogic.ManagingClass;
import filemanagerLogic.TaskFactory;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Box;
import javafx.util.Callback;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderStroke;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class MainController extends BaseController{
    /**
     * Initializes the controller class.
     */
    public String title;
    private ManagingClass MC;
    
    
    //@FXML private MenuItem menuItemClose;
    @FXML public TreeTableView treeTableView;
    @FXML public TreeView treeView;
    @FXML public AnchorPane left;
    @FXML public FlowPane flowView;
    @FXML public ScrollPane flowViewScroll;
    @FXML public TableView tableView;
    @FXML public Label sampleLabel;
    
    @FXML public TextField currentDirText;
    @FXML public Button buttonPrev;
    @FXML public Button buttonParent;
    @FXML public Button buttonForw;
    @FXML public Button buttonGo;
    
    private ObservableList columns;
    
    private int currentView;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        System.out.println("Initilization from Controller <"+rootDirectory.getAbsolutePath()+">");
        MC = new ManagingClass(rootDirectory);
        
        
        
    }
    public void closeWindow(){ 
        
        System.out.println("Closing internally " + title);
        ViewManager.getInstance().closeWindow(title);
       
    }
    /*
    private TreeTableView makeTreeTableView(ExtFolder folder){
        TreeTableView view = new TreeTableView();
        final TreeItem<ExtFile> rootElement = new TreeItem<>(folder);
        rootElement.setExpanded(false);
        folder.
        
        Collection<ExtFile> list = folder.getFilesCollection();
        for(ExtFile file:list){
            TreeItem<String> element = new TreeItem<>(file.getName());
            rootElement.getChildren().add(element);
        }
        TreeTableColumn<String, String> column = new TreeTableColumn<>("Column");
        column.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {
        @Override public ObservableValue<String> call(CellDataFeatures<String, String> p) {
        return new ReadOnlyStringWrapper(p.getValue().getValue());
        }
        });
        return view;
    }
        */
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
            if(event.isPrimaryButtonDown() && event.getClickCount() >1){
                if(!tableView.getSelectionModel().isEmpty()){
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
                }
            }
        });
        tableView.setOnContextMenuRequested((eh)->{
           if(!tableView.getSelectionModel().isEmpty()){
                    ExtFile file = (ExtFile) tableView.getSelectionModel().getSelectedItem();
                    //tableView.getSelectionModel().getSelectionMode().
                    if(file.getIdentity().equals("folder")){
                        changeToNewDir((ExtFolder) file);
                    }else if(file.getIdentity().equals("file")){
                        try {
                            file.doOnOpen();
                        } catch (IOException ex) {
                            //TODO error handling
                        }                    
                    }
                }else{
               
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
        currentDirText.setText(MC.currentDir.getAbsolutePath());
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
        ViewManager.getInstance().newWindow();
    }
    public void test(){
        
       
        try {
            LocationInRoot location = new LocationInRoot(rootDirectory,new ExtFile("E:\\Test1\\TheEdenProject"));
            ExtFolder folder = (ExtFolder) MC.getFileByLocation(rootDirectory, location);

            System.out.println("DONE");
            System.out.println(folder.getAbsolutePath());
            folder.populateRecursive();
            ExtTask task = TaskFactory.getInstance().copyFiles(MC.prepareForCopy(folder.getListRecursive(),MC.rootDirectory.files.get("Dest")));
            System.out.println(task.getState());
            ViewManager.getInstance().newProgressDialog(task); 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public void setTreeListView(){
    
        //ViewManager.getInstance().setTreeTableView(title,treeTableView);
    }
    public void changeToNewDir(ExtFolder dir){
       MC.changeDirTo(dir);
       updateCurrentView();
    }
    
    public void openCustomDir(){
        String possibleDir = currentDirText.getText();
        if(possibleDir.equals(MC.currentDir.getAbsolutePath())){
            updateCurrentView();
        }else if(Files.isDirectory(Paths.get(possibleDir))){
            try {
                LocationInRoot location = new LocationInRoot(rootDirectory,new ExtFile(possibleDir));
                ExtFolder folder = (ExtFolder) MC.getFileByLocation(rootDirectory, location);
                changeToNewDir(folder);
            } catch (Exception ex) {
                updateCurrentView();
            }
            
        }else{
            updateCurrentView();
        }
    }
    
    public void changeToParent(){
        MC.changeToParent();
        updateCurrentView();
    }
    public void changeToPrevious(){
        
        updateCurrentView();
        
    }
    public void changeToForward(){
        
        updateCurrentView();
    }
    
    
}
