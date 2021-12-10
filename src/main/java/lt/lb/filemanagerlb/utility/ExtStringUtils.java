package lt.lb.filemanagerlb.utility;

import java.util.HashMap;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtStringUtils extends StringUtils{
    
    public static class StringInfo {

        public String string;
        public int length;
        public HashMap<Character, Integer> map;
        public HashMap<Character, Integer> mapIgnoreCase;

        public StringInfo(String str) {
            this.string = str;
            this.length = string.length();
            this.map = new HashMap<>();
            this.mapIgnoreCase = new HashMap<>();
            for (char ch : string.toCharArray()) {
                if (map.containsKey(ch)) {
                    Integer get = map.get(ch);
                    get++;
                    map.replace(ch, get);
                } else {
                    map.put(ch, 1);
                }
            }
            for (char ch : string.toUpperCase().toCharArray()) {
                if (mapIgnoreCase.containsKey(ch)) {
                    Integer get = mapIgnoreCase.get(ch);
                    get++;
                    mapIgnoreCase.replace(ch, get);
                } else {
                    mapIgnoreCase.put(ch, 1);
                }
            }
        }

        @Override
        public String toString() {
            String s = "";
            for (char c : map.keySet()) {
                s += "[" + c + " " + map.get(c) + "]";
            }
            s += "\n";
            for (char c : mapIgnoreCase.keySet()) {
                s += "[" + c + " " + mapIgnoreCase.get(c) + "]";
            }
            return s;
        }
    }

    public static double correlationRatio(String s1, String s2) {
        // max combinations n*(1/6)*(n+1)*(n+2)
        double totalCount = 0;
        double n1 = s1.length();
        double n2 = s2.length();
        n1 = n1 * (n1 + 1) * (n1 + 2) / 6;
        n2 = n2 * (n2 + 1) * (n2 + 2) / 6;
        totalCount += correlationRatio2(s1, s2) / n1;
        totalCount += correlationRatio2(s2, s1) / n2;
        String us1 = s1.toUpperCase(Locale.ROOT);
        String us2 = s2.toUpperCase(Locale.ROOT);
        totalCount += correlationRatio2(us1, us2) / n1;
        totalCount += correlationRatio2(us2, us1) / n2;
        return totalCount / 4;
    }

    private static long correlationRatio2(String s1, String s2) {
        long count = 0;
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = i + 1; j <= s1.length(); j++) {
                String substring = s1.substring(i, j);
                if (s2.contains(substring)) {
                    int addition = substring.length();
                    count += addition;
                }
            }
        }
        return count;
    }

    public static String trimEnd(String string) {
        return string.replaceAll("\\s+$", "");
    }

    public static String simpleFormat(long number, int numberOfPositions) {
        boolean positive = true;
        if (number < 0) {
            number *= -1;
            positive = false;
        }
        String result = String.valueOf(number);
        while (result.length() < numberOfPositions) {
            result = '0' + result;
        }
        result = result.trim();
        if (!positive) {
            result = "-" + result;
        }
        return result;
    }

    public static String extractNumber(Number number) {
        String result = "";
        Double numb = number.doubleValue();
        Long fullPart = (long) Math.floor(numb);
        numb = numb - fullPart;
        if (fullPart > 0) {
            while (fullPart > 0) {
                result = (fullPart % 10) + result;
                fullPart /= 10;
            }
        } else {
            result = "0";
        }
        String numbS = String.valueOf(numb);
        int index = numbS.indexOf('.');
        result += numbS.substring(index, Math.min(numbS.length(), index + 4));

        return result;
    }
    

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
        return StringUtils.trim(newName);
    }

    public static String parseSimple(String originalName, String lookFor, String replacement) {
        return originalName.replace(lookFor, replacement).trim();
    }

    public static boolean equalAmmount(String string, String matches0, String matches1, String... matches) {
        int ammount = StringUtils.countMatches(string, matches0);
        int ammount1 = StringUtils.countMatches(string, matches1);
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
