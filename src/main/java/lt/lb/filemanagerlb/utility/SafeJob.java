package lt.lb.filemanagerlb.utility;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.events.JobEventListener;
import lt.lb.jobsystem.events.SystemJobDependency;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import lt.lb.uncheckedutils.func.UncheckedFunction;

/**
 *
 * @author Lemmin
 */
public class SafeJob<T> extends Job<T> {

    public SafeJob(UncheckedConsumer<SafeJob<Void>> call) {
        super((Consumer) call);
        this.addListener(SystemJobEventName.ON_EXCEPTIONAL, errorListener());
    }

    public SafeJob(UncheckedFunction<SafeJob<T>, T> call) {
        super((UncheckedFunction) call);
        this.addListener(SystemJobEventName.ON_EXCEPTIONAL, errorListener());
    }

    public static JobEventListener errorListener() {
        return lis -> {
            Optional<ExecutionException> data = lis.getData();
            data.map(m -> m.getCause())
                    //                    .filter(ex -> !(ex instanceof CancellationException))// expected
                    .ifPresent(ErrorReport::report);
        };
    }

    public SafeJob<T> withDep(SystemJobEventName name, Job job) {
        this.addDependency(new SystemJobDependency(job, name));
        return this;
    }

}
