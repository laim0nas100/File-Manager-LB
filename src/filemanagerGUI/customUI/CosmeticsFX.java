/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import LibraryLB.Log;
import LibraryLB.Threads.ExtTask;
import LibraryLB.Threads.TimeoutTask;
import filemanagerGUI.ViewManager;
import filemanagerLogic.LocationAPI;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtFolder;
import filemanagerLogic.fileStructure.ExtPath;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
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
            //Work-around to update table
            selectedIndices.forEach((i) -> {
                table.getSelectionModel().select(i);
            });
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
            CosmeticsFX.selectInverted(table.getSelectionModel());
        }
    }
    
    public static void wrapSelectContextMenu(ContextMenu menu,MultipleSelectionModel model){
        
        BooleanBinding greaterThan1 = Bindings.size(model.getSelectedItems()).greaterThan(0);
        Menu select = new Menu("Select");
        select.visibleProperty().bind(greaterThan1);
        
        MenuItem selectAll = new MenuItem("All");
        selectAll.setOnAction(eh -> {
            model.selectAll();
        });
        selectAll.visibleProperty().bind(model.selectionModeProperty().isEqualTo(SelectionMode.MULTIPLE).and(greaterThan1));
        
        MenuItem selectInverted = new MenuItem("Invert selection");
        selectInverted.setOnAction(eh -> {
            selectInverted(model);
        });
        selectInverted.visibleProperty().bind(selectAll.visibleProperty());
        
        MenuItem selectNone = new MenuItem("None");
        selectNone.setOnAction(eh -> {
            model.clearSelection();
        });
        selectNone.visibleProperty().bind(greaterThan1);
        select.getItems().setAll(selectAll,selectNone,selectInverted);
        menu.getItems().add(select);
        
    }
    public static void selectInverted(MultipleSelectionModel sm){
        ArrayDeque<Integer> array = new ArrayDeque<>(sm.getSelectedIndices());
        sm.selectAll();
        array.stream().forEach(sm::clearSelection);
    }
    public static MenuItem simpleMenuItem(String name,EventHandler onAction, BooleanExpression visibleProperty){
        MenuItem item = new MenuItem(name);
        item.setOnAction(onAction);
        if(visibleProperty !=null){
            item.visibleProperty().bind(visibleProperty);
        }
        return item;
        
    }
    public static void simpleMenuBindingWrap(Menu menu){
        final ArrayDeque<BooleanExpression> list = new ArrayDeque<>();
        menu.getItems().forEach(item ->{
            if(item instanceof Menu){
                simpleMenuBindingWrap((Menu) item);
            }
            list.add(item.visibleProperty());
        });
        if(list.isEmpty()){
            return;
        }
        if(list.size()==1){
            menu.visibleProperty().bind(list.pollFirst());
        }else if(list.size()>1){
            BooleanBinding bind = list.pollFirst().or(list.pollFirst());
            for(BooleanExpression b:list){
                bind = bind.or(b);
            }
            menu.visibleProperty().bind(bind);
        }
    }
    public static void simpleMenuBindingWrap(ContextMenu menu){
        menu.getItems().forEach(item ->{
            if(item instanceof Menu){
                simpleMenuBindingWrap((Menu) item);
            }
        }); 
    }
}
