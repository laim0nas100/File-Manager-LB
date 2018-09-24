/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums;
import filemanagerLogic.Enums.Identity;
import java.nio.file.Files;
import java.util.*;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lt.lb.commons.Log;
import lt.lb.commons.containers.ObjectBuffer;
import lt.lb.commons.javafx.FXTask;

/**
 *
 * @author Laimonas Beniušis
 */
public class VirtualFolder extends ExtFolder {

    public static String VIRTUAL_FOLDER_PREFIX = "V";

    public static void createVirtualFolder() {
        int index = 0;
        String name = VIRTUAL_FOLDER_PREFIX + index;
        while (FileManagerLB.VirtualFolders.files.containsKey(name)) {
            index += 1;
            name = VIRTUAL_FOLDER_PREFIX + index;
        }
        VirtualFolder VF = new VirtualFolder(FileManagerLB.VIRTUAL_FOLDERS_DIR + name);
        FileManagerLB.VirtualFolders.files.put(name, VF);
    }

    public VirtualFolder(String src) {
        super(src);
        this.populated = true;
    }

    @Override
    public void update() {
        update(FXCollections.observableArrayList(), null);
    }

    @Override
    public void update(ObservableList<ExtPath> list, BooleanProperty isCanceled) {

        if (this.equals(FileManagerLB.VirtualFolders)) {
            list.setAll(this.getFilesCollection());
            return;
        }

        if (this.isAbsoluteRoot.get()) {
            FXTask task = new FXTask() {
                @Override
                protected Void call() throws Exception {
                    Log.print("Start update");
                    FileManagerLB.remountUpdateList = list;
                    FileManagerLB.remount();
                    Log.print("End update");
                    return null;
                }
            };
            task.run();

        } else {
            Iterator<ExtPath> iter = this.getFilesCollection().iterator();
            while (iter.hasNext()) {
                if (!Files.exists(iter.next().toPath())) {
                    iter.remove();
                }
            }
        }

    }

    @Override
    public Collection<ExtPath> getListRecursive(boolean applyDisable) {
        ArrayList<ExtPath> listRecursive = new ArrayList(super.getListRecursive(applyDisable));
        listRecursive.remove(0);
        return listRecursive;
    }

    @Override
    public Enums.Identity getIdentity() {
        return Identity.VIRTUAL;
    }

    public void add(ExtPath file) {
        String name = file.getName(true);
        if (!files.containsKey(name)) {
            files.put(name, file);
        }
    }

    public void addAll(Collection<ExtPath> list) {
        list.forEach(item -> {
            add(item);
        });
    }

    @Override
    public void populateFolder(ObjectBuffer list, BooleanProperty isCanceled) {

    }

    @Override
    public String getAbsoluteDirectory() {
        return this.propertyName.get();
    }

}
