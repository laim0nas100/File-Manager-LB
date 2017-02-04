/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;
import LibraryLB.Parsing.Lexer;
import LibraryLB.Parsing.Literal;
import LibraryLB.Parsing.Token;
import filemanagerGUI.BaseController;
import filemanagerLogic.Enums;
import filemanagerLogic.TaskFactory;
import filemanagerLogic.fileStructure.ExtPath;
import filemanagerLogic.fileStructure.VirtualFolder;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;
import utility.ErrorReport;
import utility.ExtStringUtils;
import static utility.ExtStringUtils.simpleFormat;
import static utility.ExtStringUtils.trimEnd;
import utility.PathStringCommands;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class AdvancedRenameController extends BaseController {

    
@FXML public Tab tbSpecificRename;
@FXML public Tab tbNumerize;
    
@FXML public TextField tfStrReg;
@FXML public TextField tfReplaceWith;
@FXML public TextField tfFilter;
@FXML public TextField tfStartingNumber;
@FXML public TextField tfIncrement;


@FXML public TableView table;

@FXML public CheckBox useRegex;
@FXML public CheckBox showFullPath;
@FXML public CheckBox recursive;
@FXML public CheckBox includeFolders;
@FXML public CheckBox showOnlyDifferences;
@FXML public Button buttonApply;


private int startingNumber;
private int increment;
private LinkedList<TableItemObject> tableList;
private VirtualFolder virtual;

public void beforeShow(String title,VirtualFolder virtual){
    super.beforeShow(title);    
    this.setNumber();
    this.tableList = new LinkedList<>();
    this.virtual = virtual;
    Tooltip tp = new Tooltip();
    tp.setText("Name ="+PathStringCommands.fileName+", Name without extension ="+PathStringCommands.nameNoExt+
            ", Name extension only ="+PathStringCommands.extension+", Number (multi-digit if consecutive) ="+PathStringCommands.number);
    this.tfFilter.setTooltip(tp);
    ArrayList<TableColumn> columns = new ArrayList<>();
    TableColumn<TableItemObject, String> nameCol1 = new TableColumn<>("Current Name");
    TableColumn<TableItemObject, String> nameCol2 = new TableColumn<>("Rename To");
    TableColumn<TableItemObject, String> sizeCol = new TableColumn<>("Size");    
    TableColumn<TableItemObject, String> dateCol = new TableColumn<>("Last Modified");
    nameCol1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            String result;
            if(showFullPath.selectedProperty().get()){
                result = cellData.getValue().path1.getPath();
            }else{
                result = cellData.getValue().path1.getName(true);
            }
            return new SimpleStringProperty(result);
        }
    });
    nameCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            String result;
            if(showFullPath.selectedProperty().get()){
                result = cellData.getValue().path2.getPath();
            }else{
                result = cellData.getValue().path2.getName(true);
            }
            return new SimpleStringProperty(result);
        }
    });
    sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            return cellData.getValue().size.asString();
        }
    });
    dateCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
            return cellData.getValue().date;
        }
    });
    sizeCol.setComparator(ExtPath.COMPARE_SIZE_STRING);
    
    columns.add(nameCol1);
    columns.add(nameCol2);
    columns.add(sizeCol);
    columns.add(dateCol);
    this.table.getColumns().setAll(columns);

    this.table.getItems().addAll(tableList);
    updateLists();
}
public void updateLists(){
    
    ArrayList<ExtPath> array = new ArrayList<>();
    if(recursive.selectedProperty().get()){
        this.virtual.getListRecursive().stream().forEach(file->{
            array.add(file);
        });
    }else{
        this.virtual.getFilesCollection().stream().forEach(file->{
            array.add(file);
        });
    }
    tableList.clear();
    for(ExtPath s:array){
       tableList.add(new TableItemObject(s));
    }
    setTableItems(tableList);
    buttonApply.setDisable(true);
}
public void previewSetting(){
    tableList.clear();
    tableList.addAll(table.getItems());
    if(this.tbNumerize.isSelected()){
        String filter = this.tfFilter.getText();
        setNumber();
        long number = startingNumber;
        
        for(TableItemObject object:this.tableList){          
            try {
                
                object.newName(parseFilter(object.path1.getName(true), filter, number));
                number+=increment;
            } catch (Lexer.NoSuchLexemeException | Lexer.StringNotTerminatedException ex) {
                ErrorReport.report(ex);
            }
        }
    }else{
        String strRegex = this.tfStrReg.getText();
        String replacement =""+ this.tfReplaceWith.getText();
        if(useRegex.isSelected()){
            for(TableItemObject object:this.tableList){
                object.newName(ExtStringUtils.parseRegex(object.path1.getName(true), strRegex, replacement));
            }
        }else{
           for(TableItemObject object:this.tableList){
                object.newName(ExtStringUtils.parseSimple(object.path1.getName(true), strRegex, replacement)); 
           } 
        }
    }
    setTableItems(applyFilters(tableList));
    
    buttonApply.setDisable(false);
}
@Override
public void update(){
    updateLists();
    
}


