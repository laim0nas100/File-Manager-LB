/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.filemanagerlb.gui.MyBaseController;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.javafx.FX;
import lt.lb.filemanagerlb.D;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class ListController extends MyBaseController {

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
        lt.lb.commons.io.TextFileIO.writeToFile(D.USER_DIR + text, of);
    }

}
