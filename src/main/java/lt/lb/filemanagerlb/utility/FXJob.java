package lt.lb.filemanagerlb.utility;

import lt.lb.commons.javafx.FX;
import com.github.laim0nas100.uncheckedutils.func.UncheckedConsumer;

/**
 *
 * @author Lemmin
 */
public class FXJob extends SafeJob<Void> {

    public FXJob(UncheckedConsumer<FXJob> call) {
        super((UncheckedConsumer) call);
    }

    @Override
    protected void runTask() {
        FX.submit(task);
    }

}
