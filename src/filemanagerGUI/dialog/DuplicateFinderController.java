/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import filemanagerLogic.ExtTask;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.util.Callback;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DuplicateFinderController extends BaseController{

    @FXML public TableView list;
    @FXML public Text correlationRatio;
    @FXML public ScrollBar scroll;
    @FXML public Button searchButton;
    @FXML public Text textRootFolder;
    @FXML public ProgressBar progressBar;
    private MenuTree menuTree;
    private ExtFolder root;
    public void beforeShow(String title,ExtFolder root) {
        super.beforeShow(title);
        this.root = root;
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        textRootFolder.setText(root.getAbsoluteDirectory());
        correlationRatio.textProperty().bind(scroll.valueProperty().divide(100).asString());
        TableColumn<SimpleTableItem, String> nameCol1 = new TableColumn<>("Name 1");
        nameCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return cellData.getValue().f1.propertyName;
            }
        });
        nameCol1.setSortType(TableColumn.SortType.ASCENDING);
        TableColumn<SimpleTableItem, String> nameCol2 = new TableColumn<>("Name 2");
        nameCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return cellData.getValue().f2.propertyName;
            }
        });

        
        TableColumn<SimpleTableItem, String> pathCol1 = new TableColumn<>("Path 1");
        pathCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f1.getAbsoluteDirectory());
            }
        });
        TableColumn<SimpleTableItem, String> pathCol2 = new TableColumn<>("Path 2");
        pathCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SimpleTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<SimpleTableItem, String> cellData) {
                return new SimpleStringProperty(cellData.getValue().f2.getAbsoluteDirectory());
            }
        });
        
        list.getColumns().setAll(nameCol1,pathCol1,nameCol2,pathCol2);
    }
    @Override
    public void afterShow(){
        menuTree = new MenuTree(null);
        MenuItem markPath1 = new MenuItem("Mark Path 1");
        markPath1.setOnAction(eh ->{
            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
            for(Object ob:selectedItems){
                SimpleTableItem item = (SimpleTableItem) ob;
                TaskFactory.getInstance().addToMarked(item.f1.getAbsoluteDirectory());
            }
        });
        MenuItem markPath2 = new MenuItem("Mark Path 2");
        markPath2.setOnAction(eh ->{
            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
            for(Object ob:selectedItems){
                SimpleTableItem item = (SimpleTableItem) ob;
                TaskFactory.getInstance().addToMarked(item.f2.getAbsoluteDirectory());
            }
        });
        menuTree.addMenuItem(markPath1, markPath1.getText());
        menuTree.addMenuItem(markPath2, markPath2.getText());
        this.list.setContextMenu(menuTree.constructContextMenu());
    }
    public void search(){
        searchButton.setDisable(true);
        list.getItems().clear();
        ExtTask task = TaskFactory.getInstance().duplicateFinderTask(root, scroll.valueProperty().divide(100).get(),list.getItems());
        task.setOnSucceeded(eh ->{
            
            searchButton.setDisable(false);
            this.progressBar.setProgress(1);
        });
        this.progressBar.progressProperty().bind(task.progressProperty());
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void update() {
    }
    public static class SimpleTableItem{
        public ExtFile f1;
        public ExtFile f2;
        public SimpleTableItem(ExtFile file1, ExtFile file2){
            f1 = file1;
            f2 = file2;
        }
    }

}
