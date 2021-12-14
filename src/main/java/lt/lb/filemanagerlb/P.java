package lt.lb.filemanagerlb;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import lt.lb.TolerantConfig;
import lt.lb.TolerantConfig.KDP;
import lt.lb.TolerantConfig.KP;
import lt.lb.commons.Ins;
import lt.lb.commons.Java;
import lt.lb.commons.reflect.fields.ReflFields;
import lt.lb.filemanagerlb.gui.MediaPlayerController;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
 */
public class P {

    public static final KDP<Boolean> debug = KDP.of("debug", false);
    public static final KDP<Integer> lookDepth = KDP.of("lookDepth", 2);
    public static final KDP<String> ROOT_NAME = KDP.of("ROOT_NAME", "ROOT");
    public static final KDP<Integer> maxThreadsForTask = KDP.of("maxThreadsForTask", TaskFactory.PROCESSOR_COUNT);
    public static final KDP<String> userDir = KDP.of("userDir", D.HOME_DIR.absolutePath);
    public static final KDP<Boolean> bufferedFileStreams = KDP.of("bufferedFileStreams", Boolean.TRUE);
    public static final KDP<String> virtualPrefix = KDP.of("virtualPrefix", "Virtual_");
    public static final KDP<String> vlcPath = KDP.of("vlcPath", D.HOME_DIR + Java.getFileSeparator() + "lib");
    public static final KDP<Boolean> oldPlayerMode = KDP.of("oldPlayerMode", true);

    public static final KDP<String> number = KDP.of("filter.number", "#");
    public static final KDP<String> fileName = KDP.of("filter.fileName", "<n>");
    public static final KDP<String> nameNoExt = KDP.of("filter.nameNoExt", "<nne>");
    public static final KDP<String> filePath = KDP.of("filter.filePath", "<ap>");
    public static final KDP<String> extension = KDP.of("filter.extension", "<ne>");
    public static final KDP<String> parent1 = KDP.of("filter.parent1", "<p1>");
    public static final KDP<String> parent2 = KDP.of("filter.parent2", "<p2>");
    public static final KDP<String> custom = KDP.of("filter.custom", "<c>");
    public static final KDP<String> relativeCustom = KDP.of("filter.relativeCustom", "<rc>");

    public static final KDP<String> commandInit = KDP.of("code.init", "#");
    public static final KDP<Integer> truncateAfter = KDP.of("code.truncateAfter", 100000);
    public static final KDP<String> commandGenerate = KDP.of("code.commandGenerate", "generate");
    public static final KDP<String> commandApply = KDP.of("code.commandApply", "apply");
    public static final KDP<String> commandClear = KDP.of("code.clear", "clear");
    public static final KDP<String> commandCancel = KDP.of("code.cancel", "cancel");
    public static final KDP<String> commandList = KDP.of("code.list", "list");
    public static final KDP<String> commandListRec = KDP.of("code.listRec", "listRec");
    public static final KDP<String> commandSetCustom = KDP.of("code.setCustom", "setCustom");
    public static final KDP<String> commandHelp = KDP.of("code.help", "help");
    public static final KDP<String> commandListParams = KDP.of("code.listParameters", "list");
    public static final KDP<Integer> maxExecutablesAtOnce = KDP.of("code.maxThreadsForCommand", maxThreadsForTask.getDefault());
    public static final KDP<String> commandCopyFolderStructure = KDP.of("code.copyFolderStructure", "copyStructure");

    public static List<KP> getActiveParameters() {
        return ReflFields.getConstantFields(P.class, Ins.of(KDP.class))
                .map(m -> m.safeGet().get())
                .map(f -> new KP(f.getKey(), f.resolve(D.parameters)))
                .collect(Collectors.toList());
    }

    public static void reload() {
        String confPath = D.HOME_DIR.getAbsolutePathWithSeparator() + "Parameters.txt";

        Configurations conf = new Configurations();
        TolerantConfig<ImmutableConfiguration> param = TolerantConfig.ofSuplierCached(() -> conf.properties(confPath));
//        ParaMap.SimpleParaMap param = ParaMap.defaultParaMap(list.iterator());
        D.DEBUG.set(P.debug.resolve(param));
        D.DEPTH = P.lookDepth.resolve(param);
        D.ROOT_NAME = P.ROOT_NAME.resolve(param);
        D.MAX_THREADS_FOR_TASK = P.maxThreadsForTask.resolve(param);
        D.USER_DIR = new PathStringCommands(userDir.resolve(param)).getPath() + File.separator;
        D.useBufferedFileStreams.setValue(bufferedFileStreams.resolve(param));
        VirtualFolder.VIRTUAL_FOLDER_PREFIX = virtualPrefix.resolve(param);
        MediaPlayerController.VLC_SEARCH_PATH = new PathStringCommands(vlcPath.resolve(param)).getPath() + File.separator;
        MediaPlayerController.oldMode = oldPlayerMode.resolve(param);
        PathStringCommands.number = number.resolve(param);
        PathStringCommands.fileName = fileName.resolve(param);
        PathStringCommands.nameNoExt = nameNoExt.resolve(param);
        PathStringCommands.filePath = filePath.resolve(param);
        PathStringCommands.extension = extension.resolve(param);
        PathStringCommands.parent1 = parent1.resolve(param);
        PathStringCommands.parent2 = parent2.resolve(param);
        PathStringCommands.custom = custom.resolve(param);
        PathStringCommands.relativeCustom = relativeCustom.resolve(param);
        CommandWindowController.commandInit = commandInit.resolve(param);
        CommandWindowController.truncateAfter = truncateAfter.resolve(param);
        CommandWindowController.commandGenerate = commandGenerate.resolve(param);
        CommandWindowController.commandApply = commandApply.resolve(param);
        CommandWindowController.commandClear = commandClear.resolve(param);
        CommandWindowController.commandCancel = commandCancel.resolve(param);
        CommandWindowController.commandList = commandList.resolve(param);
        CommandWindowController.commandListRec = commandListRec.resolve(param);
        CommandWindowController.commandSetCustom = commandSetCustom.resolve(param);
        CommandWindowController.commandHelp = commandHelp.resolve(param);
        CommandWindowController.commandListParams = commandListParams.resolve(param);
        CommandWindowController.maxExecutablesAtOnce = maxExecutablesAtOnce.resolve(param);
        CommandWindowController.commandCopyFolderStructure = commandCopyFolderStructure.resolve(param);

        param.getEntries().forEachRemaining(entry -> Logger.info(entry.getKey() + "=" + entry.getValue()));

        D.parameters = param;
    }

}
