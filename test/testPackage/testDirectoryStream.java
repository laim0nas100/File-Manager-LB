/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testPackage;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.junit.Test;

/**
 *
 * @author lemmin
 */
public class testDirectoryStream {
    public testDirectoryStream(){
        
    }
    
    @Test
    public void test() throws IOException{
        System.out.println("Test");
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("/home/lemmin/"));
        Iterator<Path> iterator = dirStream.iterator();
        
        while(iterator.hasNext()){
            System.out.println(iterator.next());
        }
        File f = new File("/home/lemmin/");
        for(String s :f.list()){
            System.out.println(s);
        }
    }
}
