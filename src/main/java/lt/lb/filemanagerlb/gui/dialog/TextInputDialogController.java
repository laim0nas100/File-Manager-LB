package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.filemanagerlb.gui.MyBaseController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author laim0nas100
 */

public abstract class TextInputDialogController extends MyBaseController implements TextInputDialogEssentials {
    @FXML public TextField textField;
    @FXML public Button buttonEnter;
    @FXML public Button buttonCancel;
    @FXML public Label description;
    
    protected SimpleBooleanProperty nameIsAvailable = new SimpleBooleanProperty();
    protected String stringToCheck;
    
    @Override
    public void beforeShow(String title){
        super.beforeShow(title);
        buttonEnter.disableProperty().bind(nameIsAvailable.not());
    }
}
