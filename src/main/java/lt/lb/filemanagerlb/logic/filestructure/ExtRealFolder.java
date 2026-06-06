package lt.lb.filemanagerlb.logic.filestructure;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.threads.TimestampingExecutionExclusive;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.Enums;
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

    protected TimestampingExecutionExclusive<Map<String, ExtPath>> pupolator = new TimestampingExecutionExclusive<>(D.exe.getMain(), WaitTime.ofSeconds(10), 8);

    @Override
    public Enums.Identity getIdentity() {
        return Enums.Identity.FOLDER;
    }

    @Override
    protected Future<Map<String, ExtPath>> populateFolder(boolean auto, Consumer<ExtPath> buffer, Supplier<Boolean> isCanceled) {

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
                            if (buffer != null) {
                                if (D.slowDownFiles) {
                                    Thread.sleep(10);
                                }
                                buffer.accept(file);
                            }
                        }

                    }
                }
            }

            return paths;
        };

        return pupolator.execute(auto, call);

    }

    @Override
    public void update() {
        Logger.info("Update:" + this.getAbsoluteDirectory());
        populateFolder(true, null, null);
    }

    @Override
    public Future update(List<ExtPath> receiver, Supplier<Boolean> isCanceled) {
        Logger.info("Update observable:" + this.getAbsoluteDirectory());
        Consumer<ExtPath> buffer = receiver != null ? receiver::add : null;
        return populateFolder(true, buffer, isCanceled);
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

    @Override
    public Collection<ExtPath> getFilesCollection() {
        return getFilesMap().values();
    }

}
