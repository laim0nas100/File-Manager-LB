/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.utility;

import lt.lb.commons.parsing.StringOp;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtStringUtils extends lt.lb.commons.parsing.StringOp {

    public static class FilterException extends Exception {

        public FilterException(String message) {
            super(message);
        }
    }

    public static String parseRegex(String originalName, String regex, String replacement) {
        return originalName.replaceAll(regex, replacement).trim();
    }

    public static String parseFilter(String originalName, String filter, long currentNumber) {
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

    public static boolean equalAmmount(String string, String matches0, String matches1, String... matches) {
        int ammount = StringOp.countMatches(string, matches0);
        int ammount1 = StringOp.countMatches(string, matches1);
        if (ammount != ammount1) {
            return false;
        } else {
            for (String match : matches) {
                ammount1 = ExtStringUtils.countMatches(string, match);
                if (ammount != ammount1) {
                    return false;
                }
            }
        }
        return true;
    }

    
}
