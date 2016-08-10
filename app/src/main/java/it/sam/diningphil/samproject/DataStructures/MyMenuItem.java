package it.sam.diningphil.samproject.DataStructures;


public class MyMenuItem {

    public int getItemImageId() {
        return itemImageId;
    }

    public String getName() {
        return name;
    }

    private int itemImageId;
    private String name;

    public MyMenuItem(int imageId, String name){
        this.name = name;
        itemImageId = imageId;
    }
}
