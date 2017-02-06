/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import LibraryLB.ExtTask;
import filemanagerGUI.BaseController;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.SimpleTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.util.Callback;
import utility.PathStringCommands;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DuplicateFinderController extends BaseController{

    @FXML public TableView list;
    @FXML public Text correlationRatio;
    @FXML public Slider slider;
    @FXML public Button searchButton;
    @FXML public Text textRootFolder;
    @FXML public ProgressBar progressBar;
    @FXML public CheckBox checkUseHash;
    @FXML public Label labelProgress;
    private HashMap<String,Double> map = new HashMap<>();
    
    private Double ratio;
    private MenuTree menuTree;
    private ExtFolder root;
    private ExtTask task;
    public void beforeShow(String title,ExtFolder root) {
        task = SimpleTask.temp();
        super.beforeShow(title);
        this.root = root;
        
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        textRootFolder.setText(root.getAbsoluteDirectory());
        this.labelProgress.textProperty().bind(this.progressBar.progressProperty().multiply(100).asString("%1$.2f").concat("%"));
        correlationRatio.textProperty().bind(slider.valueProperty().divide(100).asString("%1.3f"));
        TableColumn<SimpleTableItem, String> nameCol1 = new TableColumn<>("Name 1");
        nameCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f1.getName(true));
            }
        });
        nameCol1.setSortType(TableColumn.SortType.ASCENDING);
        TableColumn<SimpleTableItem, String> nameCol2 = new TableColumn<>("Name 2");
        nameCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f2.getName(true));
            }
        });        
        TableColumn<SimpleTableItem, String> pathCol1 = new TableColumn<>("Path 1");
        pathCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f1.getPath());
            }
        });
        TableColumn<SimpleTableItem, String> pathCol2 = new TableColumn<>("Path 2");
        pathCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f2.getPath());
            }
        });
        TableColumn<SimpleTableItem, String> ratioCol = new TableColumn<>("Ratio");
        ratioCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                String str = cellData.getValue().ratio+"";
                return new SimpleStringProperty(str.substring(0, Math.min(5, str.length())));
            }
        });
        
        list.getColumns().setAll(nameCol1,pathCol1,nameCol2,pathCol2,ratioCol);
    }
    @Override
    public void afterShow(){
        menuTree = new MenuTree(null);
        MenuItem markPath1 = new MenuItem("Mark Path 1");
        markPath1.setOnAction(eh ->{
            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
            for(Object ob:selectedItems){
                SimpleTableItem item = (SimpleTableItem) ob;
                TaskFactory.getInstance().addToMarked(LocationAPI.getInstance().getFileUnsecure(item.f1.getPath()));
            }
        });
        MenuItem markPath2 = new MenuItem("Mark Path 2");
        markPath2.setOnAction(eh ->{
            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
            for(Object ob:selectedItems){
                SimpleTableItem item = (SimpleTableItem) ob;
                TaskFactory.getInstance().addToMarked(LocationAPI.getInstance().getFileUnsecure(item.f2.getPath()));
            }
        });
        menuTree.addMenuItem(markPath1, markPath1.getText());
        menuTree.addMenuItem(markPath2, markPath2.getText());
        this.list.setContextMenu(menuTree.constructContextMenu());
    }
    public void search(){
        ratio = slider.valueProperty().divide(100).get();
        Platform.runLater(()->{
            cancel();
            list.getItems().clear();
            List synchronizedList = Collections.synchronizedList(list.getItems());
            ArrayList<PathStringCommands> array = new ArrayList<>();
            root.getListRecursive().stream().forEach(item->{
                array.add(new PathStringCommands(item.getAbsolutePath()));
            });
            if(this.checkUseHash.selectedProperty().get()){
                task = TaskFactory.getInstance().duplicateFinderTask(array, ratio,synchronizedList,map);
            }else{
                task = TaskFactory.getInstance().duplicateFinderTask(array, ratio,synchronizedList,null);

            }
            this.progressBar.progressProperty().bind(task.progressProperty());
            
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start(); 
        });
        
    }

    @Override
    public void update() {
    }
    public void cancel(){
        if(task!=null){
            task.cancel();
        }
    }
    public static class SimpleTableItem{
        public PathStringCommands f1;
        public PathStringCommands f2;
        public double ratio;
        public SimpleTableItem(PathStringCommands file1, PathStringCommands file2,double ratio){
            f1 = file1;
            f2 = file2;
            this.ratio = ratio;
        }
    }
  

    @Override
    public void exit() {
        cancel();
        super.exit(); //To change body of generated methods, choose Tools | Templates.
    }


}
