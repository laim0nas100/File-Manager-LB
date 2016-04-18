/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
        public Finder(String pattern,ObservableList list,BooleanProperty property) {
            setUp();
            this.list = list; 
            useRegex = new SimpleBooleanProperty(false);
            useRegex.bind(property);
        }
        public void newTask(String pattern){
            patternStr = pattern.toLowerCase(Locale.ROOT);
            noRegex = true;
            
            if(hasRegexChar(pattern)){
                try{
                    this.pattern = Pattern.compile(pattern);
                    noRegex = false;
                }catch(Exception e){

                }
            }
            
            if(list==null){
                list = FXCollections.observableArrayList();
            }
        }

        public void find(Path file) {
            if (file.getFileName() != null){
                String str = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if(noRegex){
                    if(str.contains(patternStr)){
                        list.add(file.toString());
                    }
                }else{
                    Matcher matcher = pattern.matcher(str);
                    if(matcher.matches()){
                        list.add(file.toString());
                    }
                }    
            }
        }
        // Invoke the pattern matching
        // method on each file.
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }
        
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
        private final void setUp(){
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
                ',',
                '+',
                '|',
                '?'
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

