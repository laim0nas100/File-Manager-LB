/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import utility.ErrorReport;
import utility.Log;

/**
 * FXML Controller class
 *
 * @author lemmin
 */
public class WebRegexHelpController extends BaseDialog{
   @FXML public WebView browser;
   

    @Override
    public void beforeShow(String title) {
        super.beforeShow(title);
        String path = "https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html";
       
        try{
            Log.writeln(path);
            if(!isInternetReachable()){
                path = "file:///"+System.getProperty("user.dir")+"/Pattern (Java Platform SE 7 ).html";
            }
            Log.writeln("Loading from local");
            browser.getEngine().load(path);
            
        }catch(Exception e){
            ErrorReport.report(e);
        }
       
       
        
    }
    public static boolean isInternetReachable(){
            try {
                //make a URL to a known source
                URL url = new URL("https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html");

                //open a connection to that source
                HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();

                //trying to retrieve data from the source. If there
                //is no connection, this line will fail
                Object objData = urlConnect.getContent();
            }catch (Exception e) {
                ErrorReport.report(e);
                return false;
            }
            return true;
        }
    
}
