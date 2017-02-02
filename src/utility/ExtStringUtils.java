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
//    private static final double PRECISION = 0.0001;
    
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
        boolean positive = true;
        if(number<0){
            number*=-1;
            positive=false;
        }
        String result = String.valueOf(number);
        while (result.length() < numberOfPositions) {
            result = '0' + result;
        }
        result = result.trim();
        if(!positive){
            result="-"+result;
        }
        return result;
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
    public static boolean equalAmmount(String string,String matches0,String matches1,String... matches){
        int ammount = ExtStringUtils.countMatches(string, matches0);
        int ammount1 = ExtStringUtils.countMatches(string, matches1);
        if(ammount != ammount1){
            return false;
        }else{
            for(String match:matches){
                ammount1 = ExtStringUtils.countMatches(string, match);
                if(ammount != ammount1){
                    return false;
                }
            }
        }
        return true;
    }
    public static double mod(double number,double mod){
        if(Math.abs(mod)<=Double.MIN_NORMAL){
            return 0;
        }
        if(number<0){
            return mod(number + mod,mod);
        }else if(number>=mod){
            return mod(number - mod,mod);
        }else{
            return number;
        }
    }
    public static int mod(int number,int mod){
        if(mod<=0){
            return 0;
        }
        if(number<0){
            return mod(number + mod,mod);
        }else if(number>=mod){
            return mod(number - mod,mod);
        }else{
            return number;
        }
    }
    public static double normalize(double number,int digitsAfterSign){
        double pow = Math.pow(10, digitsAfterSign);
        boolean isNegative = number<0;
        number = Math.abs(number);
        long intPart = (long) number;
        number = number - intPart;
        long doublePart = (long)(number * pow);
        if(isNegative){
            intPart*=-1;
        }
        return (double) intPart + ((double)doublePart/pow);
//        return Double.parseDouble(intPart+"."+doublePart);
//        String numb = String.valueOf(number);
//        int dotIndex = numb.indexOf(".");
//        
//        String intValue = numb.substring(0,dotIndex);
//        numb= numb.substring(dotIndex+1);
//        String realValue = "";
//        for(int i=0; i<digitsAfterSign;i++){
//            if(i>=numb.length()){
//                break;
//            }
//            realValue +=numb.charAt(i);
//        }
//        if(realValue.length()==0){
//            realValue+="0";
//        }
//        return Double.p
    }
    
    
}
