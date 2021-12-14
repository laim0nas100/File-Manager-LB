package lt.lb.filemanagerlb.gui.dialog;

import lt.lb.commons.parsing.token.Literal;
import lt.lb.commons.parsing.token.Token;
import java.io.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import lt.lb.commons.javafx.DynamicTaskExecutor;
import lt.lb.commons.javafx.ExtTask;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.parsing.*;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.P;
import lt.lb.filemanagerlb.gui.FileManagerLB;
import lt.lb.filemanagerlb.gui.MainController;
import lt.lb.filemanagerlb.gui.MyBaseController;
import lt.lb.filemanagerlb.gui.ViewManager;
import lt.lb.filemanagerlb.gui.custom.AbstractCommandField;
import lt.lb.filemanagerlb.logic.Enums.Identity;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.utility.ContinousCombinedTask;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.ExtStringUtils;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import lt.lb.filemanagerlb.utility.SimpleTask;
import lt.lb.uncheckedutils.Checked;
import org.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class CommandWindowController extends MyBaseController {

    @FXML
    TextField textField;
    @FXML
    TextArea textArea;
    private Commander command;
    public static DynamicTaskExecutor executor = new DynamicTaskExecutor();
    public static int maxExecutablesAtOnce;
    public static int truncateAfter;
    public static String commandGenerate,
            commandApply,
            commandList,
            commandListRec,
            commandListParams,
            commandInit,
            commandSetCustom,
            commandClear,
            commandCancel,
            commandCopyFolderStructure,
            commandHelp;

    @Override
    public void beforeShow(String title) {
        super.beforeShow(title);
        command = new Commander(textField);
        command.addCommand(commandCopyFolderStructure, (String... params) -> {
            Logger.info("Copy params", Arrays.asList(params));
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandCopyFolderStructure + " ", "");
            ExtFolder root = (ExtFolder) LocationAPI.getInstance().getFileOptimized(newCom);
            ExtFolder dest = (ExtFolder) LocationAPI.getInstance().getFileOptimized(D.customPath.getPath());
            Logger.info("Copy structure:", root, dest);

            ContinousCombinedTask finalTask = new ContinousCombinedTask() {
                @Override
                protected void preparation() throws Exception {
                    ArrayList<ExtPath> collection = new ArrayList<>();
                    Callback<ExtPath, Void> call = new Callback<ExtPath, Void>() {
                        @Override
                        public Void call(ExtPath param) {
                            collection.add(param);
                            return null;
                        }
                    };
                    SimpleTask collectFolders = new SimpleTask() {
                        @Override
                        protected Void call() throws Exception {
                            root.collectRecursive(ExtPath.IS_NOT_DISABLED.and(ExtPath.IS_FOLDER), call);
                            return null;
                        }
                    };
                    collectFolders.setDescription("Collect folders");
                    this.addTask(collectFolders);

                    ExtPath parent = LocationAPI.getInstance().getFileOptimized(root.getPathCommands().getParent(1));
                    ContinousCombinedTask copyFiles = TaskFactory.getInstance().copyFilesEx(collection, dest, parent);
                    this.addTask(copyFiles);

                }
            };
            finalTask.setDescription("Copy folder structure");

//            FXTask copyFiles = TaskFactory.getInstance().copyFiles(root.getListRecursive(true),
//                    dest, LocationAPI.getInstance().getFileOptimized(root.getPathCommands().getParent(1)));
//            ViewManager.getInstance().newProgressDialog(copyFiles);
            ViewManager.getInstance().newProgressDialog(finalTask);

        });
        command.addCommand(commandCancel, (String... params) -> {
            executor.stopEverything();
        });
        command.addCommand(commandGenerate, (String... params) -> {
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandGenerate + " ", "");
            command.generate(newCom);
        });

        command.addCommand(commandApply, (String... params) -> {
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandApply + " ", "");
            command.apply(newCom);
        });
        command.addCommand(commandInit, (String... params) -> {
            FX.submit(() -> {
                Checked.checkedRun(() -> {
                    FileManagerLB.reInit();
                }).ifPresent(ErrorReport::report);

            });

        });
        command.addCommand(commandListRec, (String... params) -> {
            ArrayDeque<String> deque = new ArrayDeque<>();
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandListRec + " ", "");
            ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);

            for (ExtPath f : file.getListRecursive(false)) {
                deque.add(f.getAbsoluteDirectory());
            }
            String desc = "Listing recursive:" + deque.removeFirst();
            ViewManager.getInstance().newListFrame(desc, deque);
        });
        command.addCommand(commandList, (String... params) -> {
            ArrayDeque<String> deque = new ArrayDeque<>();
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandList + " ", "");
            ExtPath file = LocationAPI.getInstance().getFileAndPopulate(newCom);
            if (file.getIdentity().equals(Identity.FOLDER)) {
                String desc = "Listing:" + file.getAbsoluteDirectory();

                ExtFolder folder = (ExtFolder) file;
                folder.update();
                for (ExtPath f : folder.getFilesCollection()) {
                    deque.add(f.getAbsoluteDirectory());
                }
                ViewManager.getInstance().newListFrame(desc, deque);
            }
        });
        command.addCommand(commandSetCustom, (String... params) -> {
            String newCom = (String) params[0];
            newCom = ExtStringUtils.replaceOnce(newCom, commandSetCustom + " ", "");
            D.customPath = new PathStringCommands(newCom.trim());
        });
        command.addCommand(commandClear, (String... params) -> {
            textArea.clear();
        });
        command.addCommand(commandHelp, (String... params) -> {

            listParameters();
            addToTextArea(textArea, "Read Parameters.txt file for info\n");
        });
        command.addCommand(commandListParams, (String... params) -> {
            listParameters();
        });
    }

    public void listParameters() {
        P.getActiveParameters().forEach(val -> {
            addToTextArea(textArea, val.getKey() + "=" + val.getValue() + "\n");
        });
    }

    public void addToTextArea(TextArea textA, String text) {
        FX.submit(() -> {
            String newString = textA.getText() + text;
            textA.setText(newString.substring(Math.max(newString.length() - truncateAfter, 0)));
            textA.positionCaret(textA.getLength());
        });
    }

    public void handleStream(Process process, TextArea textArea, boolean setTextAfterwards, String command) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        ArrayDeque<String> lines = new ArrayDeque<>();
        if (!setTextAfterwards) {
            addToTextArea(textArea, "Begin: " + command);
        }
        while (line != null) {
            if (setTextAfterwards) {
                lines.add(line);
            } else {
                addToTextArea(textArea, line + "\n");
            }
            line = reader.readLine();
        }
        final int errorCode = process.exitValue();
        if (setTextAfterwards) {
            lines.add("Error Code:" + errorCode + "\n");
            StringBuilder main = new StringBuilder();
            for (String ln : lines) {
                main.append(ln).append("\n");
            }
            addToTextArea(textArea, main.toString());
        } else {
            addToTextArea(textArea, "Error Code:" + errorCode + "\n\n");
        }
    }

    public class Commander extends AbstractCommandField {

        private boolean setTextAfterwards = false;

        public Commander(TextField tf) {
            super(tf);
        }

        public void apply(String name) throws IOException, InterruptedException {
            ArrayDeque<String> readFromFile = new ArrayDeque(
                    lt.lb.commons.io.TextFileIO.readFromFile(D.USER_DIR + name));
            this.setTextAfterwards = true;
            for (String command : readFromFile) {
                submit(command);
            }
        }

        public void generate(String command) {
            try {

//                System.out.println(MainController.markedList);
                LinkedList<String> l = new LinkedList<>();
                MainController.markedList.forEach(item -> {
                    l.add(item.getAbsolutePath());
                });
                LinkedList<String> allCommands = new LinkedList<>();
                Lexer lexer = new Lexer();
                lexer.resetLines(Arrays.asList(command));
                lexer.setSkipWhitespace(false);
                lexer.addKeywordBreaking(PathStringCommands.returnDefinedKeys());
                int index = 1;
                for (String absPath : l) {
                    PathStringCommands pathInfo = new PathStringCommands(absPath);
                    lexer.reset();
                    String commandToAdd = "";
                    int numbersToAdd = 0;
                    while (true) {
                        Optional<Token> opt = lexer.getNextToken();
                        if (!opt.isPresent()) {
                            break;
                        }
                        Token token = opt.get();
                        if (token.value.equals(PathStringCommands.number)) {
                            numbersToAdd++;
                            continue;
                        } else if (numbersToAdd > 0) {
                            commandToAdd += ExtStringUtils.simpleFormat(index, numbersToAdd);
                            numbersToAdd = 0;
                        }

                        if (token.value.equals(PathStringCommands.fileName)) {
                            commandToAdd += pathInfo.getName(true);
                        } else if (token.value.equals(PathStringCommands.nameNoExt)) {
                            commandToAdd += pathInfo.getName(false);
                        } else if (token.value.equals(PathStringCommands.filePath)) {
                            commandToAdd += pathInfo.getPath();
                        } else if (token.value.equals(PathStringCommands.extension)) {
                            commandToAdd += pathInfo.getExtension();
                        } else if (token.value.equals(PathStringCommands.parent1)) {
                            commandToAdd += pathInfo.getParent(1);
                        } else if (token.value.equals(PathStringCommands.parent2)) {
                            commandToAdd += pathInfo.getParent(2);
                        } else if (token.value.equals(PathStringCommands.custom)) {
                            commandToAdd += D.customPath.getPath();
                        } else if (token.value.equals(PathStringCommands.relativeCustom)) {
                            commandToAdd += D.customPath.relativePathTo(pathInfo.getPath());
                        } else {
                            Literal lit = (Literal) token;
                            commandToAdd += lit.value;
                        }

                    }
                    if (numbersToAdd > 0) {
                        commandToAdd += ExtStringUtils.simpleFormat(index, numbersToAdd);
                    }
                    allCommands.add(commandToAdd);
                    Logger.info(command + " => " + commandToAdd);
                    index++;
                }
                ViewManager.getInstance().newListFrame("Script generation", allCommands);
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }

        @Override
        public void submit(String command) {
            Logger.info(command);
            LinkedList<String> list = new LinkedList<>();
            String[] split = command.split(" ");
            for (String spl : split) {
                if (spl.length() > 0) {
                    list.add(spl);
                }
            }
            LinkedList<String> coms = new LinkedList<>();
            coms.addAll(list);
            coms.add(1, command);
            Logger.info(coms);
            String c = coms.pollFirst();
            String[] params = coms.toArray(new String[1]);
            Logger.info("Params", Arrays.asList(params));
            addToTextArea(textArea, "$:" + command + "\n");
            try {
                if (runCommand(c, params)) {
                    Logger.info("Run in-built command:", command);
                } else {
                    ExtTask task = new ExtTask() {
                        @Override
                        protected Void call() throws Exception {
                            Logger.info("Run native command:", command);
                            Process process = new ProcessBuilder(list.toArray(new String[1])).redirectErrorStream(true).start();
                            handleStream(process, textArea, setTextAfterwards, command);
                            return null;
                        }
                    };
                    executor.submit(task);
                }
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
    }

    @Override
    public void update() {
    }

    public void submit() {
        command.setTextAfterwards = false;
        command.submit(this.textField.getText());
    }

    @Override
    public void exit() {
        super.exit();

    }

}
