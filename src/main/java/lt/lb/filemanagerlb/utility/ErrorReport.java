/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.utility;

import lt.lb.filemanagerlb.gui.FileManagerLB;
import lt.lb.filemanagerlb.gui.MainController;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;
import lt.lb.commons.Log;
import lt.lb.filemanagerlb.D;

/**
 *
 * @author Laimonas Beniušis
 */
public class ErrorReport {

    
    static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    public static void report(Throwable ex) {
        ErrorReport error = new ErrorReport(ex);

        if (D.DEBUG.get()) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
        MainController.errorLog.add(0, error);
        Log.print("Exception:", ex.getMessage());
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
