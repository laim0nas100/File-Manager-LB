/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lt.lb.commons.javafx.FX;

/**
 *
 * @author Laimonas Beniušis
 */
public class Finder extends SimpleFileVisitor<Path> {
    
        public ObservableList<String> list;
        private HashSet<Character> regexSet;
        private String patternStr;
        private boolean noRegex;
        private Pattern pattern;
        public SimpleBooleanProperty useRegex;
        public SimpleBooleanProperty isCanceled;
        public Finder(String pattern,BooleanProperty property) {
            setUp();
            useRegex = new SimpleBooleanProperty(false);
            isCanceled = new SimpleBooleanProperty(false);
            useRegex.bind(property);
            list = FXCollections.observableArrayList();
        }
        public void newTask(String pattern){
            patternStr = pattern.toLowerCase(Locale.ROOT);
            noRegex = true;
            if(useRegex.get() && hasRegexChar(pattern)){
                try{
                    this.pattern = Pattern.compile(pattern);
                    noRegex = false;
                }catch(Exception e){}
            }
        }

        public void find(Path file) {
            if (file.getFileName() != null){
                String str = file.getFileName().toString();
                boolean matches = false;
                if(noRegex){
                    matches = str.toLowerCase().contains(patternStr);
                }else{
                    Matcher matcher = pattern.matcher(str);
                    matches = matcher.matches();
                }
                if(matches){
                    FX.submit(()->{
                        String toAdd = file.toAbsolutePath().toString();
                        if(!list.contains(toAdd)){
                            list.add(toAdd);
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
            if(isCanceled.get()){
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            if(isCanceled.get()){
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            if(isCanceled.get()){
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
        private void setUp(){
            Character[] array = new Character[] {
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
            };
            regexSet = new HashSet<>();
            regexSet.addAll(Arrays.asList(array));
        }
        private boolean hasRegexChar(String regex){  
            for(char c:regex.toCharArray()){
                if(this.regexSet.contains(c)){
                    return true;
                }
            }
            return false;     
        }
        
    }

