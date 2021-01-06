package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.filemanagerlb.gui.MyBaseController;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lt.lb.commons.javafx.CosmeticsFX;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.javafx.FXTask;
import lt.lb.commons.javafx.MenuBuilders;
import lt.lb.filemanagerlb.utility.PathStringCommands;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class DuplicateFinderController extends MyBaseController {

    @FXML
    public TableView list;
    @FXML
    public Text correlationRatio;
    @FXML
    public Slider slider;
    @FXML
    public Button searchButton;
    @FXML
    public Text textRootFolder;
    @FXML
    public ProgressBar progressBar;
    @FXML
    public CheckBox checkUseHash;
    @FXML
    public Label labelProgress;
    private HashMap<String, Double> map = new HashMap<>();

    private Double ratio;
    private ExtFolder root;
    private FXTask task;

    public void beforeShow(String title, ExtFolder root) {
        task = FXTask.temp();
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
                String str = cellData.getValue().ratio + "";
                return new SimpleStringProperty(str.substring(0, Math.min(5, str.length())));
            }
        });

        list.getColumns().setAll(nameCol1, pathCol1, nameCol2, pathCol2, ratioCol);
    }

    @Override
    public void afterShow() {
        ContextMenu build = new MenuBuilders.ContextMenuBuilder()
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Mark path 1")
                        .withAction(eh -> {
                            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
                            for (Object ob : selectedItems) {
                                SimpleTableItem item = (SimpleTableItem) ob;
                                TaskFactory.getInstance().addToMarked(LocationAPI.getInstance().getFileOptimized(item.f1.getPath()));
                            }
                        })
                )
                .addItem(new MenuBuilders.MenuItemBuilder()
                        .withText("Mark path 2")
                        .withAction(eh -> {
                            ObservableList selectedItems = list.getSelectionModel().getSelectedItems();
                            for (Object ob : selectedItems) {
                                SimpleTableItem item = (SimpleTableItem) ob;
                                TaskFactory.getInstance().addToMarked(LocationAPI.getInstance().getFileOptimized(item.f2.getPath()));
                            }
                        })
                )
                .addNestedDisableBind()
                .addNestedVisibilityBind()
                .build();
        MenuItem wrapSelectContextMenu = CosmeticsFX.wrapSelectContextMenu(list.getSelectionModel());
        build.getItems().add(wrapSelectContextMenu);
        this.list.setContextMenu(build);
    }

    public void search() {
        ratio = slider.valueProperty().divide(100).get();
        FX.submit(() -> {
            cancel();
            list.getItems().clear();
            List synchronizedList = Collections.synchronizedList(list.getItems());
            ArrayList<PathStringCommands> array = new ArrayList<>();
            root.getListRecursive(true).stream().forEach(item -> {
                array.add(new PathStringCommands(item.getAbsolutePath()));
            });
            if (this.checkUseHash.selectedProperty().get()) {
                task = TaskFactory.getInstance().duplicateFinderTask(array, ratio, synchronizedList, map);
            } else {
                task = TaskFactory.getInstance().duplicateFinderTask(array, ratio, synchronizedList, null);

            }
            this.progressBar.progressProperty().bind(task.progressProperty());

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });

    }

    @Override
    public void update() {
        root.update();
    }

    public void cancel() {
        task.cancel();
    }

    public static class SimpleTableItem {

        public PathStringCommands f1;
        public PathStringCommands f2;
        public double ratio;

        public SimpleTableItem(PathStringCommands file1, PathStringCommands file2, double ratio) {
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
