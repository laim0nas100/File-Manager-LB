package lt.lb.filemanagerlb.gui.dialog;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import lt.lb.commons.F;
import lt.lb.commons.iteration.For;
import lt.lb.commons.javafx.FX;
//import lt.lb.commons.parsing.*;
//import lt.lb.commons.parsing.token.Literal;
//import lt.lb.commons.parsing.token.Token;
import lt.lb.commons.threads.executors.TaskBatcher;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.MyBaseController;
import static lt.lb.filemanagerlb.gui.dialog.CommandWindowController.C;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.LocationInRoot;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.ExtStringUtils;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import lt.lb.recombinator.CodepointFlattener;
import lt.lb.recombinator.FlatMatched;
import lt.lb.recombinator.Utils;
import lt.lb.uncheckedutils.Checked;
import org.apache.commons.lang3.StringUtils;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class AdvancedRenameController extends MyBaseController {

    @FXML
    public Tab tbSpecificRename;
    @FXML
    public Tab tbNumerize;

    @FXML
    public TextField tfStrReg;
    @FXML
    public TextField tfReplaceWith;
    @FXML
    public TextField tfFilter;
    @FXML
    public TextField tfStartingNumber;
    @FXML
    public TextField tfIncrement;

    @FXML
    public TableView table;

    @FXML
    public CheckBox useRegex;
    @FXML
    public CheckBox showFullPath;
    @FXML
    public CheckBox recursive;
    @FXML
    public CheckBox includeFolders;
    @FXML
    public CheckBox showOnlyDifferences;
    @FXML
    public Button buttonApply;

    private int startingNumber;
    private int increment;
    private LinkedList<TableItemObject> tableList;
    private ExtFolder virtual;

    public void beforeShow(String title, ExtFolder virtual) {
        super.beforeShow(title);
        this.setNumber();
        this.tableList = new LinkedList<>();
        this.virtual = virtual;
        Tooltip tp = new Tooltip();
        tp.setText("Name =" + PathStringCommands.fileName + ", Name without extension =" + PathStringCommands.nameNoExt
                + ", Name extension only =" + PathStringCommands.extension + ", Number (multi-digit if consecutive) =" + PathStringCommands.number);
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
                if (showFullPath.selectedProperty().get()) {
                    result = cellData.getValue().path1.getPath();
                } else {
                    result = cellData.getValue().path1.getName(true);
                }
                return new SimpleStringProperty(result);
            }
        });
        nameCol2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableItemObject, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<TableItemObject, String> cellData) {
                String result;
                if (showFullPath.selectedProperty().get()) {
                    result = cellData.getValue().path2.getPath();
                } else {
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

    public void updateLists() {
        virtual.update();
        ArrayList<ExtPath> array = new ArrayList<>();
        if (recursive.selectedProperty().get()) {
            this.virtual.getListRecursive(true).stream().forEach(file -> {
                array.add(file);
            });
        } else {
            this.virtual.getFilesCollection().stream().forEach(file -> {
                array.add(file);
            });
        }
        tableList.clear();
        for (ExtPath s : array) {
            tableList.add(new TableItemObject(s));
        }
        setTableItems(tableList);
        buttonApply.setDisable(true);
    }

    public void previewSetting() {
        tableList.clear();
        tableList.addAll(table.getItems());
        if (this.tbNumerize.isSelected()) {
            String filter = this.tfFilter.getText();
            setNumber();
            long number = startingNumber;

            for (TableItemObject object : this.tableList) {
                try {

                    object.newName(parseFilter(object.path1, filter, number));
                    number += increment;
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
            }
        } else {
            String strRegex = this.tfStrReg.getText();
            String replacement = "" + this.tfReplaceWith.getText();
            if (useRegex.isSelected()) {
                try {
                    Pattern compiled = Pattern.compile(strRegex);
                    for (TableItemObject object : this.tableList) {
                        object.newName(compiled.matcher(object.path1.getName(true)).replaceAll(replacement));
                    }
                } catch (PatternSyntaxException ex) {
                    ErrorReport.report(ex);
                    return;
                }
            } else {
                for (TableItemObject object : this.tableList) {
                    object.newName(ExtStringUtils.parseSimple(object.path1.getName(true), strRegex, replacement));
                }
            }
        }
        setTableItems(applyFilters(tableList));

        buttonApply.setDisable(false);
    }

    @Override
    public void update() {

        updateLists();

    }

    public String parseFilter(PathStringCommands pathInfo, String filter, long index) {
        CodepointFlattener iterator = new CodepointFlattener(Utils.peekableCodepoints(filter));

        for (String key : PathStringCommands.returnDefinedKeys()) {
            if (key.equals(PathStringCommands.number)) {
                iterator.with(C.makeNew(key).repeating(true).string(key));
            } else {
                iterator.with(C.makeNew(key).string(key));
            }

        }

        iterator.with(C.whitespace())
                .with(C.makeNew("Any").any(1));

        List<FlatMatched<String, String>> collect = iterator.toStream().collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();

        for (FlatMatched<String, String> flat : collect) {
            if (flat.containsMatcher(PathStringCommands.number)) {
                int numbersToAdd = flat.getItem().length() / PathStringCommands.number.length();
                sb.append(ExtStringUtils.simpleFormat(index, numbersToAdd));
            } else if (flat.containsMatcher(PathStringCommands.fileName)) {
                sb.append(pathInfo.getName(true));
            } else if (flat.containsMatcher(PathStringCommands.nameNoExt)) {
                sb.append(pathInfo.getName(false));
            } else if (flat.containsMatcher(PathStringCommands.filePath)) {
                sb.append(pathInfo.getPath());
            } else if (flat.containsMatcher(PathStringCommands.extension)) {
                sb.append(pathInfo.getExtension());
            } else if (flat.containsMatcher(PathStringCommands.parent1)) {
                sb.append(pathInfo.getParent(1));
            } else if (flat.containsMatcher(PathStringCommands.parent2)) {
                sb.append(pathInfo.getParent(2));
            } else if (flat.containsMatcher(PathStringCommands.custom)) {
                sb.append(D.customPath.getPath());
            } else if (flat.containsMatcher(PathStringCommands.relativeCustom)) {
                sb.append(D.customPath.relativePathTo(pathInfo.getPath()));
            } else {
                sb.append(flat.getItem());
            }
        }
        return sb.toString();
    }

//    public String parseFilterOld(String origName, String filter, long currentNumber) throws Lexer.StringNotTerminatedException {
//        Lexer lexer = new Lexer();
//        lexer.resetLines(Arrays.asList(filter));
//        lexer.setSkipWhitespace(false);
//        lexer.addKeywordBreaking(PathStringCommands.fileName, PathStringCommands.nameNoExt, PathStringCommands.extension, PathStringCommands.number);
//        int numerationAmmount = 0;
//        String newName = "";
//        PathStringCommands pathString = new PathStringCommands(origName);
//
//        boolean addingDigits = false;
//        for (Token token : lexer.getTokenIterator()) {
//            if (token.value.equals(PathStringCommands.number)) {
//                if (addingDigits) {
//                    numerationAmmount++;
//                } else {
//                    numerationAmmount = 1;
//                    addingDigits = true;
//                }
//            } else {
//                if (addingDigits) {
//                    addingDigits = false;
//                    newName += ExtStringUtils.simpleFormat(currentNumber, numerationAmmount);
//                }
//                if (token.value.equals(PathStringCommands.fileName)) {
//                    newName += pathString.getName(true);
//                } else if (token.value.equals(PathStringCommands.nameNoExt)) {
//                    newName += pathString.getName(false);
//                } else if (token.value.equals(PathStringCommands.extension)) {
//                    newName += pathString.getExtension();
//                } else {
//                    Literal lit = (Literal) token;
//                    newName += lit.value;
//                }
//
//            }
//        }
//        if (addingDigits) {
//            newName += ExtStringUtils.simpleFormat(currentNumber, numerationAmmount);
//        }
//        return StringUtils.trim(newName);
//    }

    public void setNumber() {
        try {
            startingNumber = Integer.parseInt(this.tfStartingNumber.getText());
            increment = Integer.parseInt(this.tfIncrement.getText());
        } catch (Exception ex) {
            startingNumber = 0;
            increment = 1;
            this.tfStartingNumber.setText(startingNumber + "");
            this.tfIncrement.setText(increment + "");
        }
    }

    public void apply() {
        TaskBatcher batcher = new TaskBatcher(D.exe);

        for (Object object : table.getItems()) {
            TableItemObject ob = (TableItemObject) object;
            batcher.execute(() -> {
                ExtFolder parent = (ExtFolder) LocationAPI.getInstance().getFileIfExists(new LocationInRoot(ob.path1.getParent(1)));
                PathStringCommands fallback = new PathStringCommands(TaskFactory.resolveAvailablePath(parent, ob.path1.getName(true)));
                String path = TaskFactory.getInstance().renameTo(ob.path1.getPath(), ob.path2.getName(true), fallback.getName(true));
                ExtPath file = LocationAPI.getInstance().getFileOptimized(path);
                if (file != null) {
                    if(this.virtual.getIdentity() == Identity.VIRTUAL){
                        this.virtual.files.put(file.getName(true), file);
                    }
                }
                return null;
            });
        }
        TaskBatcher.BatchRunSummary summary = batcher.awaitTolerateFails();
        For.elements().iterate(summary.failures, (i, e) -> {
            ErrorReport.report(F.cast(e));
        });
        Checked.checkedRun(FX.submit(this::update)::get);

    }

    private static class TableItemObject {

        public PathStringCommands path1;
        public PathStringCommands path2;
        public StringProperty date;
        public LongProperty size;
        public boolean excludeMe;
        public boolean isFolder;

        public TableItemObject(ExtPath file) {
            this.date = file.propertyDate;
            this.size = file.propertySize;
            this.path1 = new PathStringCommands(file.getAbsolutePath());
            this.path2 = new PathStringCommands(file.getAbsolutePath());
            this.isFolder = file.getIdentity().equals(Enums.Identity.FOLDER);
        }

        public void newName(String s) {

            String parent = this.path2.getParent(1);
            this.path2.setPath(parent + File.separator + s);
        }
    }

    private LinkedList<TableItemObject> applyFilters(LinkedList<TableItemObject> items) {
        LinkedList<TableItemObject> list = new LinkedList<>();
        for (TableItemObject object : items) {
            if (!this.includeFolders.selectedProperty().get()) {
                if (object.isFolder) {
                    object.excludeMe = true;
                }
            }
            if (this.showOnlyDifferences.selectedProperty().get()) {
                if (object.path1.getName(true).equals(object.path2.getName(true))) {
                    object.excludeMe = true;
                }
            }
            list.add(object);
        }
        return list;
    }

    private void setTableItems(LinkedList<TableItemObject> items) {

        table.getItems().clear();
        for (TableItemObject object : items) {
            if (!object.excludeMe) {
                table.getItems().add(object);
            }
        }
        TableColumn get = (TableColumn) table.getColumns().get(0);
        get.setVisible(false);
        get.setVisible(true);
    }
}