public String parseFilter(String origName, String filter, long currentNumber) throws Lexer.NoSuchLexemeException, Lexer.StringNotTerminatedException{
    Lexer lexer = new Lexer(filter);
    lexer.skipWhitespace = false;
    lexer.addToken(PathStringCommands.fileName,PathStringCommands.nameNoExt,PathStringCommands.extension,PathStringCommands.number);
    int numerationAmmount = 0;
    String newName = "";
    PathStringCommands pathString = new PathStringCommands(origName);

    boolean addingDigits = false;
    Collection<Token> remainingTokens = lexer.getRemainingTokens();
    for(Token token:remainingTokens){
        if(token.id.equals(PathStringCommands.number)){
            if(addingDigits){
                numerationAmmount++;
            }else{
                numerationAmmount = 1;
                addingDigits = true;
            }
        }else{
            if(addingDigits){
                addingDigits = false;
                newName += simpleFormat(currentNumber,numerationAmmount);
            }
            if(token.id.equals(PathStringCommands.fileName)){
                newName += pathString.getName(true);
            }else if(token.id.equals(PathStringCommands.nameNoExt)){
                newName += pathString.getName(false);
            }else if(token.id.equals(PathStringCommands.extension)){
                newName += pathString.getExtension();
            }else{
                Literal lit = (Literal)token;
                newName += lit.value;
            }
            
        }
    }
    if(addingDigits){
        newName += simpleFormat(currentNumber,numerationAmmount);
    }
    return trimEnd(newName);
}
public void setNumber(){
    try{
        startingNumber = Integer.parseInt(this.tfStartingNumber.getText());
        increment = Integer.parseInt(this.tfIncrement.getText());
    }catch(Exception ex){
        startingNumber = 0;
        increment = 1;
        this.tfStartingNumber.setText(startingNumber+"");
        this.tfIncrement.setText(increment+"");
        //reportError(ex);
    }
}
public void apply(){
    for(Object object:table.getItems()){
        TableItemObject ob = (TableItemObject) object;
        
        try {
//            ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileAndPopulate(ob.path1.getParent(1));
//            String fallback = TaskFactory.resolveAvailablePath(parent, ob.path1.getName(true));
//            fallback = ExtStringUtils.replaceOnce(fallback, parent.getAbsoluteDirectory(), "");
            TaskFactory.getInstance().renameTo(ob.path1.getPath(),ob.path2.getName(true),Math.random()+""+Math.random());
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
    }
    update();
    Platform.runLater(()->{
        update();
    });
    
}
private static class TableItemObject{
    public PathStringCommands path1;
    public PathStringCommands path2;
    public SimpleStringProperty date;
    public SimpleLongProperty size;
    public boolean excludeMe;
    public boolean isFolder;
    
    
    public TableItemObject(ExtPath file){
        this.date = new SimpleStringProperty(file.lastModified()+"");
        this.size = new SimpleLongProperty(file.size());
        this.path1 = new PathStringCommands(file.getAbsolutePath());
        this.path2 = new PathStringCommands(file.getAbsolutePath());
        this.isFolder = file.getIdentity().equals(Enums.Identity.FOLDER);
    }
    public void newName(String s){
        
        
        String parent = this.path2.getParent(1);
        this.path2.setPath(parent+File.separator+s);
    }
}
private LinkedList<TableItemObject> applyFilters(LinkedList<TableItemObject> items){
    LinkedList<TableItemObject> list = new LinkedList<>();
    for(TableItemObject object:items){
        if(!this.includeFolders.selectedProperty().get()){
            if(object.isFolder){
                object.excludeMe = true;
            }
        }
        if(this.showOnlyDifferences.selectedProperty().get()){
            if(object.path1.getName(true).equals(object.path2.getName(true))){
                object.excludeMe = true;
            }
        }
        list.add(object);
    }
    return list;
}
private void setTableItems(LinkedList<TableItemObject> items){
    
    table.getItems().clear();
    for(TableItemObject object:items){
        if(!object.excludeMe){
            table.getItems().add(object);
        }
    }
    TableColumn get = (TableColumn) table.getColumns().get(0);
    get.setVisible(false);
    get.setVisible(true);
}    
}
