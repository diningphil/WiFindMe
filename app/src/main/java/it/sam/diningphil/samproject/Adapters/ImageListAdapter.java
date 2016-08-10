package it.sam.diningphil.samproject.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.lang.Integer;
import android.widget.ImageView;

import java.util.ArrayList;

import it.sam.diningphil.samproject.R;

public class ImageListAdapter extends ArrayAdapter<Integer> {

    private ArrayList<Integer> imagesDataset;

    public ImageListAdapter(Context context, int resource) {
        super(context, resource);

        imagesDataset = new ArrayList<>();

        imagesDataset.add(R.drawable.red_nine);
        imagesDataset.add(R.drawable.orange_nine);
        imagesDataset.add(R.drawable.yellow_nine);
        imagesDataset.add(R.drawable.light_green_nine);
        imagesDataset.add(R.drawable.light_blue_nine);
        imagesDataset.add(R.drawable.dark_blue_nine);
        imagesDataset.add(R.drawable.purple_nine);

    }

    @Override
    public int getCount() {
        return imagesDataset.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_dialog, parent, false);

        ImageView iV = (ImageView) v.findViewById(R.id.dialog_image);

        iV.setImageResource(imagesDataset.get(position));

        return v;
    }

    public ArrayList<Integer> getImageDataset(){ return imagesDataset; }


}
