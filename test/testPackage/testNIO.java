/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testPackage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 *
 * @author Lemmin
 */
public class testNIO {
    public testNIO(){};
    
    @Test
    public void test1() throws IOException{
        Path p = Paths.get("E:\\Dev\\Scripts");
        System.out.println(Files.getLastModifiedTime(p));
        System.out.println(p.toRealPath().toString());
    }
}
