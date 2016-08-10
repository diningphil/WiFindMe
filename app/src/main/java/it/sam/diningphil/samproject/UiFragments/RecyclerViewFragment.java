package it.sam.diningphil.samproject.UiFragments;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import it.sam.diningphil.samproject.DataStructures.Group;
import it.sam.diningphil.samproject.R;
import it.sam.diningphil.samproject.Adapters.RecyclerViewAdapter;

public class RecyclerViewFragment extends Fragment{

    private RecyclerView mRecyclerView;

    @SuppressWarnings("all") // can be converted to local variable
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerViewAdapter mRecyclerViewAdapter;

    private WifiP2pManager wifip2pManager;
    private Channel wifiDirectChannel;

    private ArrayList<Group> groups;

    private ConcurrentHashMap<String,WifiP2pDevice> deviceMap;

    private Runnable discoveryServiceRunnable = new Runnable() {
        @Override
        public void run() {
            wifip2pManager.discoverServices(wifiDirectChannel, servicesListener);
        }
    };

    private static WifiP2pManager.ActionListener servicesListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            // Success!
            Log.d("DISCOVERY SERVICES", "SUCCESS");
        }

        @Override
        public void onFailure(int code) {
            // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            Log.d("DISCOVERY SERVICES", "FAIL " + code);
        }
    };

    private static WifiP2pManager.ActionListener peersListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            //Log.d("DISCOVER_PEERS:", "SUCCESS");
        }

        @Override
        public void onFailure(int reason) {
            String err = "Wifi Direct failed: ";

            switch (reason) {
                case WifiP2pManager.BUSY:
                    err += "BUSY";
                    break;
                case WifiP2pManager.ERROR:
                    err += "ERROR";
                    break;
                case WifiP2pManager.P2P_UNSUPPORTED:
                    err += "UNSUPPORTED";
                    break;
                default:
                    err += "UNKNOWN";
                    break;
            }

            Log.d("DISCOVER_PEERS:", err);
        }
    };
    public RecyclerViewFragment() {
        super();
        groups = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Salvo il fragment nel bundle dell'activity, quando viene riaggiunto potrebbe avere ancora i suoi riferimenti
        // Perciò controllo prima di re-istanziare qualcosa
        if (groups == null)
            groups = new ArrayList<>();

        if (mRecyclerView == null)
            mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);

        // using a linear layout manager as doc suggest
        if (mLayoutManager == null) {
            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
        }

        if(mRecyclerViewAdapter == null)
            mRecyclerViewAdapter = new RecyclerViewAdapter(groups);

        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        return mRecyclerView;
    }


    /* il ciclo di vita dell'activity è diverso da quello degli oggetti
     * ma quando ruoto lo schermo wifip2pManager e il channel venivano deallocati.
     * ho bisogno dunque di reimpostarli nel fragment
     */
    public void setWiFiState(WifiP2pManager wp2pM, Channel channel){
        wifip2pManager = wp2pM;
        wifiDirectChannel = channel;
    }

    public void discoveryServices(){

        if(deviceMap == null)
            deviceMap = new ConcurrentHashMap<>();

        deviceMap.clear();

        groups.clear();

        // Mi è stata sollevata una null pointer.. lo sto usando prima di aver invocato onCreateView?
        if(mRecyclerViewAdapter == null && mRecyclerView != null){
            mRecyclerViewAdapter = new RecyclerViewAdapter(groups);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }

        if(mRecyclerViewAdapter != null)
            mRecyclerViewAdapter.notifyDataSetChanged();

        new Thread(discoveryServiceRunnable).start();
    }

    public void addElement(String name, Drawable ownerIcon, String isFreeS) {

        boolean isFree = false;

        if(isFreeS.equals("true"))
            isFree = true;


        Group g = new Group(name, isFree, ownerIcon);
        groups.add(g);

        if (mRecyclerViewAdapter == null) {
            mRecyclerViewAdapter = new RecyclerViewAdapter(groups);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        } else
            mRecyclerViewAdapter.notifyDataSetChanged();

    }

    public ConcurrentHashMap<String,WifiP2pDevice> getDeviceMap(){
        if(deviceMap == null)
            deviceMap = new ConcurrentHashMap<>();

        return deviceMap;
    }

    public void discoveryPeers() {
        wifip2pManager.discoverPeers(wifiDirectChannel, peersListener);
    }

    public ArrayList<Group> getGroupList() {
        return groups;
    }
}


