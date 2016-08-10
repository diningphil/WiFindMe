package it.sam.diningphil.samproject.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import it.sam.diningphil.samproject.DataStructures.MyMenuItem;
import it.sam.diningphil.samproject.R;


public class SlidingMenuAdapter extends ArrayAdapter<MyMenuItem>{

    private static final String OPEN_PREFERENCES = "open_preferences";

    private final List<MyMenuItem> mDataset;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView t = (TextView) v.findViewById(R.id.item_name);

            Resources res = getContext().getResources();

            // Need to take that from resources
            String info = res.getString(R.string.info);
            String pref = res.getString(R.string.preferences);
            
            if(info.equals(t.getText())) {
                buildInfoDialog();

            }else if(pref.equals(t.getText())){
                openPreferenceFragment();
            }
        }
    };

    public SlidingMenuAdapter(Context context, int resource, List<MyMenuItem> objects) {
        super(context, resource, objects);
        mDataset = objects;
    }

    private void openPreferenceFragment() {
        getContext().sendBroadcast(new Intent().setAction(OPEN_PREFERENCES));
    }

    private void buildInfoDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        String version = "Unknown";

        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            version = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        builder.setTitle("Info WiFindMe");

        builder.setMessage(getContext().getResources().getString(R.string.version) + " " + version + getContext().getResources().getString(R.string.appInfo));

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.menu_item_layout, parent, false);

        TextView t = (TextView) v.findViewById(R.id.item_name);

        t.setText(mDataset.get(position).getName());

        ImageView iV = (ImageView) v.findViewById(R.id.item_image);

        iV.setImageResource(mDataset.get(position).getItemImageId());

        v.setClickable(true);

        v.setOnClickListener(clickListener);

        return v;
    }
}
