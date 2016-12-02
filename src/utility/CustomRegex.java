/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import static utility.ExtStringUtils.simpleFormat;
import static utility.ExtStringUtils.trimEnd;

/**
 *
 * @author Lemmin
 * 
 * 
 */
public class CustomRegex {
    public static String regexIndicator = "<i>";
    public static String regexNumberStart = "<num>";
    public static String regexNumberEnd = "</num>";
    public static String regexNumberChar = "#";//
    public static String regexOriginalName = "?";
    public static String regexOriginalNameNoExtension = "*";
 
    public static String regexNull = "<null>";
    public static String removeNulls(String string){
        return string.replaceAll(regexNull,"");
    }
    public static class CRED{//CustomREgexDescriptor
        public String regexStart;
        public String regexEnd;
        public String regexId;
        public boolean isAtom;
        public CRED(boolean isAtom, String... startEnd){
            this.isAtom = isAtom;
            this.regexId = regexIndicator;
            this.regexStart = startEnd[0];
            if(!isAtom && startEnd.length>1){
                this.regexEnd = startEnd[1];
            }
        }
        public String apply(String string, String replaceWith) throws Exception{
            int countStart,countEnd = 0;
            countStart = ExtStringUtils.countMatches(string, this.regexStart);
            if (countStart==0){
                System.out.println("invoke 1");
                return string;
            }
            if (this.isAtom){
                for(int i=0; i<countStart; i++){
                    string = ExtStringUtils.replaceOnce(string, this.regexStart, replaceWith);
                }
                System.out.println("invoke 2");

                return string;
            }else {
                countEnd = ExtStringUtils.countMatches(string, this.regexEnd);
                if (countStart!=countEnd){
                    throw new Exception("Different ammount of start and end tags");
                }
                if(countStart == 1){
                    int start,end;
                    start = string.indexOf(this.regexStart);
                    end = string.indexOf(this.regexEnd);
                    String stringBefore = string.substring(0, start);//exclude start tag
                    String stringAfter = string.substring(end+this.regexEnd.length());//exclude end tag
                    String n = string.substring(start+this.regexStart.length(), end);
                    n = ExtStringUtils.replaceOnce(n, this.regexId, replaceWith);
                    System.out.println("invoke 3");

                    return stringBefore + n + stringAfter;
                }else{
                    System.out.println("invoke 4");
                    return replaceNestedRegex(this,string,replaceWith);
                } 
            }
        }
        public static String replaceAtomRegex(CRED cred, String string, String replaceWith){
            return ExtStringUtils.replace(string, cred.regexStart, replaceWith);
        }
        public static String replaceRegex(CRED cred, String string, String replaceWith){
            int start,end;
            start = string.indexOf(cred.regexStart);
            end = string.indexOf(cred.regexEnd);
            String stringBefore = string.substring(0, start);//exclude start tag
            String stringAfter = string.substring(end+cred.regexEnd.length());//exclude end tag
            String n = string.substring(start+cred.regexStart.length(), end);
            n = ExtStringUtils.replaceOnce(n, cred.regexId, replaceWith);
            return stringBefore + n + stringAfter;
        }
        public static String replaceNestedRegex(CRED cred, String string, String replaceWith) throws Exception{
            String temp = string;
            int countStart,countEnd = 0;
            countStart = ExtStringUtils.countMatches(string, cred.regexStart);
            countEnd = ExtStringUtils.countMatches(string, cred.regexEnd);
            if (countStart!=countEnd){
                   throw new Exception("Different ammount of start and end tags");
            }
            //first instance of start tag
            int start,end = 0;
            int iteration = 0;
            while(true){
                System.out.println("Iteration "+iteration++);
                String before = "";
                String afterward = "";
                while(true){
                    try{
                        
                        start = ExtStringUtils.indexOf(temp, cred.regexStart);
                        end = ExtStringUtils.indexOf(temp, cred.regexEnd);
                        before= temp.substring(0, start);
                        afterward= temp.substring(end+cred.regexEnd.length());
                        temp = temp.substring(start, end+cred.regexEnd.length());
                        System.out.println("After replace:"+before+","+temp+","+afterward);

                        boolean equalAmmount = ExtStringUtils.equalAmmount(temp, cred.regexStart, cred.regexEnd);
                        if(equalAmmount){
                            System.out.println(string+" temp:"+temp);
                            System.out.println(before+","+temp+","+afterward);

                            System.out.println(start+","+end);
                            start = ExtStringUtils.indexOf(temp, cred.regexStart);
                            end = ExtStringUtils.indexOf(temp, cred.regexEnd);
                            
                            break;
                        }else{
                            temp =before+ ExtStringUtils.replaceOnce(temp,cred.regexStart, regexNull) + afterward;
                            

                        }
                    }catch(Exception e){
                        throw new Exception("Different ammount of start and end tags");
                    }

                }
                String stringBefore = temp.substring(0, start);//exclude start tag
                String stringAfter = temp.substring(end+cred.regexEnd.length());//exclude end tag
                String n = temp.substring(start+cred.regexStart.length(), end);
                n = ExtStringUtils.replaceOnce(n, cred.regexId, replaceWith);
                string = before + n + afterward;
                string = ExtStringUtils.replace(string, regexNull, cred.regexStart);
                countStart = ExtStringUtils.countMatches(string, cred.regexStart);
                if(countStart == 0){
                    break;
                }else{
                    temp = string;
                }
            }
            
            return string;
        }
        
    }
    public static class CNRED extends CRED{
        
        public CNRED(boolean isAtom, String... startEnd) {
            super(isAtom, startEnd);
        }
        
    }
    
    public static String parseFilter(String originalName, String filter, long currentNumber){
        String originalNameNoExtension = originalName.substring(0, originalName.lastIndexOf("."));
        CRED origNameNoExt = new CRED(true,regexOriginalNameNoExtension);
        CRED origName = new CRED(true,regexOriginalName);

        int numerationAmmount = 0;
        String newName = "";
        boolean preWasH = false;
        for (int i = 0; i < filter.length(); i++) {
            char c = filter.charAt(i);
            
            if (c == '#') {
                if (preWasH) {
                    numerationAmmount++;
                } else {
                    numerationAmmount = 1;
                    preWasH = true;
                }
            } else {
                if (preWasH) {
                    preWasH = false;
                    newName += simpleFormat(currentNumber, numerationAmmount);
                }
                if (c == '?') {
                    newName += originalName;
                } else {
                    newName += c;
                }
            }
        }
        if (preWasH) {
            newName += simpleFormat(currentNumber, numerationAmmount);
        }
        return trimEnd(newName);
    }
    public static String parseFilterOld(String originalName, String filter, long currentNumber){
        int numerationAmmount = 0;
        String newName = "";
        boolean preWasH = false;
        for (int i = 0; i < filter.length(); i++) {
            char c = filter.charAt(i);
            if (c == '#') {
                if (preWasH) {
                    numerationAmmount++;
                } else {
                    numerationAmmount = 1;
                    preWasH = true;
                }
            } else {
                if (preWasH) {
                    preWasH = false;
                    newName += simpleFormat(currentNumber, numerationAmmount);
                }
                if (c == '?') {
                    newName += originalName;
                } else {
                    newName += c;
                }
            }
        }
        if (preWasH) {
            newName += simpleFormat(currentNumber, numerationAmmount);
        }
        return trimEnd(newName);
    }
    
}
