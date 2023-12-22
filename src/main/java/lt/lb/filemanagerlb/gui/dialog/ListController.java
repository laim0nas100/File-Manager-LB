package lt.lb.filemanagerlb.gui.dialog;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.javafx.FX;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.gui.MyBaseController;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class ListController extends MyBaseController<ListController> {

    @FXML
    public Label descriptionLabel;
    @FXML
    public ListView listView;
    @FXML
    public TextField pathToSave;
    @FXML
    public Label size;

    @Override
    public void update() {
    }

    public void beforeShow(String title, String desc) {
        super.beforeShow(title);
        this.descriptionLabel.setText(desc);

    }

    public void afterShow(Collection<String> list) {
        FX.submit(() -> {
            listView.getItems().setAll(list);
            size.setText(list.size() + "");
        });
    }

    public void save() throws FileNotFoundException, UnsupportedEncodingException {
        String text = this.pathToSave.getText();
        ObservableList<String> items = this.listView.getItems();
        ReadOnlyIterator<String> of = ReadOnlyIterator.of(items.stream().map(m -> m.trim()));
        lt.lb.commons.io.text.TextFileIO.writeToFile(D.USER_DIR + text, of);
    }

}
