package lt.lb.filemanagerlb.gui.custom;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lt.lb.commons.containers.collections.LoopingList;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.utility.ExtStringUtils;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileAddressField {

    private LoopingList<String> list;
    public TextField field;
    public ExtFolder folder;
    public String f;

    public FileAddressField(TextField Tfield) {
        list = new LoopingList<>();
        this.field = Tfield;
        this.field.setOnKeyReleased((KeyEvent t) -> {
            t.consume();
            KeyCode code = t.getCode();
            if (code.equals(KeyCode.DOWN) || code.equals(KeyCode.UP)) {
                //Log.writeln("FileAddressField invoked");
                String text;
                if (f == null) {
                    text = field.getText();
                } else {
                    text = f;
                }
                list.clear();
                folder.getFoldersFromFiles().forEach(fold -> {
                    list.add(fold.propertyName.get());
                });
                String name = ExtStringUtils.replaceOnce(text, folder.getAbsoluteDirectory(), "");
                int index = 0;
                while (index < list.size()) {
                    String s;
                    if (code.equals(KeyCode.DOWN)) {
                        s = list.next();
                    } else {
                        s = list.prev();
                    }
                    index++;
                    if (ExtStringUtils.startsWithIgnoreCase(s, name)) {
                        Platform.runLater(() -> {
                            f = name;
                            if (folder.getIdentity().equals(Identity.VIRTUAL)) {
                                field.setText(s);
                            } else {
                                field.setText(folder.getAbsoluteDirectory() + s);
                            }

                            field.positionCaret(field.getLength());
                        });
                        break;
                    }
                }
            } else {
                f = null;
            }
        });
    }
}
