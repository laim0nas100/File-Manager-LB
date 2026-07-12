package lt.lb.filemanagerlb.gui.custom;

import java.util.ArrayList;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lt.lb.commons.javafx.FX;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import org.apache.commons.lang3.Strings;

/**
 *
 * @author laim0nas100
 */
public class FileAddressField {

    private ArrayList<String> list;
    public TextField field;
    public ExtFolder folder;
    public String entered = "";
    public int index = 0;
    public int lastCorrectIndex = 0;

    public FileAddressField(TextField textField) {
        list = new ArrayList<>();
        this.field = textField;
        this.field.setOnKeyReleased((KeyEvent t) -> {

            KeyCode code = t.getCode();
            t.consume();
            boolean isDown = code.equals(KeyCode.DOWN);
            boolean isUp = code.equals(KeyCode.UP);

            String text = field.getText();
            if (code.isDigitKey() || code.isLetterKey() || code.isWhitespaceKey()) {//entered new text, reset
                entered = Strings.CI.replaceOnce(text, folder.getAbsoluteDirectory(), "");
                list.clear();
                folder.getFoldersFromFiles().forEach(fold -> {
                    list.add(fold.propertyName.get());
                });
                index = -1;
            }else{
                index = lastCorrectIndex;
            }
            if (isDown || isUp) {
                //Log.writeln("FileAddressField invoked");
                
                boolean end = false;
                while (!end && (index >= -1 && index <= list.size())) {
                    if (isDown) {
                        index++;
                    } else {
                        index--;
                    }
                    if (index < 0) {
                        index = 0;
                        end = true;
                    }
                    if (index >= list.size()) {
                        index = list.size() - 1;
                        end = true;
                    }
                    String s = list.get(index);

                    if (Strings.CI.startsWith(s, entered)) {
                        FX.submit(() -> {
                            if (folder.getIdentity().equals(Identity.VIRTUAL)) {
                                field.setText(s);
                            } else {
                                field.setText(folder.getAbsoluteDirectory() + s);
                            }
                            lastCorrectIndex = index;
                            field.positionCaret(field.getLength());
                        });
                        end = true;
                    }

                }
            }
        });
    }
}
