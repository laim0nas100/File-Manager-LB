/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import LibraryLB.Threads.TimeoutTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public class CosmeticsFX {
    public static class MenuTree{

        TreeMap<String,MenuTree> leafs;
        LinkedList<String> myMapping;
        MenuItem data;
        public boolean hidden;
        public MenuTree(MenuItem data,String...mapping){
            leafs = new TreeMap<>();
            myMapping = new LinkedList<>();
            myMapping.addAll(Arrays.asList(mapping));
            this.data = data;

        }
        public void addMenuItem(MenuItem data,String...mapping){
            MenuTree Leaf = new MenuTree(data,mapping);
            MenuTree correctLeaf = this;
            LinkedList<String> getToLeaf = new LinkedList<>();
            getToLeaf.addAll(Leaf.myMapping);
            String removeLast = getToLeaf.removeLast();
            for(String s:getToLeaf){
                correctLeaf = correctLeaf.leafs.get(s);
            }
            correctLeaf.leafs.put(removeLast, Leaf);

        }
        @Override
        public String toString(){
            String s = "";
            s+=this.myMapping.toString()+"item:"+data.getText()+"\n";
            for(MenuTree mp:this.leafs.values()){
                s+=mp.toString();
            }
            return s;
        }
        public ContextMenu constructContextMenu(){
            ContextMenu cm = new ContextMenu();

            for(MenuTree mapping:this.leafs.values()){
                MenuItem item = constructMenuFromTree(mapping);
                if(item!= null){
                    cm.getItems().add(item);
                }

            }
            return cm;
        }
        public void setHidden(boolean hidden,String...mapping){
            MenuTree correctLeaf = this;
            LinkedList<String> getToLeaf = new LinkedList<>();
            getToLeaf.addAll(Arrays.asList(mapping));
            for(String s:getToLeaf){
                correctLeaf = correctLeaf.leafs.get(s);
            }
            correctLeaf.hidden = hidden;

        }
        private MenuItem constructMenuFromTree(MenuTree leaf){
            if(leaf.hidden){
                return null;
            }
            if(leaf.leafs.isEmpty()){
                return leaf.data;
            }else{
                Menu menu = (Menu) leaf.data;
                for(MenuTree mapping:leaf.leafs.values()){
                    MenuItem item = constructMenuFromTree(mapping);
                    if(item!= null){
                        menu.getItems().add(item);
                    }
                }
                return menu;
            }
        }

    }
    public static class ExtTableView{
        public final long resizeTimeout = 500;
        public SimpleBooleanProperty recentlyResized;
        public TimeoutTask resizeTask = new TimeoutTask(resizeTimeout,10,()->{
            recentlyResized.set(false);
        });
        
        public ArrayList<SortType> sortTypes;
        public ArrayList<TableColumn> sortColumns;
        public int sortByColumn;
        public boolean sortable = true;
        public TableView table;
        
        public ExtTableView(TableView table){
            this.table = table; 
            defaultValues();
        }
        public ExtTableView(){
            defaultValues(); 
        }
        private void defaultValues(){
            sortTypes = new ArrayList<>();
            sortColumns = new ArrayList<>();
            sortByColumn = 0;
            recentlyResized = new SimpleBooleanProperty();            
        }
        public void prepareChangeListeners(){
            try{
                table.getColumns().forEach(col ->{
                   
                    TableColumn c = (TableColumn) col;
                    changeListener(c);
                    c.setPrefWidth(90);//optional
                    
                });
            }catch(Exception e){
                ErrorReport.report(e);
            }
        }
        private void changeListener(final TableColumn listerColumn) {
            listerColumn.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                    recentlyResized.set(true);
                    resizeTask.update();
                }
            });
        }
        public void saveSortPrefereces(){
            if(!this.sortable){
                return;
            }
            sortColumns = new ArrayList<>();
            sortTypes = new ArrayList<>();
            if (!table.getSortOrder().isEmpty()) {
                table.getSortOrder().forEach(ob->{
                    TableColumn col = (TableColumn) ob;
                    sortColumns.add(col);
                    sortTypes.add(col.getSortType());
                });
                
            }
        }
        public void setSortPreferences(){     
            if (!sortColumns.isEmpty()) {
                table.getSortOrder().clear();
                for(int i=0;i<sortColumns.size();i++){
                    table.getSortOrder().add(sortColumns.get(i));
                    sortColumns.get(i).setSortType(sortTypes.get(i));
                    sortColumns.get(i).setSortable(true);
                }
            }
        }
        public void updateContents(Collection collection){
            ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();
            table.setItems(FXCollections.observableArrayList(collection));
            //Workaround to update table
            for(Integer i:selectedIndices){
                table.getSelectionModel().select(i);
            }
            TableColumn get = (TableColumn) table.getColumns().get(0);
            get.setVisible(false);
            get.setVisible(true);
            
        }
        public void updateContentsAndSort(Collection collection){
            saveSortPrefereces();
            updateContents(collection);
            setSortPreferences();
        }
        public void selectInverted(){
            TableView.TableViewSelectionModel selectionModel = this.table.getSelectionModel();
            ObservableList<Integer> selected = selectionModel.getSelectedIndices();
            ArrayList<Integer> array = new ArrayList<>();
            array.addAll(selected);
            selectionModel.selectAll();
            array.stream().forEach((Integer index) -> {
                selectionModel.clearSelection(index);
            });
        }
    }
    
    public static void wrapSelectContextMenu(Control node){
        MultipleSelectionModel model;
        ObservableList items = FXCollections.observableArrayList();
        if(node instanceof ListView){
            ListView view = (ListView) node;
            model = view.getSelectionModel();
            Bindings.bindContent(items, view.getItems());
        }else if(node instanceof TableView){
            TableView view = (TableView) node;
            model = view.getSelectionModel();
            Bindings.bindContent(items, view.getItems());
        }else{
            return;
        }
        Menu select = new Menu("Select");
        select.visibleProperty().bind(Bindings.size(model.getSelectedItems()).greaterThan(0));
        
        MenuItem selectAll = new MenuItem("All");
        selectAll.setOnAction(eh -> {
            model.selectAll();
        });
        selectAll.visibleProperty().bind(model.selectionModeProperty().isEqualTo(SelectionMode.MULTIPLE));
        
        MenuItem selectInverted = new MenuItem("Invert selection");
        selectInverted.setOnAction(eh -> {
            selectInverted(model);
        });
        selectInverted.visibleProperty().bind(Bindings.size(model.getSelectedItems()).greaterThan(0).and(selectAll.visibleProperty()));
        
        
        
        MenuItem selectNone = new MenuItem("None");
        selectNone.setOnAction(eh -> {
            model.clearSelection();
        });
        select.getItems().setAll(selectAll,selectNone,selectInverted);
        
        node.getContextMenu().getItems().add(select);
        
    }
    public static void selectInverted(MultipleSelectionModel sm){
        ObservableList<Integer> selected = sm.getSelectedIndices();
        ArrayList<Integer> array = new ArrayList<>();
        array.addAll(selected);
        sm.selectAll();
        array.stream().forEach(sm::clearSelection);
    }

}
