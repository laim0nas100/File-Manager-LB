package com.databasemanagement;

import com.databasemanagement.sqlParsing.Commander;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.stage.Stage;
import static javafx.application.Application.launch;



public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        load();
        ViewManager.getInstance().newWindow();
        ViewManager.getInstance().newAdminWindow();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        launch(args);
    }
    private void load() throws IOException, ClassNotFoundException, SQLException{
        ArrayList<String> connectionArg = new ArrayList<>();
        connectionArg.addAll(Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\databaseURL.txt"));
        connectionArg.addAll(Commander.readFromFile("E:\\Programming\\Java\\Workspace\\DatabaseManagement\\src\\main\\resources\\init\\login.txt"));
        System.out.println(connectionArg);
        Commander.getInstance().establishConnection(connectionArg.get(0),connectionArg.get(1),connectionArg.get(2));
        
                
        
    }


}
