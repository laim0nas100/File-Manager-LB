/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.filemanagerlb.utility;

import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Tooltip;
import lt.lb.filemanagerlb.logic.LocationAPI;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;

/**
 *
 * @author Laimonas Beniušis
 */
public class FavouriteLink {

    private final SimpleStringProperty propertyName;
    public final ExtPath location;

    public FavouriteLink(String name, ExtPath dir) {
        propertyName = new SimpleStringProperty(name);
        location = dir;
    }

    public FavouriteLink(String absoluteDir) {
        ExtPath fileOptimized = LocationAPI.getInstance().getFileOptimized(absoluteDir);
        Objects.requireNonNull(fileOptimized);
        location = fileOptimized;
        propertyName = new SimpleStringProperty(location.getName(false));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.location);
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FavouriteLink) {
            FavouriteLink link = (FavouriteLink) other;
            return location.equals(link.location);
        }
        return false;
    }

    public Tooltip getToolTip() {
        Tooltip tltp = new Tooltip();
        tltp.setText(this.location.getAbsoluteDirectory());
        return tltp;
    }

    public SimpleStringProperty getPropertyName() {
        return this.propertyName;
    }

}
