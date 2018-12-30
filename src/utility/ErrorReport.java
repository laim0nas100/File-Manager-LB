/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import filemanagerGUI.FileManagerLB;
import filemanagerGUI.MainController;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;
import lt.lb.commons.Log;

/**
 *
 * @author Laimonas Beniušis
 */
public class ErrorReport {

    public static void report(Throwable ex) {
        ErrorReport error = new ErrorReport(ex);

        if (FileManagerLB.DEBUG.get()) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
        MainController.errorLog.add(0, error);
        Log.print("Exception:", ex.toString());
    }
    private final SimpleStringProperty errorName;
    private final Throwable errorCause;
    private final Date date;
    private final Tooltip details;

    public ErrorReport(Throwable errorCause) {
        this.errorCause = errorCause;
        date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
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

    private String getOnlyError(Throwable e) {
        String err = e.toString();
        int index = err.indexOf(":");
        if (index > 0) {
            return err.substring(0, index);
        } else {
            return err;
        }
    }

}
