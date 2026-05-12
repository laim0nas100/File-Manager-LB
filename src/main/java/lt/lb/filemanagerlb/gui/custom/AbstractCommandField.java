package lt.lb.filemanagerlb.gui.custom;

import java.util.ArrayList;
import java.util.HashMap;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import lt.lb.commons.javafx.FX;
import lt.lb.filemanagerlb.utility.ErrorReport;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class AbstractCommandField {

    public HashMap<String, Command> commands = new HashMap<>();
    public TextField field;
    public ArrayList<String> commandHistory = new ArrayList<>();
    private int index = 0;

    public AbstractCommandField(TextField tf) {
        field = tf;
        field.setOnKeyReleased(eh -> {
            KeyCode code = eh.getCode();
            if (code.equals(KeyCode.UP)) {
                if (index - 1 >= 0) {
                    index--;
                    field.setText(commandHistory.get(index));
                }
            }
            if (code.equals(KeyCode.DOWN)) {
                if (index + 1 < commandHistory.size()) {
                    index++;
                    field.setText(commandHistory.get(index));
                }
            }
        });
        field.setOnAction(eh -> {
            String command = field.getText();
            commandHistory.add(command);
            index = commandHistory.size();
            field.clear();
            submit(command);
        });
    }

    public void addCommand(String commandInit, Command command) {
        this.commands.put(commandInit, command);
    }

    public abstract void submit(String command);

    public boolean runCommand(String commandInit, String[] params) throws Exception {
        if (this.commands.containsKey(commandInit)) {
            FX.submit(() -> {
                try {
                    this.commands.get(commandInit).run(params);
                } catch (Exception ex) {
                    ErrorReport.report(ex);
                }
            });
            return true;
        }
        return false;
    }

    public interface Command {

        public void run(String[] params) throws Exception;
    }
}
