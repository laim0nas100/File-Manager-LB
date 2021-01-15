package lt.lb.filemanagerlb.utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;
import lt.lb.commons.F;
import lt.lb.commons.func.unchecked.UnsafeRunnable;
import lt.lb.filemanagerlb.gui.MainController;
import org.tinylog.Logger;

/**
 *
 * @author Laimonas Beniušis
 */
public class ErrorReport {

    public static void with(UnsafeRunnable run){
        F.checkedRun(run).ifPresent(ErrorReport::report);
    }
    
    static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    public static void report(Throwable ex) {
        Logger.error(ex,"Error");
        ErrorReport error = new ErrorReport(ex);
        MainController.errorLog.add(0, error);
    }
    private final SimpleStringProperty errorName;
    private final Throwable errorCause;
    private final Date date;
    private final Tooltip details;

    public ErrorReport(Throwable errorCause) {
        this.errorCause = errorCause;
        date = Calendar.getInstance().getTime();
        
        String format = dateFormat.format(date);
        this.errorName = new SimpleStringProperty(format + " : " + errorCause.getMessage());
        this.details = new Tooltip();
        details.setText(getOnlyError(errorCause));
    }

    public SimpleStringProperty getErrorName() {
        return errorName;
    }

    public Throwable getErrorCause() {
        return errorCause;
    }

    public Date getDate() {
        return date;
    }

    public Tooltip getTooltip() {
        return details;
    }

    private static String getOnlyError(Throwable e) {
        String err = e.toString();
        int index = err.indexOf(":");
        if (index > 0) {
            return err.substring(0, index);
        } else {
            return err;
        }
    }

}
