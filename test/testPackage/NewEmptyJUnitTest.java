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
import utility.CustomRegex;
import utility.CustomRegex.CRED;
import utility.ExtStringUtils;

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
    public void test1() throws Exception{

        CRED c = new CRED(false,"<t>","</t>");
        CRED c1 = new CRED(false,"<r>","</r>");

        String t2 = "2 <t>sometext</t>";
        print(c.apply(t2, "k"));
        String t3 = "3 <t>some</t><t>text</t>";     
        print(c.apply(t3, "k"));
        String t4 = "4 <t>some<t>text</t>ok</t>";
        print(c.apply(t4, "k"));
        String t5 = "5 <t>some<r>text<i></r><i>that<r><i>is</r>advanced</t>";
        print(c.apply(t5, "k"));
        String f = "file.txt";
        print(f.substring(0, f.lastIndexOf(".")));
        
//        print(ExtStringUtils.countMatches(t3, "<t>"));
//        print(ExtStringUtils.countMatches(t3, "</t>"));
        
        
//        print(t.substring(t.indexOf("<tag>")+5));
        
        
    }
    public static void print(Object...o){
        for(Object ob:o){
            System.out.println(ob);
        }
    }
}
