/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.ArrayList;

/**
 *
 * @author Laimonas Beniušis
 * Text Manager for managing logs or messages for input by line
 */
public class TextMan {
    private ArrayList<String> textLines;
    public TextMan(){
        textLines = new ArrayList<String>();
    }
    public TextMan(ArrayList<String> list){
        this.textLines = list;
    }
    public void addLn(String...strings){
        for(String s:strings){
            textLines.add(s);
        }
    }
    public void add(String...strings){
        for(String s:strings){
            if(textLines.isEmpty()){
                addLn(s);
            }else{
                int index = textLines.size()-1;
                String line = textLines.get(index);
                line += s;
                textLines.set(index,line);
            }
            
        }
    }
    public String toBigString(){
        String bigString = "";
        for(String s:textLines){
            bigString += s + "\n";
        }
        return bigString;
    }
    public ArrayList<String> getList(){
        return this.textLines;
    }
}
