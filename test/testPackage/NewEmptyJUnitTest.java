/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static utility.ExtStringUtils.mod;
import static utility.ExtStringUtils.normalize;
import utility.PathStringCommands;

/**
 *
 * @author Laimonas Beniušis
 */
public class NewEmptyJUnitTest {
    Logger logger;
    public NewEmptyJUnitTest() {
        this.logger = LoggerFactory.getLogger(NewEmptyJUnitTest.class );
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
//        print("123".substring(0, 10));
        
    }
 
    public static void print(Object...o){
        for(Object ob:o){
            System.out.println(ob);
        }
    }
}
