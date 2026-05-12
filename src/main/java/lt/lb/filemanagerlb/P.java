package lt.lb.filemanagerlb;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import lt.lb.KeyProp;
import lt.lb.TolerantConfig;
import lt.lb.KeyProp.KP;
import lt.lb.KeyProp.KeyDefaultProperty;
import lt.lb.commons.Java;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.reflect.unified.ReflFields;
import lt.lb.filemanagerlb.gui.MediaPlayerController;
import lt.lb.filemanagerlb.gui.VLCInit;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.tinylog.Logger;

/**
 * Parameters
 * @author laim0nas100
 */
public class P {
    
    public static TolerantConfig<ImmutableConfiguration> parameters;

    public static final KeyDefaultProperty<Boolean> debug = KeyProp.of("debug",false).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Boolean> showAbout = KeyProp.of("showAbout",true).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Integer> lookDepth = KeyProp.of("lookDepth", 2).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> ROOT_NAME = KeyProp.of("ROOT_NAME", "ROOT").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Integer> maxThreadsForTask = KeyProp.of("maxThreadsForTask", Java.getAvailableProcessors()).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> userDir = KeyProp.of("userDir", D.HOME_DIR.absolutePath).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Boolean> bufferedFileStreams = KeyProp.of("bufferedFileStreams", Boolean.FALSE).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> virtualPrefix = KeyProp.of("virtualPrefix", "Virtual_").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> vlcPath = KeyProp.of("vlcPath", D.HOME_DIR + Java.getFileSeparator() + "lib").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Boolean> oldPlayerMode = KeyProp.of("oldPlayerMode", false).toPreparedCachableDefaultProperty(getConfig());

    public static final KeyDefaultProperty<String> number = KeyProp.of("filter.number", "#").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> fileName = KeyProp.of("filter.fileName", "<n>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> nameNoExt = KeyProp.of("filter.nameNoExt", "<nne>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> filePath = KeyProp.of("filter.filePath", "<ap>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> extension = KeyProp.of("filter.extension", "<ne>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> parent1 = KeyProp.of("filter.parent1", "<p1>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> parent2 = KeyProp.of("filter.parent2", "<p2>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> custom = KeyProp.of("filter.custom", "<c>").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> relativeCustom = KeyProp.of("filter.relativeCustom", "<rc>").toPreparedCachableDefaultProperty(getConfig());

    public static final KeyDefaultProperty<String> commandInit = KeyProp.of("code.init", "init").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Integer> truncateAfter = KeyProp.of("code.truncateAfter", 1000000).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandGenerate = KeyProp.of("code.commandGenerate", "generate").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandApply = KeyProp.of("code.commandApply", "apply").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandClear = KeyProp.of("code.clear", "clear").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandCancel = KeyProp.of("code.cancel", "cancel").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandList = KeyProp.of("code.list", "list").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandListRec = KeyProp.of("code.listRec", "listRec").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandSetCustom = KeyProp.of("code.setCustom", "setCustom").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandHelp = KeyProp.of("code.help", "help").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandListParams = KeyProp.of("code.listParameters", "list").toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<Integer> maxExecutablesAtOnce = KeyProp.of("code.maxThreadsForCommand", maxThreadsForTask.getDefault()).toPreparedCachableDefaultProperty(getConfig());
    public static final KeyDefaultProperty<String> commandCopyFolderStructure = KeyProp.of("code.copyFolderStructure", "copyStructure").toPreparedCachableDefaultProperty(getConfig());

    public static List<KP> getActiveParameters() {
        return ReflFields.getConstantFields(P.class, KeyProp.KeyProperty.class)
                .mapSafeOpt(ErrorReport::report, m -> m.safeGet())
                .map(f -> new KP(f.getKey(), f.resolve(P.parameters)))
                .toUnmodifiableList();
    }
    
    private static Supplier<List<TolerantConfig>> getConfig(){
        return () -> ImmutableCollections.listOf(P.parameters);
    }

    public static void reload() {

        Configurations conf = new Configurations();
        TolerantConfig<ImmutableConfiguration> param = TolerantConfig.ofSuplierCached(() -> conf.properties(D.HOME_DIR.Parameters.absolutePath));
        P.parameters = param;
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

        
    }

}
