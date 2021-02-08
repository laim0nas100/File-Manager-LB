package lt.lb.filemanagerlb.utility;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lt.lb.commons.javafx.FX;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.VoidJob;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.commons.func.unchecked.UncheckedConsumer;

/**
 *
 * @author Lemmin
 */
public class FXJob extends VoidJob {
    
    public FXJob(UncheckedConsumer<Job<Void>> call) {
        super(call);
        this.addListener(SystemJobEventName.ON_EXCEPTIONAL, lis -> {
            Optional<ExecutionException> data = lis.getData();
            data.map(m -> m.getCause()).ifPresent(ErrorReport::report);
        });
    }
    
    @Override
    protected void runTask() {
        FX.submit(task);
    }
    
}
