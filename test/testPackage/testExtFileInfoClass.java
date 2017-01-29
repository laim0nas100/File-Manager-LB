/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testPackage;

import org.junit.Test;
import utility.PathStringCommands;

/**
 *
 * @author lemmin
 */
public class testExtFileInfoClass {
    public testExtFileInfoClass(){};
    
    @Test
    public void test1(){
        String path1 = "file";
        String path2 = "/home/lemmin/Android/MyAlarm/file.lol";
        PathStringCommands info = new PathStringCommands(path1);
        System.out.println(info.getName(false));
        System.out.println(info.getName(true));
//        for(int i=0; i<7;i++){
//            System.out.println(i+" "+info.getParent(i));
//
//        }

    }
}
