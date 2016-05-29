/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

/**
 *
 * @author Laimonas Beniušis
 */

public class ExtStringUtils extends org.apache.commons.lang3.StringUtils {
    private static final double PRECISION = 0.0001;
    public static class FilterException extends Exception{
        public FilterException(String message){
            super(message);
        }
    }
    public static String parseRegex(String originalName, String regex, String replacement) {
        String result = originalName;
        try {
            result = originalName.replaceAll(regex, replacement).trim();
        } catch (Exception e) {
            throw(e);
        }
        return result;
    }

    public static String parseFilter(String originalName, String filter, long currentNumber){
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

    public static String parseSimple(String originalName, String lookFor, String replacement) {
        return originalName.replace(lookFor, replacement).trim();
    }

    public static String trimEnd(String string) {
        return string.replaceAll("\\s+$", "");
    }

    public static String simpleFormat(long number, int numberOfPositions) {
        String result = String.valueOf(number);
        while (result.length() < numberOfPositions) {
            result = '0' + result;
        }
        return result.trim();
    }
    public static String extractNumber(Number number){
        String result = "";
        Double numb = number.doubleValue();
        Long fullPart = (long)Math.floor(numb);
        numb = numb - fullPart;
        if(fullPart>0){
            while(fullPart>0){
                result = (fullPart % 10) + result;
                fullPart/=10;
            }
        }else{
            result = "0";        
        }
        String numbS = String.valueOf(numb);
        int index = numbS.indexOf('.');
        result +=numbS.substring(index,Math.min(numbS.length(),index+4));
        
        return result;
    }
    
}
