package it.sam.diningphil.samproject;

import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;

public class WifiStateFragment extends Fragment{

    WifiP2pManager wifiP2pManager;

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    WifiP2pManager.Channel channel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("CLIENT_FRAGMENT", "CREATED");

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    public void setWifiState(WifiP2pManager man, WifiP2pManager.Channel ch){
        wifiP2pManager = man;
        channel = ch;
    }
}
