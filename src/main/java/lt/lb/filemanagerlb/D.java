package lt.lb.filemanagerlb;

import com.github.laim0nas100.jobsystem.ScheduledJobExecutor;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.Java;
import lt.lb.commons.io.directoryaccess.Dir;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.threads.service.ServiceExecutorAggregatorBase;
import lt.lb.filemanagerlb.dirinfo.HomeDir;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import com.github.laim0nas100.uncheckedutils.Checked;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.executors.scheduled.DelayedTaskExecutor;
import lt.lb.commons.threads.sync.WaitTime;

/**
 * Definitions
 *
 * @author laim0nas100
 */
public class D {

    public static class ServiceExecutorAggregatorMain extends ServiceExecutorAggregatorBase {

        public ServiceExecutorAggregatorMain() {
            this.defaultSupplier = () -> Checked.createDefaultExecutorService();
//            this.defaultSupplier = () -> new FastWaitingExecutor(8, WaitTime.ofSeconds(4));
            this.defaultSchedulerSupplier = () -> new DelayedTaskExecutor(getMain());

//            setService("date-size", () -> new FastWaitingExecutor(16, WaitTime.ofSeconds(12)));
            setService("date-size", () -> Checked.createDefaultExecutorService());

            
            setMainService("MAIN");
            setService("MAIN", () -> {
                return new FastWaitingExecutor(Math.min(Java.getAvailableProcessors() * 4, 40), WaitTime.ofSeconds(60));
//                return new NestedTaskSubmitionExecutorLayer(Checked.createDefaultExecutorService());
//                return Checked.createDefaultExecutorService();
            });
            setMainSchedulerService("MAIN_SCHED");
            setService("MAIN_SCHED", ()-> new DelayedTaskExecutor(getMain()));
        }

    }

    public static final ServiceExecutorAggregatorBase exe = new ServiceExecutorAggregatorMain();
    public static final ScheduledJobExecutor jobsExecutor = new ScheduledJobExecutor(D.exe.service("jobs"));

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

    public static Set<String> globalDisabledSet = new HashSet<>();
    public static ReentrantLock lock = new ReentrantLock();

    public static Serializable dragInitWindowID = "";

    public static boolean slowDownFiles = false; // for partial folder view display testing

}
