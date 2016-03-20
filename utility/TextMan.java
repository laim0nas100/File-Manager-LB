/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.Vector;

/**
 *
 * @author Laimonas Beniušis
 * Text Manager for managing logs or messages for input by line
 */
public class TextMan {
    private Vector<String> textLines;
    public TextMan(){
        textLines = new Vector<String>();
    }
    public TextMan(Vector<String> list){
        this.textLines = list;
    }
    public void addLn(String...strings){
        for(String s:strings){
            textLines.add(s);
        }
    }
    public void add(String...strings){
        for(String s:strings){
            if(textLines.size()==0){
                addLn(s);
            }else{
                int index = textLines.size()-1;
                String line = textLines.get(index);
                line += s;
                textLines.setElementAt(line, index);
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
    public Vector<String> getList(){
        return this.textLines;
    }
}
