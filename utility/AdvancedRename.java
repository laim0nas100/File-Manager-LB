/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import static filemanagerGUI.FileManagerLB.reportError;

/**
 *
 * @author Laimonas Beniušis
 */
public class AdvancedRename {
    public static class FilterException extends Exception{
        public FilterException(String message){
            super(message);
        }
    }

    public static String parseFilter(String originalName, String filter, long currentNumber) throws FilterException{
        int numerationAmmount = 0;
        String newName = "";
        boolean preWasH = false;
        if(originalName.contains("?")){
            throw new FilterException("original name contains '?'");
        }
        for(int i=0; i<filter.length(); i++){
            char c = filter.charAt(i);
            
            if(c =='#'){
                if(preWasH){
                    numerationAmmount++;
                }else{
                    numerationAmmount = 1;
                    preWasH = true;
                }
            }else{
                if(preWasH){
                    preWasH = false;
                    newName+=simpleFormat(currentNumber,numerationAmmount);
                }
                if(c =='?'){
                    newName+=originalName;
                }else{
                    newName+=c;       
                }
            } 
        }
        if(preWasH){
            newName+=simpleFormat(currentNumber,numerationAmmount);
        }
        return trimEnd(newName);
    }
    public static String parseRegex(String originalName, String regex, String replacement){
        String result = originalName;
        try{
            result= originalName.replaceAll(regex, replacement).trim();
        }catch(Exception e){
            reportError(e);
        }
        return result;
    }
    public static String parseSimple(String originalName, String lookFor, String replacement){
        return originalName.replace(lookFor, replacement).trim();
    }
    public static String simpleFormat(long number, int numberOfPositions){
        String result = String.valueOf(number);
        while(result.length()<numberOfPositions){
            result = '0'+result;
        }
        return result.trim();
    }
    public static String trimEnd(String string){
        return string.replaceAll("\\s+$", "");
    }
}
