package lt.lb.filemanagerlb;

import java.io.File;
import java.util.List;
import lt.lb.KeyProp;
import lt.lb.TolerantConfig;
import lt.lb.KeyProp.KDP;
import lt.lb.KeyProp.KP;
import lt.lb.KeyProp.KeyDefaultProperty;
import lt.lb.commons.Java;
import lt.lb.commons.reflect.unified.ReflFields;
import lt.lb.filemanagerlb.gui.MediaPlayerController;
import lt.lb.filemanagerlb.gui.VLCInit;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
 */
public class P {

    public static final KeyDefaultProperty<Boolean> debug = KeyProp.of("debug",false).toKeyDefaultProperty();
    public static final KeyDefaultProperty<Integer> lookDepth = KeyProp.of("lookDepth", 2).toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> ROOT_NAME = KeyProp.of("ROOT_NAME", "ROOT").toKeyDefaultProperty();
    public static final KeyDefaultProperty<Integer> maxThreadsForTask = KeyProp.of("maxThreadsForTask", Java.getAvailableProcessors()).toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> userDir = KeyProp.of("userDir", D.HOME_DIR.absolutePath).toKeyDefaultProperty();
    public static final KeyDefaultProperty<Boolean> bufferedFileStreams = KeyProp.of("bufferedFileStreams", Boolean.TRUE).toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> virtualPrefix = KeyProp.of("virtualPrefix", "Virtual_").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> vlcPath = KeyProp.of("vlcPath", D.HOME_DIR + Java.getFileSeparator() + "lib").toKeyDefaultProperty();
    public static final KeyDefaultProperty<Boolean> oldPlayerMode = KeyProp.of("oldPlayerMode", false).toKeyDefaultProperty();

    public static final KeyDefaultProperty<String> number = KeyProp.of("filter.number", "#").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> fileName = KeyProp.of("filter.fileName", "<n>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> nameNoExt = KeyProp.of("filter.nameNoExt", "<nne>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> filePath = KeyProp.of("filter.filePath", "<ap>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> extension = KeyProp.of("filter.extension", "<ne>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> parent1 = KeyProp.of("filter.parent1", "<p1>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> parent2 = KeyProp.of("filter.parent2", "<p2>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> custom = KeyProp.of("filter.custom", "<c>").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> relativeCustom = KeyProp.of("filter.relativeCustom", "<rc>").toKeyDefaultProperty();

    public static final KeyDefaultProperty<String> commandInit = KeyProp.of("code.init", "#").toKeyDefaultProperty();
    public static final KeyDefaultProperty<Integer> truncateAfter = KeyProp.of("code.truncateAfter", 100000).toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandGenerate = KeyProp.of("code.commandGenerate", "generate").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandApply = KeyProp.of("code.commandApply", "apply").toCachableDefaultProperty();
    public static final KeyDefaultProperty<String> commandClear = KeyProp.of("code.clear", "clear").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandCancel = KeyProp.of("code.cancel", "cancel").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandList = KeyProp.of("code.list", "list").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandListRec = KeyProp.of("code.listRec", "listRec").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandSetCustom = KeyProp.of("code.setCustom", "setCustom").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandHelp = KeyProp.of("code.help", "help").toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandListParams = KeyProp.of("code.listParameters", "list").toKeyDefaultProperty();
    public static final KeyDefaultProperty<Integer> maxExecutablesAtOnce = KeyProp.of("code.maxThreadsForCommand", maxThreadsForTask.getDefault()).toKeyDefaultProperty();
    public static final KeyDefaultProperty<String> commandCopyFolderStructure = KeyProp.of("code.copyFolderStructure", "copyStructure").toKeyDefaultProperty();

    public static List<KP> getActiveParameters() {
        return ReflFields.getConstantFields(P.class, KDP.class)
                .mapSafeOpt(ErrorReport::report, m -> m.safeGet())
                .map(f -> new KP(f.getKey(), f.resolve(D.parameters)))
                .toUnmodifiableList();
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
        VLCInit.VLC_SEARCH_PATH = new PathStringCommands(vlcPath.resolve(param)).getPath() + File.separator;
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
