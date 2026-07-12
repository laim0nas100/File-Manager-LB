package lt.lb.filemanagerlb.utility;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import lt.lb.commons.iteration.streams.MakeStream;
import lt.lb.commons.javafx.FX;

/**
 *
 * @author laim0nas100
 */
public class Finder extends SimpleFileVisitor<Path> {

    private static final Set<Character> regexCharSet = MakeStream.fromValues(
            '\\',
            '[',
            ']',
            '*',
            '^',
            '$',
            '?',
            '{',
            '}',
            '(',
            ')',
            ',',
            '+',
            '|',
            '?',
            '.'
    ).toSet();

    public ObservableList<String> list;
    private String patternStr;
    private boolean noRegex;
    private Pattern pattern;
    public final boolean useRegex;
    public SimpleBooleanProperty isCanceled;

    public Finder(String pattern, boolean useRegex, ObservableList<String> sink) {
        this.useRegex = useRegex;
        isCanceled = new SimpleBooleanProperty(false);
        list = sink;
        patternStr = pattern.toLowerCase(Locale.ROOT);
        noRegex = true;
        if (useRegex && hasRegexChar(pattern)) {
            try {
                this.pattern = Pattern.compile(pattern);
                noRegex = false;
            } catch (Exception e) {
            }
        }
    }

    public void find(Path file) {
        if (file.getFileName() != null) {
            String str = file.getFileName().toString();
            boolean matches = false;
            if (noRegex) {
                matches = str.toLowerCase().contains(patternStr);
            } else {
                matches = pattern.matcher(str).matches();
            }
            if (matches) {
                FX.submit(() -> {
                    if (!isCanceled.get()) {
                        list.add(file.toAbsolutePath().toString());
                    }
                });
            }
        }
    }
    // Invoke the pattern matching
    // method on each file.

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        find(file);
        if (isCanceled.get()) {
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    // Invoke the pattern matching
    // method on each directory.
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        find(dir);
        if (isCanceled.get()) {
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (isCanceled.get()) {
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    private static boolean hasRegexChar(String regex) {
        for (char c : regex.toCharArray()) {
            if (regexCharSet.contains(c)) {
                return true;
            }
        }
        return false;
    }

}
