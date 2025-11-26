package lt.lb.filemanagerlb.gui.dialog;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import lt.lb.commons.javafx.ExtTask;
import lt.lb.commons.javafx.FX;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.DLog;
import lt.lb.commons.DLog.LogStream;
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
import lt.lb.recombinator.CodepointFlattener;
import lt.lb.recombinator.FlatMatched;
import lt.lb.recombinator.Utils;
import lt.lb.recombinator.impl.codepoint.CodepointMatchers;
import lt.lb.uncheckedutils.Checked;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
    public FastExecutor executor;
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

    private DLog dlog = assignDLog();

    private DLog assignDLog() {
        DLog log = new DLog();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(LocalDateTime.now());
        int i = 1;
        while (i < 10) {
            try {
                DLog.changeStream(log, LogStream.FILE, D.HOME_DIR.COMMAND_WINDOW_LOGS + "session_" + date + ".txt");
                DLog.setMinimal(log);
                log.display = false;//only write to file
                return log;
            } catch (IOException e) {
                ErrorReport.report(e);
                i++;
            }

        }
        DLog.close(log);
        return null;
    }

    @Override
    public void beforeShow(String title) {
        super.beforeShow(title);

        executor = new FastExecutor(maxExecutablesAtOnce);
        command = new Commander(this, textField);
        command.addCommand(commandCopyFolderStructure, (String... params) -> {
            Logger.info("Copy params", Arrays.asList(params));
            String newCom = (String) params[0];
            newCom = Strings.CS.replaceOnce(newCom, commandCopyFolderStructure + " ", "");
            ExtFolder root = (ExtFolder) LocationAPI.getInstance().getPathNearest(newCom);
            ExtFolder dest = (ExtFolder) LocationAPI.getInstance().getPathNearest(D.customPath.getPath());
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

                    ExtPath parent = LocationAPI.getInstance().getPathNearest(root.getPathCommands().getParent(1));
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
            executor.cancelAll(false);
        });
        command.addCommand(commandGenerate, (String... params) -> {
            String newCom = (String) params[0];
            newCom = Strings.CS.replaceOnce(newCom, commandGenerate + " ", "");
            command.generate(newCom);
        });

        command.addCommand(commandApply, (String... params) -> {
            String newCom = (String) params[0];
            newCom = Strings.CS.replaceOnce(newCom, commandApply + " ", "");
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
            newCom = Strings.CS.replaceOnce(newCom, commandListRec + " ", "");
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
            newCom = Strings.CS.replaceOnce(newCom, commandList + " ", "");
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
            newCom = Strings.CS.replaceOnce(newCom, commandSetCustom + " ", "");
            D.customPath = new PathStringCommands(newCom.trim());
        });
        command.addCommand(commandClear, (String... params) -> {
            textArea.clear();
        });
        command.addCommand(commandHelp, (String... params) -> {

            listParameters();
            addToTextArea("Read:" + D.HOME_DIR.Parameters.absolutePath + " file for info\n");
        });
        command.addCommand(commandListParams, (String... params) -> {
            listParameters();
        });
    }

    public void listParameters() {
        P.getActiveParameters().forEach(val -> {
            addToTextArea(val.getKey() + "=" + val.getValue() + "\n");
        });
    }

    public void addToTextArea(String text) {
        addToTextArea(true, text);
    }

    public void addToTextArea(boolean logMe, String text) {
        FX.submit(() -> {
            if (logMe) {
                DLog.println(dlog, Strings.CS.removeEnd(text, "\n"));
            }
            String newString = textArea.getText() + text;
            textArea.setText(newString.substring(Math.max(newString.length() - truncateAfter, 0)));
            textArea.positionCaret(textArea.getLength());
        });
    }

    public void handleStream(Process process, boolean setTextAfterwards, String command) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        ArrayDeque<String> lines = new ArrayDeque<>();
        if (setTextAfterwards) {
            lines.add("$" + command);
//            addToTextArea(textArea, "Begin: " + command);
        } else {

        }
        while (line != null) {
            lines.add(line);
            if (!setTextAfterwards) {
                addToTextArea(line + "\n");
            }
            line = reader.readLine();
        }
        final int errorCode = process.exitValue();
        lines.add("Error Code:" + errorCode + "\n");
        if (setTextAfterwards) {
            StringBuilder main = new StringBuilder();
            for (String ln : lines) {
                main.append(ln).append("\n");
            }
            addToTextArea(main.toString());
        } else {
            addToTextArea("Error Code:" + errorCode + "\n\n");
        }
    }
    public static final CodepointMatchers C = new CodepointMatchers();

    public static class Commander extends AbstractCommandField {

        private boolean setTextAfterwards = false;
        private CommandWindowController ctrl;

        public Commander(CommandWindowController controller, TextField tf) {
            super(tf);
            this.ctrl = controller;
        }

        public void apply(String name) throws IOException, InterruptedException {
            String script = D.HOME_DIR.SCRIPTS + name;
            try {

                ArrayDeque<String> readFromFile = new ArrayDeque(
                        lt.lb.commons.io.text.TextFileIO.readFromFile(script));
                this.setTextAfterwards = true;
                ctrl.addToTextArea("Running script:" + script + "\n");
                for (String command : readFromFile) {
                    submit(command);
                }
            } catch (IOException io) {
                ctrl.addToTextArea("Failed to run script:" + script + "\n");
                ctrl.addToTextArea(io.getLocalizedMessage());
            }

        }

        public void generate(String command) {
            try {

                LinkedList<String> l = new LinkedList<>();
                MainController.markedList.forEach(item -> {
                    l.add(item.getAbsolutePath());
                });
                LinkedList<String> allCommands = new LinkedList<>();
                CodepointFlattener iterator = new CodepointFlattener(Utils.peekableCodepoints(command));

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

                Logger.info(collect);
                int index = 1;
                for (String absPath : l) {
                    PathStringCommands pathInfo = new PathStringCommands(absPath);
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
                    allCommands.add(sb.toString());
                    Logger.info(command + " => " + sb);
                    index++;
                }
                ViewManager.getInstance().newListFrame("Script generation", allCommands);
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }

//        public void generateOld(String command) {
//            try {
//
        ////                System.out.println(MainController.markedList);
//                LinkedList<String> l = new LinkedList<>();
//                MainController.markedList.forEach(item -> {
//                    l.add(item.getAbsolutePath());
//                });
//                LinkedList<String> allCommands = new LinkedList<>();
//                Lexer lexer = new Lexer();
//                lexer.resetLines(Arrays.asList(command));
//                lexer.setSkipWhitespace(false);
//                lexer.addKeywordBreaking(PathStringCommands.returnDefinedKeys().stream().toArray(s -> new String[s]));
//                int index = 1;
//                for (String absPath : l) {
//                    PathStringCommands pathInfo = new PathStringCommands(absPath);
//                    lexer.reset();
//                    String commandToAdd = "";
//                    int numbersToAdd = 0;
//                    while (true) {
//                        Optional<Token> opt = lexer.getNextToken();
//                        if (!opt.isPresent()) {
//                            break;
//                        }
//                        Token token = opt.get();
//                        if (token.value.equals(PathStringCommands.number)) {
//                            numbersToAdd++;
//                            continue;
//                        } else if (numbersToAdd > 0) {
//                            commandToAdd += ExtStringUtils.simpleFormat(index, numbersToAdd);
//                            numbersToAdd = 0;
//                        }
//
//                        if (token.value.equals(PathStringCommands.fileName)) {
//                            commandToAdd += pathInfo.getName(true);
//                        } else if (token.value.equals(PathStringCommands.nameNoExt)) {
//                            commandToAdd += pathInfo.getName(false);
//                        } else if (token.value.equals(PathStringCommands.filePath)) {
//                            commandToAdd += pathInfo.getPath();
//                        } else if (token.value.equals(PathStringCommands.extension)) {
//                            commandToAdd += pathInfo.getExtension();
//                        } else if (token.value.equals(PathStringCommands.parent1)) {
//                            commandToAdd += pathInfo.getParent(1);
//                        } else if (token.value.equals(PathStringCommands.parent2)) {
//                            commandToAdd += pathInfo.getParent(2);
//                        } else if (token.value.equals(PathStringCommands.custom)) {
//                            commandToAdd += D.customPath.getPath();
//                        } else if (token.value.equals(PathStringCommands.relativeCustom)) {
//                            commandToAdd += D.customPath.relativePathTo(pathInfo.getPath());
//                        } else {
//                            Literal lit = (Literal) token;
//                            commandToAdd += lit.value;
//                        }
//
//                    }
//                    if (numbersToAdd > 0) {
//                        commandToAdd += ExtStringUtils.simpleFormat(index, numbersToAdd);
//                    }
//                    allCommands.add(commandToAdd);
//                    Logger.info(command + " => " + commandToAdd);
//                    index++;
//                }
//                ViewManager.getInstance().newListFrame("Script generation", allCommands);
//            } catch (Exception ex) {
//                ErrorReport.report(ex);
//            }
//        }

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
            Logger.info("Params:{}", Arrays.asList(params));

            try {
                if (runCommand(c, params)) {
                    ctrl.addToTextArea("$:" + command + "\n");
                    Logger.info("Run in-built command:{}", command);
                } else {
                    ExtTask task = new ExtTask() {
                        @Override
                        protected Void call() throws Exception {
                            Logger.info("Run native command:{}", command);
                            CommandLine parse = CommandLine.parse(command);
                            List<String> args = new ArrayList<>();
                            args.add(parse.getExecutable());
                            for (String s : parse.getArguments()) {
                                args.add(s);
                            }
                            ProcessBuilder processBuilder = new ProcessBuilder(args.stream().toArray(s -> new String[s])).redirectErrorStream(true);

                            Process process = processBuilder.start();
                            ctrl.handleStream(process, setTextAfterwards, command);
                            return null;
                        }
                    };
                    task.appendOnFailed(h -> {
                        ErrorReport.report(task.getException());
                    });
                    ctrl.executor.submit(task);
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
    public void exitLogic() {
        executor.shutdown();
        if (dlog != null) {
            DLog.close(dlog);
            dlog = null;
        }
    }

}
