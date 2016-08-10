package it.sam.diningphil.samproject.DataStructures;

import android.graphics.drawable.Drawable;

public class Group {

    public String getName() {
        return name;
    }

    public boolean isFree() {
        return isFree;
    }

    public Drawable getGroupImage() {
        return groupImage;
    }

    String name;
    boolean isFree;
    Drawable groupImage;

    public Group(String name, boolean isFree, Drawable groupImage) {
        this.name = name;
        this.isFree = isFree;
        this.groupImage = groupImage;
    }
}
