/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author Laimonas Beniušis
 */
public class MultiLinedString{
    public LinkedList<String> list;
    public MultiLinedString(String...str){
        list = new LinkedList<>();
        list.addAll(Arrays.asList(str));
    }
    public void addLines(String...str){
        list.addAll(Arrays.asList(str));
    }
    public String getLines(){
        String s = "";
        for(String st:list){
            s+=st+"\n";
        }
        return s;
    }
    public void clear(){
        this.list.clear();
    }
    public MultiLinedString(MultiLinedString str){
        list = new LinkedList<>();
        for(String s:str.list){
            list.add(s);
        }
    }
    public char getCharAt(int line,int index){
        return list.get(line).charAt(index);
    }
}
