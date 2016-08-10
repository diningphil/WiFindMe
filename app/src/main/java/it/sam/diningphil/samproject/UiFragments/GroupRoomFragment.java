package it.sam.diningphil.samproject.UiFragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.MapFragment;

import it.sam.diningphil.samproject.MainActivity;

public class GroupRoomFragment extends MapFragment{

    public GroupRoomFragment(){
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
           This method must be called from the main thread.
           The callback will be executed in the main thread.
           In the case where Google Play services is not installed on the user's device, the callback will not be triggered until the user installs it.
         */
        getMapAsync( (MainActivity) getActivity());

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
