/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Laimonas Beniušis
 */
public class Log extends PrintStream{
    
    private static final Log INSTANCE = new Log();
    private final ArrayList<String> list;
    protected Log(){
        
        super(new FileOutputStream(FileDescriptor.out));
        list = new ArrayList<>();
    }
    public static Log getInstance(){        
        return INSTANCE;
    }
    public static void changeStream(char c,File...file){
        try {
            INSTANCE.out.close();
        } catch (IOException ex) {
            ex.printStackTrace();          
        }
        switch(c){
            case('f'):{
                try {
                    INSTANCE.out = new FileOutputStream(file[0]);
                } catch (FileNotFoundException ex) {}
                break;
            }
            case('e'):{
                
                INSTANCE.out = new FileOutputStream(FileDescriptor.err);
                break;
            }
            default:{
                INSTANCE.out = new FileOutputStream(FileDescriptor.out);
                break;
            }
        }
    }
    
    public static void write(Object...objects){
        String string = "";
        for(Object s:objects){  
            string+=s.toString();
        }
        Log.writeln(string);
    }
    public static void writeln(Object...objects){
            for(Object s:objects){
                Log.INSTANCE.println(s);
                Log.INSTANCE.list.add(s.toString());
            } 
        
    }
    public static void printProperties(Properties properties){
        Object[] toArray = properties.keySet().toArray();
        
        for(Object o:toArray){
            String property = properties.getProperty((String) o);
            writeln(o.toString()+" : "+property);
        }
    }
}
