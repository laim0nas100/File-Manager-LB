package lt.lb.filemanagerlb.logic.filestructure;

import com.github.laim0nas100.uncheckedutils.Checked;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.threads.Futures;
import lt.lb.commons.threads.TimestampingExecutionExclusive;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.utility.BulkConsumer;
import lt.lb.filemanagerlb.utility.ErrorReport;
import org.tinylog.Logger;

/**
 *
 * @author laim0nas100
 */
public class ExtRealFolder extends ExtFolder {

    public ExtRealFolder(String src, Object... optional) {
        super(src, optional);
    }

    @Override
    public Enums.Identity getIdentity() {
        return Enums.Identity.FOLDER;
    }

    @Override
    protected Future<Map<String, ExtPath>> populateFolder(boolean auto, Consumer<ExtPath> reciever, Supplier<Boolean> isCanceled) {

        Callable<Map<String, ExtPath>> call = () -> {

            Map<String, ExtPath> paths = new LinkedHashMap<>();

            if (Files.isDirectory(toPath())) {
                String parent = getAbsoluteDirectory();

                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(parent))) {
                    for (Path f : dirStream) {

                        if (isCanceled != null && isCanceled.get()) {
                            Logger.info("Canceled from populate");
                            break;
                        }

                        final String name = f.getFileName().toString();
                        final String filePathStr = f.toString();
                        ExtPath file = null;
                        if (Files.exists(f)) {
                            if (Files.isDirectory(f)) {
                                file = new ExtRealFolder(filePathStr, f);
                            } else if (Files.isSymbolicLink(f)) {
                                file = new ExtLink(filePathStr, f);
                            } else {
                                file = new ExtPath(filePathStr, f);
                            }
                            paths.put(name, file);
                            if (reciever != null) {
                                if (D.slowDownFiles) {
                                    Thread.sleep(10);
                                }
                                reciever.accept(file);
                            }
                        }

                    }
                }
            }

            return paths;
        };

        TimestampingExecutionExclusive<Map<String, ExtPath>> populator = D.folderPopulatorCache.get(this.getAbsolutePath(), p -> {
            return createPopulator();
        });
        if (populator != null) {
            return populator.execute(auto, call);
        }else{
            //should never happen
            return Futures.done(Checked.uncheckedCall(call::call));
        }
    }

    protected TimestampingExecutionExclusive<Map<String, ExtPath>> createPopulator() {
        return new TimestampingExecutionExclusive<>(
                D.exe.getMain(),
                WaitTime.ofSeconds(10),
                32); //should not pass this cycle without overwriting incomplete slots, or deadlock might happen
    }

    @Override
    public void update() {
        Logger.info("Update:" + this.getAbsoluteDirectory());
        populateFolder(true, null, null);
    }

    @Override
    public Future update(BulkConsumer<ExtPath> receiver, Supplier<Boolean> isCanceled) {
        Logger.info("Update observable:" + this.getAbsoluteDirectory());
        return populateFolder(true, receiver, isCanceled);
    }

    @Override
    public Map<String, ExtPath> getFilesMap() {
        try {
            return populateFolder(false, null, null).get();
        } catch (Exception ex) {
            ErrorReport.report(ex);
            return ImmutableCollections.mapOf();
        }
    }

}
