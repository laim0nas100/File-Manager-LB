package lt.lb.filemanagerlb;

import com.github.laim0nas100.cfg.KeyProp;
import com.github.laim0nas100.cfg.KeyProp.KP;
import com.github.laim0nas100.cfg.KeyProp.KeyDefaultProperty;
import com.github.laim0nas100.cfg.TolerantConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import lt.lb.commons.Java;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.reflect.unified.ReflFields;
import lt.lb.filemanagerlb.gui.MediaPlayerController;
import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.tinylog.Logger;

/**
 * Parameters
 *
 * @author laim0nas100
 */
public class P {

    public static TolerantConfig parameters;

    public static <T> KeyDefaultProperty<T> prop(String key, T def) {
        KeyDefaultProperty prop = null;
        boolean cache = false;
        if (def instanceof Boolean) {
            prop = KeyProp.ofBoolean(key).cache(cache).toPreparedKeyDefaultProperty((boolean) def, getConfig());
        } else if (def instanceof Integer) {
            prop = KeyProp.ofInteger(key).cache(cache).toPreparedKeyDefaultProperty((int) def, getConfig());
        } else if (def instanceof String) {
            prop = KeyProp.ofString(key).cache(cache).toPreparedKeyDefaultProperty((String) def, getConfig());
        } else {
            throw new IllegalArgumentException("type of " + def + " is not implemented");
        }
        return Objects.requireNonNull(prop);
    }

    public static final KeyDefaultProperty<Boolean> debug = prop("debug", false);
    public static final KeyDefaultProperty<Boolean> showAbout = prop("showAbout", false);
    public static final KeyDefaultProperty<Integer> lookDepth = prop("lookDepth", 2);
    public static final KeyDefaultProperty<String> ROOT_NAME = prop("ROOT_NAME", "ROOT");
    public static final KeyDefaultProperty<Integer> maxThreadsForTask = prop("maxThreadsForTask", Java.getAvailableProcessors());
    public static final KeyDefaultProperty<String> userDir = prop("userDir", D.HOME_DIR.absolutePath);
    public static final KeyDefaultProperty<Boolean> bufferedFileStreams = prop("bufferedFileStreams", false);
    public static final KeyDefaultProperty<String> virtualPrefix = prop("virtualPrefix", "Virtual_");
    public static final KeyDefaultProperty<String> vlcPath = prop("vlcPath", D.HOME_DIR + Java.getFileSeparator() + "lib");
    public static final KeyDefaultProperty<Boolean> oldPlayerMode = prop("oldPlayerMode", false);

    public static final KeyDefaultProperty<String> number = prop("filter.number", "#");
    public static final KeyDefaultProperty<String> fileName = prop("filter.fileName", "<n>");
    public static final KeyDefaultProperty<String> nameNoExt = prop("filter.nameNoExt", "<nne>");
    public static final KeyDefaultProperty<String> filePath = prop("filter.filePath", "<ap>");
    public static final KeyDefaultProperty<String> extension = prop("filter.extension", "<ne>");
    public static final KeyDefaultProperty<String> parent1 = prop("filter.parent1", "<p1>");
    public static final KeyDefaultProperty<String> parent2 = prop("filter.parent2", "<p2>");
    public static final KeyDefaultProperty<String> custom = prop("filter.custom", "<c>");
    public static final KeyDefaultProperty<String> relativeCustom = prop("filter.relativeCustom", "<rc>");

    public static final KeyDefaultProperty<String> commandInit = prop("code.init", "init");
    public static final KeyDefaultProperty<Integer> truncateAfter = prop("code.truncateAfter", 1000000);
    public static final KeyDefaultProperty<String> commandGenerate = prop("code.commandGenerate", "generate");
    public static final KeyDefaultProperty<String> commandApply = prop("code.commandApply", "apply");
    public static final KeyDefaultProperty<String> commandClear = prop("code.clear", "clear");
    public static final KeyDefaultProperty<String> commandCancel = prop("code.cancel", "cancel");
    public static final KeyDefaultProperty<String> commandList = prop("code.list", "list");
    public static final KeyDefaultProperty<String> commandListRec = prop("code.listRec", "listRec");
    public static final KeyDefaultProperty<String> commandSetCustom = prop("code.setCustom", "setCustom");
    public static final KeyDefaultProperty<String> commandHelp = prop("code.help", "help");
    public static final KeyDefaultProperty<String> commandListParams = prop("code.listParameters", "list");
    public static final KeyDefaultProperty<Integer> maxExecutablesAtOnce = prop("code.maxThreadsForCommand", maxThreadsForTask.getDefault());
    public static final KeyDefaultProperty<String> commandCopyFolderStructure = prop("code.copyFolderStructure", "copyStructure");

    public static List<KP> getActiveParameters() {
        return ReflFields.getConstantFields(P.class, KeyProp.KeyProperty.class)
                .mapSafeOpt(ErrorReport::report, m -> m.safeGet())
                .map(f -> new KP(f.getKey(), f.resolve(P.parameters)))
                .toUnmodifiableList();
    }

    private static Supplier<List<TolerantConfig>> getConfig() {
        return () -> ImmutableCollections.listOf(P.parameters);
    }

    public static void reload() throws IOException {

        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(D.HOME_DIR.Parameters.getPath()));

        TolerantConfig param = TolerantConfig.of(properties);


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

        param.getEntries().forEach(entry -> Logger.info(entry.getKey() + "=" + entry.getValue()));

    }

}
