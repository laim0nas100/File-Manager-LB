/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author Laimonas Beniušis
 */
public class ExtLink extends ExtFile{
    
    public ExtLink(String link){
        super(link);
    }
    
    
    
    @Override
    public ExtLink getTrueForm(){
        return this;
    }
    @Override
    public String getIdentity(){
        return "link";
    }
}
