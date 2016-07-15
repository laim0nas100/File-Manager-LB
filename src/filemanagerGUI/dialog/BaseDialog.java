/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import filemanagerGUI.BaseController;
import filemanagerGUI.ViewManager;

/**
 *
 * @author Laimonas Beniušis
 */
public abstract class BaseDialog extends BaseController {
    @Override
    public void exit(){
        ViewManager.getInstance().closeFrame(this.windowID);
        ViewManager.getInstance().updateAllWindows();
    }
}
