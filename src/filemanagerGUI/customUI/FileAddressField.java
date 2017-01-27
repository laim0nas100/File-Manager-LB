/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.customUI;

import filemanagerLogic.fileStructure.ExtFolder;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import utility.ExtStringUtils;
import LibraryLB.Containers.LoopingList;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileAddressField{
    private LoopingList<String> list;
    public TextField field;
    public ExtFolder folder;
    public String f;
    public FileAddressField(TextField Tfield){
        list = new LoopingList<>();
        this.field = Tfield;
        this.field.setOnKeyReleased(new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent t) {
                t.consume();
                KeyCode code = t.getCode();
                if(code.equals(KeyCode.DOWN)||code.equals(KeyCode.UP)){
                    //Log.writeln("FileAddressField invoked");
                    String text;
                    if(f == null){
                        text = field.getText();
                    }else{
                        text = f;
                    }
                    list.clear();
                    folder.getFoldersFromFiles().forEach(fold->{
                        list.add(fold.propertyName.get());
                    });
                    String name = ExtStringUtils.replaceOnce(text, folder.getAbsoluteDirectory(), "");
                    int index = 0;
                    while(index<list.size()){
                        String s;
                        if(code.equals(KeyCode.DOWN)){
                            s = list.next();
                        }else{
                            s = list.prev();
                        }
                        index++;
                        if(ExtStringUtils.startsWithIgnoreCase(s, name)){
                            Platform.runLater(()->{
                                f = name;
                                field.setText(folder.getAbsoluteDirectory()+s);
                                field.positionCaret(field.getLength());
                            });
                            break;
                        } 
                    }                        
                }else{
                    f=null;
                }
            }
            
        });
    }
}
