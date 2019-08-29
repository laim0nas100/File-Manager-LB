/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.gui.dialog;

import java.net.HttpURLConnection;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import lt.lb.commons.Log;
import lt.lb.filemanagerlb.gui.BaseController;
import lt.lb.filemanagerlb.logic.Enums;
import lt.lb.filemanagerlb.utility.ErrorReport;

/**
 * FXML Controller class
 *
 * @author Laimonas Beniušis
 */
public class WebDialogController extends BaseController{
   @FXML public WebView browser;
   


    public void afterShow(Enums.WebDialog info) {
        String path = "";
        
        try{
            path = info.address;
            Log.print(path);
            if(!isInternetReachable(path)){
                path = "file:///"+System.getProperty("user.dir")+"/"+info.local;
            }
            Log.print("Loading from local");
            browser.getEngine().load(path);
            
        }catch(Exception e){
            ErrorReport.report(e);
        }
    }
    public static boolean isInternetReachable(String path){
            try {
                //make a URL to a known source
                URL url = new URL(path);

                //open a connection to that source
                HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();

                //trying to retrieve data from the source. If there
                //is no connection, this line will fail
                Object objData = urlConnect.getContent();
            }catch (Exception e) {
                //ErrorReport.report(e);
                return false;
            }
            return true;
        }

    @Override
    public void update() {
    }
    
}
