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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 * @author Laimonas Beniušis
 */
public class Log extends PrintStream{
    
    private static final Log INSTANCE = new Log();
    protected Log(){
        
        super(new FileOutputStream(FileDescriptor.out));
        
    }
    public static Log getInstance(){        
        return INSTANCE;
    }
    private static char LogType = 'e';
    public void setType(char c){
        Log.LogType = c;
        changeStream(c);
    }
    public static void changeStream (char c,File...file){
        try {
            INSTANCE.out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        switch(c){
            case('f'):{
            try {
                INSTANCE.out = new FileOutputStream(file[0]);
                } catch (FileNotFoundException ex) {
                }
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
    
    public static void write(String...strings){
        for(String s:strings){  
            Log.INSTANCE.print(s);
        }
    }
    public static void writeln(String...strings){
        for(String s:strings){
            Log.INSTANCE.println(s);
        }
    }
    public static void writeln(Object object){
        Log.INSTANCE.println(object);
    }
    public static void write(Object object){
        Log.INSTANCE.print(object);
    }
}
