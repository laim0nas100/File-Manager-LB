package lt.lb.filemanagerlb.utility;

import java.util.Set;
import lt.lb.commons.iteration.streams.MakeStream;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class ExtStringUtils  {

    public static double fuzzyScore(String s1, String s2) {
        return FuzzySearch.ratio(s1, s2) / 100d;
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

    public static String normalizeWhitespace(String originalName, Character... deleteSpaceBeforeSymbol) {
        String normalizeSpace = StringUtils.normalizeSpace(originalName);

        if (deleteSpaceBeforeSymbol.length == 0 || normalizeSpace.length() <= 1) {
            return normalizeSpace;
        }
        int[] codePoints = normalizeSpace.codePoints().toArray();

        Set<Integer> symbols = MakeStream.from(deleteSpaceBeforeSymbol).map(m -> (int) m).toSet();
        StringBuilder sb = new StringBuilder(codePoints.length);
        int prev = codePoints[0];
        for (int i = 1; i < codePoints.length; i++) {
            int current = codePoints[i];
            if (!(Character.isWhitespace(prev) && symbols.contains(current))) {
                sb.appendCodePoint(prev);
                //otherwise a space and forbidden symbol, don't include
            }
            prev = current;
        }
        sb.appendCodePoint(prev);
        return sb.toString().trim();

    }
}
