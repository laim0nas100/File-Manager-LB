package lt.lb.filemanagerlb;

import java.util.concurrent.Executors;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.Java;
import lt.lb.commons.io.directoryaccess.Dir;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.executors.layers.NestedTaskSubmitionExecutorLayer;
import lt.lb.commons.threads.service.ServiceExecutorAggregatorBase;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.filemanagerlb.dirinfo.HomeDir;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import lt.lb.uncheckedutils.Checked;

/**
 *
 * @author laim0nas100
 */
public class D {

    public static class ServiceExecutorAggregatorMain extends ServiceExecutorAggregatorBase {

        public ServiceExecutorAggregatorMain() {
            this.defaultSupplier = () -> Checked.createDefaultExecutorService();
            this.defaultSchedulerSupplier = () -> Executors.newScheduledThreadPool(4);

            setService("date-size", () -> new FastWaitingExecutor(4, WaitTime.ofSeconds(3)));

            
            setMainService("MAIN");
            setService("MAIN", () -> {
//            FastWaitingExecutor exe = new FastWaitingExecutor(Math.max(Java.getAvailableProcessors() * 4, 40), WaitTime.ofSeconds(120));
                return new NestedTaskSubmitionExecutorLayer(Checked.createDefaultExecutorService());
            });
        }

    }

    public static final ServiceExecutorAggregatorBase exe = new ServiceExecutorAggregatorMain();

    public static SessionInfo sessionInfo = new SessionInfo();

    public static final HomeDir HOME_DIR = Dir.establishDirectory(HomeDir.class, Java.getUserHome(), "lb-soft", "FileManagerLB");

    public static final String VIRTUAL_FOLDERS_DIR = HOME_DIR.getAbsolutePathWithSeparator() + "VIRTUAL_FOLDERS" + Java.getFileSeparator();
    public static final String ARTIFICIAL_ROOT_DIR = HOME_DIR.getAbsolutePathWithSeparator() + "ARTIFICIAL_ROOT";
    public static String USER_DIR = HOME_DIR.getAbsolutePathWithSeparator();
    public static String ROOT_NAME = "ROOT";
    public static int MAX_THREADS_FOR_TASK = 10;
    public static int DEPTH = 1;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(false);
    public static int LogBackupCount = 1;
    public static SimpleBooleanProperty useBufferedFileStreams = new SimpleBooleanProperty(false);
    public static PathStringCommands customPath = new PathStringCommands(HOME_DIR.absolutePath);

    public static MultiStageManager sm;

    public static final ClassLoader cLoader = D.class.getClassLoader();

}
