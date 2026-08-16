package lt.lb.filemanagerlb.utility;

import com.github.laim0nas100.jobsystem.Job;
import com.github.laim0nas100.jobsystem.events.JobEventListener;
import com.github.laim0nas100.jobsystem.events.SystemJobDependency;
import com.github.laim0nas100.jobsystem.events.SystemJobEventName;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.github.laim0nas100.uncheckedutils.func.UncheckedConsumer;
import com.github.laim0nas100.uncheckedutils.func.UncheckedFunction;
import java.io.Serializable;

/**
 *
 * @author Lemmin
 */
public class SafeJob<T> extends Job<T> {

    public SafeJob(Serializable id, UncheckedConsumer<SafeJob<Void>> call) {
        super(id, (Consumer) call);
        this.addListener(SystemJobEventName.ON_EXCEPTIONAL, errorListener());
    }

    public SafeJob(Serializable id, UncheckedFunction<SafeJob<T>, T> call) {
        super(id, (UncheckedFunction) call);
        this.addListener(SystemJobEventName.ON_EXCEPTIONAL, errorListener());
    }

    public SafeJob(UncheckedConsumer<SafeJob<Void>> call) {
        this(Job.getNextID(), call);
    }

    public SafeJob(UncheckedFunction<SafeJob<T>, T> call) {
        this(Job.getNextID(), call);
    }

    public static JobEventListener errorListener() {
        return (j, type, d) -> {
            Optional<ExecutionException> data = d;
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
