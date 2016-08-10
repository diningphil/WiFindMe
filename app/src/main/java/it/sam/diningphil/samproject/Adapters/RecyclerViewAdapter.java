package it.sam.diningphil.samproject.Adapters;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.util.List;

import it.sam.diningphil.samproject.DataStructures.Group;
import it.sam.diningphil.samproject.R;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String GROUP_SELECTED_INTENT_ACTION = "Saverio";
    private static final String GROUP_NAME = "GROUP_NAME";

    private List<Group> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    // The adapter receives a list ( or it fetches ), his job is to place it on views
    public RecyclerViewAdapter(List<Group> myDataset) {
        mDataset = myDataset;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public CardView cv;
        public TextView groupName;
        public TextView isGroupFree;
        public ImageView groupImage;
        public Button joinButton;

        public static Intent groupIntent = new Intent(GROUP_SELECTED_INTENT_ACTION);

        public ViewHolder(final View itemView) {
            super(itemView);

            // The item I receive has a cardview layout!!! see onCreateViewHolder()
            cv = (CardView)itemView.findViewById(R.id.groupList_cardview);
            groupName = (TextView) itemView.findViewById(R.id.group_name);
            isGroupFree = (TextView) itemView.findViewById(R.id.group_free);
            groupImage = (ImageView) itemView.findViewById(R.id.group_image);
            joinButton = (Button) itemView.findViewById(R.id.join_group);

            // Preferisco la leggibilità in questo caso, non è immediato impostare un clicklistener globale in recyclerview
            joinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("GROUP SELECTED:", groupName.getText().toString());

                    if(groupIntent == null) // first time
                        groupIntent = new Intent(GROUP_SELECTED_INTENT_ACTION).putExtra(GROUP_NAME, groupName.getText().toString());
                    else { // reuse the intent
                        groupIntent.removeExtra(GROUP_NAME);
                        groupIntent.putExtra(GROUP_NAME, groupName.getText());
                    }

                    LocalBroadcastManager.getInstance(itemView.getContext()).sendBroadcast(groupIntent);
                }
            });
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                 .inflate(R.layout.grouplist_cardview_layout, parent, false);

        // Creates the viewHolder, that stores references to the various views
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        holder.groupName.setText(mDataset.get(position).getName());
        holder.isGroupFree.setText(mDataset.get(position).isFree() ? "Free" : "Locked");
        holder.groupImage.setImageDrawable(mDataset.get(position).getGroupImage());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}
