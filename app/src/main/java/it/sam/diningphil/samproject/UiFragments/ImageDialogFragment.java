package it.sam.diningphil.samproject.UiFragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import it.sam.diningphil.samproject.Adapters.ImageListAdapter;
import it.sam.diningphil.samproject.ImageSelectedInterface;
import it.sam.diningphil.samproject.MainActivity;
import it.sam.diningphil.samproject.R;


public class ImageDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener{

    private ImageListAdapter imageListAdapter;
    private ImageSelectedInterface callback;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (ImageSelectedInterface) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().setTitle(getResources().getString(R.string.pick_a_color));

        View v = inflater.inflate(R.layout.image_dialog, container, false);
        ListView lv = (ListView) v.findViewById(R.id.image_dialog_listview);

        if(imageListAdapter == null)
            imageListAdapter = new ImageListAdapter(getActivity(), R.layout.item_image_dialog);

        lv.setOnItemClickListener(this);

        lv.setAdapter(imageListAdapter); // Non contiene TextView, vediamo che succede

        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        
        int imageId = imageListAdapter.getImageDataset().get(position);
        
        callback.updateSelectedImage(imageId);
    }
}
