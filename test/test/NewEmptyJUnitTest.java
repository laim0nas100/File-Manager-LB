/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import LibraryLB.AverageKeepingList;
import filemanagerGUI.customUI.CosmeticsFX.MenuTree;
import java.util.LinkedList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void test1(){
        MenuItem item0 = new MenuItem("Item0");
        Menu menu0 = new Menu("Menu0");
        Menu menu1 = new Menu("Menu1");
        MenuItem item1 = new MenuItem("Item1");
        MenuItem item2 = new MenuItem("Item2");
        MenuTree map = new MenuTree(menu0);
        map.addMenuItem(item0, "Item0");
        map.addMenuItem(menu1, "Menu1");
        map.addMenuItem(item1, "Menu1","Item1");
        map.addMenuItem(item2, "Item2");
        System.out.println(map);
    }
    //@Test
    public void test0(){
        double average = 0;
        LinkedList<Integer> list = new LinkedList<>();
        list.add(1);
        average = ajustAverageAdd(average,list.size(),1);
        list.add(5);
        average = ajustAverageAdd(average,list.size(),5);
        list.add(6);
        average = ajustAverageAdd(average,list.size(),6);
        list.add(6);
        average = ajustAverageAdd(average,list.size(),6);
        System.out.println(averageNormal(list));
        System.out.println(average);
        list.removeLast();
        average = ajustAverageRemove(average,list.size(),6);
        System.out.println(averageNormal(list));
        System.out.println(average);
        System.out.println("==========");
        AverageKeepingList<Integer> l = new AverageKeepingList<>();
        
        l.add(1);
        l.add(5);
        l.add(6);
        l.add(6);
        System.out.println(l.average);
        l.remove(3);
        System.out.println(l.average);

        
    }
    public double averageNormal(LinkedList<Integer> list){
        double av = 0;
        for(Integer i:list){
            av+=i;
        }
        return av/list.size();
    }
    public double ajustAverageAdd(double currentAverage,int size, int newElem){
        currentAverage = (double)currentAverage*(size-1)/(size)+ (double)newElem/size;
        return currentAverage;
    }
    public double ajustAverageRemove(double currentAverage,int size, int newElem){
        currentAverage = (double)currentAverage*(size+1)/(size) - (double)newElem/size;
        return currentAverage;
    }
    
}
