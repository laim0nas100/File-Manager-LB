/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Laimonas Beniušis
 */
public class ErrorReport {
    private final SimpleStringProperty errorName;
    private final Exception errorCause;
    private final Date date;
    private final Tooltip details;

    public ErrorReport(Exception errorCause) {
        this.errorCause = errorCause;
        date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String format = dateFormat.format(date);
        this.errorName = new SimpleStringProperty(format +" : "+ errorCause.getMessage());
        this.details = new Tooltip();
        details.setText(getOnlyError(errorCause));
    }

    public SimpleStringProperty getErrorName() {
        return errorName;
    }

    public Exception getErrorCause() {
        return errorCause;
    }

    public Date getDate() {
        return date;
    }
    public Tooltip getTooltip(){
        return details;
    }
    private String getOnlyError(Exception e){
        String err = e.toString();
        int index = err.lastIndexOf(":");
        if(index>0){
            return err.substring(0,index);
        }else{
            return err;
        }
            
    }
    
    
}
