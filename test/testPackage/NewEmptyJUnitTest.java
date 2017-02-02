/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testPackage;

import filemanagerLogic.fileStructure.VirtualFolder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import LibraryLB.Parsing.CustomRegex;
import utility.ExtStringUtils;
import static utility.ExtStringUtils.mod;
import static utility.ExtStringUtils.normalize;
import utility.PathStringCommands;

/**
 *
 * @author Laimonas Beniušis
 */
public class NewEmptyJUnitTest {

    public NewEmptyJUnitTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    public void test0() throws InterruptedException, IOException{
        ProcessBuilder builder = new ProcessBuilder(
            "cmd.exe", "/c", "cd \"C:\\Program Files\\\" && dir");
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }
    
    @Test
    public void test1(){
        print(mod(5,5));
        print(mod(0.055,0.05));
        print(normalize(123.55555,2));
        PathStringCommands path = new PathStringCommands("C:\\");
        print(path.getName(true));
        
    }
 
    public static void print(Object...o){
        for(Object ob:o){
            System.out.println(ob);
        }
    }
}
