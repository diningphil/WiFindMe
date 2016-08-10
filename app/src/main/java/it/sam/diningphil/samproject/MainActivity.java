package it.sam.diningphil.samproject;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.sam.diningphil.samproject.Adapters.SlidingMenuAdapter;
import it.sam.diningphil.samproject.BackgroundThreads.ClientReceiver;
import it.sam.diningphil.samproject.BackgroundThreads.ClientSender;
import it.sam.diningphil.samproject.BackgroundThreads.GroupOwnerServer;
import it.sam.diningphil.samproject.DataStructures.MyMenuItem;
import it.sam.diningphil.samproject.UiFragments.GroupRoomFragment;
import it.sam.diningphil.samproject.UiFragments.ImageDialogFragment;
import it.sam.diningphil.samproject.UiFragments.NavigationMenuFragment;
import it.sam.diningphil.samproject.UiFragments.RecyclerViewFragment;
import it.sam.diningphil.samproject.UiFragments.SettingsFragment;
import it.sam.diningphil.samproject.Utils.Utils;

public class MainActivity extends AppCompatActivity implements // subclass of FragmentActivity
        LocationListener,
        ImageSelectedInterface,
        OnClickListener,
        OnMapReadyCallback,
        TaskCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener,
        SwipeRefreshLayout.OnRefreshListener{

    // Intent action used when a group item is selected from list
    private static final String GROUP_SELECTED_INTENT_ACTION = "Saverio";
    private static final String TIMER_DIALOG_INTENT = "Timer_Dialog";
    private static final String UPDATE_THE_MAP = "Update_Map";
    private static final String CONNECTION_ERROR = "Connection error with the server";
    private static final String CONNECTION_CLOSED = "Server has closed the connection!";
    private static final String RECYCLER_VIEW_FRAGMENT = "recycler.view.fragment";
    private static final String GROUP_ROOM_FRAGMENT = "group.room.fragment";
    private static final String PREFERENCES_FRAGMENT = "preferences.fragment";
    private static final String MENU_OPENED = "menu_is_opened";
    private static final String WHICH_FRAGMENT_VISIBLE = "group_list_is_visible";
    private static final String GROUP_CONNECTION_INITIALIZED = "Group.Connection.Initialized";
    private static final String GROUP_NAME = "GROUP_NAME";
    private static final String CLIENT_TASK_RECEIVER = "client_task_receiver";
    private static final String CLIENT_TASK_SENDER = "client_task_sender";
    private static final String OWNER_TASK_FRAGMENT = "owner_task_fragment";
    private static final String DIALOG_SHOWING = "dialog";
    private static final java.lang.String TIMER_ID = "TIMER_ID";
    private static final int SERVER_PORT = 5467;
    private static final String TCP_FAILED = "Failed_TCP";
    private static final String CLOSE_P2P_GROUP = "Please_Close_p2p_group";
    private static final String WIFI_STATE_FRAGMENT = "Wifi_State";
    private static final String OPEN_PREFERENCES = "open_preferences";

    private static final int GROUPLIST = 0;
    private static final int MAPGROUP = 1;
    private static final int PREFERENCES = 2;
    private static final String IMAGE_DIALOG_FRAGMENT = "image.dialog.fragment";
    private static final int NOTIFICATION = 1;
    private static final String REFRESH_ID = "Refresh.id" ;

    // For toolbar
    private Toolbar toolbar;

    // For sliding menu
    private DrawerLayout mDrawerLayout;
    private ListFragment navMenuFragment;

    private SlidingMenuAdapter adapter;
    private ArrayList<MyMenuItem> menuList;
    private boolean menuIsOpened;

    // My UI Fragments
    private RecyclerViewFragment mRecyclerViewFragment;
    private GroupRoomFragment myGroupFragment;
    private SettingsFragment mSettingsFragment;

    private WifiP2pManager wifip2pManager;
    private Channel wifiDirectChannel;
    private WifiP2pManager.ActionListener creationGroupListener;
    private WifiP2pManager.ActionListener deleteGroupListener;

    private BroadcastReceiver localReceiver;
    private BroadcastReceiver broadcastReceiver;
    private boolean isGroupConnectionInitialized;
    private ProgressDialog dialog;

    private LocationManager locationManager;
    private String locationProvider;
    private Location mLocation;

    // Table used to get the state of all members of a group
    private ConcurrentHashMap<String, Location> sharedLocations;

    private String mName;
    private Socket mSocket;
    private GroupOwnerServer groupOwnerServer;
    private ClientReceiver clientReceiver;
    private ClientSender clientSender;

    @SuppressWarnings("all")
    private boolean groupOwner;
    private InetAddress mGroupOwnerAddress;
    private boolean isDialogShowing;

    @SuppressWarnings("all")
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiStateFragment wifiState;

    // Which fragment is visible?
    private int fragmentVisible;

    private static Runnable startClientRunnable;
    private Runnable openWifiRunnable;
    private Runnable closeWifiRunnable;
    private Runnable failCreationRunnable;

    // for dialog timer
    private int timerID;
    private Intent timerIntent;
    private Intent startActivityIntent;
    private SwipeRefreshLayout swipeLayout;
    private int refreshID;
    private Intent progressIntent;
    private boolean mustCloseGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        swipeLayout = (MySwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setEnabled(false);

        // Default values
        mustCloseGroup = false;
        refreshID = 0;
        timerID = 0;
        menuIsOpened = false;
        fragmentVisible = GROUPLIST;
        isGroupConnectionInitialized = false;
        dialog = new ProgressDialog(this);

        // For the very first time, set preference values (false will not reset my values, if set)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_xml, false);

        // I don't attach them to a View! These are retained fragments
        FragmentManager fm = getFragmentManager();

        groupOwnerServer = (GroupOwnerServer) fm.findFragmentByTag(OWNER_TASK_FRAGMENT);
        clientReceiver = (ClientReceiver) fm.findFragmentByTag(CLIENT_TASK_RECEIVER);
        clientSender = (ClientSender) fm.findFragmentByTag(CLIENT_TASK_SENDER);
        wifiState = (WifiStateFragment) fm.findFragmentByTag(WIFI_STATE_FRAGMENT);

        // restore locations
        if(groupOwnerServer != null){
            sharedLocations = groupOwnerServer.getLocations();
        }else{
            if(clientSender != null){
                sharedLocations = clientSender.getLocations();
            }
            else if(clientReceiver != null){
                sharedLocations = clientReceiver.getLocations();
            }
        }

        if(savedInstanceState!= null) {

            // Restore values!
            timerID = savedInstanceState.getInt(TIMER_ID);
            refreshID = savedInstanceState.getInt(REFRESH_ID);

            menuIsOpened = savedInstanceState.getBoolean(MENU_OPENED);

            fragmentVisible = savedInstanceState.getInt(WHICH_FRAGMENT_VISIBLE);

            isDialogShowing = savedInstanceState.getBoolean(DIALOG_SHOWING);

            isGroupConnectionInitialized = savedInstanceState.getBoolean(GROUP_CONNECTION_INITIALIZED);

            if (isDialogShowing) {
                DialogInterface.OnCancelListener listener = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        wifip2pManager.cancelConnect(wifiDirectChannel, null);
                    }
                };

                String joining = getResources().getString(R.string.joining);
                String please_wait = getResources().getString(R.string.please_wait);
                dialog = ProgressDialog.show(this, joining, please_wait, false, true, listener);
            }

            // Restore fragments
            if (fragmentVisible == GROUPLIST)
                mRecyclerViewFragment = (RecyclerViewFragment) getFragmentManager().getFragment(savedInstanceState, RECYCLER_VIEW_FRAGMENT);
            else if (fragmentVisible == MAPGROUP)
                myGroupFragment = (GroupRoomFragment) getFragmentManager().getFragment(savedInstanceState, GROUP_ROOM_FRAGMENT);
            else if (fragmentVisible == PREFERENCES)
                mSettingsFragment = (SettingsFragment) getFragmentManager().getFragment(savedInstanceState, PREFERENCES_FRAGMENT);
        }

        initToolbar();

        initDrawer();

        initSlidingMenu();

        initLocationManager();

        initWiFiDirect();

        initCurrentFragment();


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(TIMER_ID, timerID);
        outState.putInt(REFRESH_ID, refreshID);

        outState.putBoolean(MENU_OPENED, menuIsOpened);

        outState.putInt(WHICH_FRAGMENT_VISIBLE, fragmentVisible);

        outState.putBoolean(DIALOG_SHOWING, isDialogShowing);

        outState.putBoolean(GROUP_CONNECTION_INITIALIZED, isGroupConnectionInitialized);

        if (fragmentVisible == GROUPLIST && mRecyclerViewFragment != null && mRecyclerViewFragment.isAdded())
            getFragmentManager().putFragment(outState, RECYCLER_VIEW_FRAGMENT, mRecyclerViewFragment);
        else if (fragmentVisible == MAPGROUP && myGroupFragment != null && myGroupFragment.isAdded())
            getFragmentManager().putFragment(outState, GROUP_ROOM_FRAGMENT, myGroupFragment);
        else if (fragmentVisible == PREFERENCES && mSettingsFragment != null && mSettingsFragment.isAdded())
            getFragmentManager().putFragment(outState, PREFERENCES_FRAGMENT, mSettingsFragment);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() { // deregister listeners
        super.onPause();

        unregisterAllReceivers();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null)
            locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() { // register listeners
        super.onResume();

        registerAllReceivers();

        // Start a dialog to set wifi if not enabled
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!wifi.isWifiEnabled())
            Utils.buildWifiDialog(wifi, this);

        // Se ero in stop e non ho ricevuto l'intent di chiusura devo chiudere!
        if(mustCloseGroup){
            mustCloseGroup = false;

            Toast.makeText(this, getResources().getString(R.string.connection_has_been_closed), Toast.LENGTH_SHORT).show();

            handleCloseGroupIntent();
        }

        if (locationManager != null && locationProvider != null) {

            boolean gps_enabled = false;

            try {
                gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) { ex.getMessage(); }

            if (!gps_enabled && fragmentVisible == MAPGROUP) {
                Toast.makeText(this, getResources().getString(R.string.enable_gps), Toast.LENGTH_LONG).show();
            }

            if(fragmentVisible == MAPGROUP) {
                // TODO imposta i metri, lascia così per test dell'esame
                locationManager.requestLocationUpdates(locationProvider, 5000, 0, this);
            }
        }
    }

    // Inserisco gli item nella TOOLBAR (già inizializzata in onCreate() )
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu); // Inflates action items


        adjustMenuIcons(menu);

        return true;
    }

    // Aggiorno cliccabilità items
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return adjustMenuIcons(menu);
    }

    public boolean adjustMenuIcons(Menu menu){

        if(fragmentVisible == GROUPLIST) {
            menu.findItem(R.id.addGroup).setEnabled(true);
            menu.findItem(R.id.addGroup).setIcon(R.drawable.ic_group_add);

            menu.findItem(R.id.refreshList).setEnabled(true);
            menu.findItem(R.id.refreshList).setIcon(R.drawable.ic_cached);
        }else{
            menu.findItem(R.id.addGroup).setEnabled(false);
            menu.findItem(R.id.addGroup).setIcon(R.drawable.ic_group_add_disabled);

            menu.findItem(R.id.refreshList).setEnabled(false);
            menu.findItem(R.id.refreshList).setIcon(R.drawable.ic_cached_disabled);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId())
        {
            case (R.id.refreshList):
                swipeLayout.setRefreshing(true);

                refreshID++;

                ((MyApplication)getApplication()).getApplicationThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {

                        final int id = refreshID;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (id == refreshID)
                                    swipeLayout.setRefreshing(false);
                            }
                        });
                    }
                });

                mRecyclerViewFragment.discoveryPeers();
                mRecyclerViewFragment.discoveryServices();

                break;

            case (R.id.addGroup):
                if(Utils.locationStatusCheck(this)) {

                    openWiFiDirectGroup();
                }

                break;

            default: return true;
        }

        return true;

    }

    /* ------------ImageSelectedInterface------------------------------------------------------------------- */

    @Override
    public void updateSelectedImage(int resourceId) {

        ImageDialogFragment imageDialogFragment = (ImageDialogFragment) getFragmentManager().findFragmentByTag(IMAGE_DIALOG_FRAGMENT);

        if (imageDialogFragment != null && imageDialogFragment.isVisible()){
            imageDialogFragment.dismiss();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(getResources().getString(R.string.image_preference), String.valueOf(resourceId));

        editor.apply(); // asincrono

        Log.d("UPDATED:", "PROFILE IMAGE");

    }

/* ------------OnBackPressed------------------------------------------------------------------- */

    // addToBackStack didn't work, found similar problems on stackoverflow
    @Override
    public void onBackPressed() {

        if(fragmentVisible == PREFERENCES) {
            fragmentVisible = GROUPLIST;

            showGroupListFragment();
        }else if (fragmentVisible == MAPGROUP) {
            leaveTheGroupWithoutClosingP2P();
        }else if(fragmentVisible == GROUPLIST){

            if(menuIsOpened){
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }else {
                super.onBackPressed();
            }
        }

    }

    /* ------------OnRefreshListener------------------------------------------------------------------- */

    @Override
    public void onRefresh() {
        Toast.makeText(this, getResources().getString(R.string.refreshing), Toast.LENGTH_LONG).show();

        mRecyclerViewFragment.discoveryPeers();
        mRecyclerViewFragment.discoveryServices();
    }

    /* ------------OnSharedPreferenceListener------------------------------------------------------------------- */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.name_preference))) {
            mName = sharedPreferences.getString(key, getResources().getString(R.string.unknown));
            Log.d("NEW NAME SETTED:", mName);
        }
    }

    /* ------------OnPreferenceClickListener------------------------------------------------------------------- */

    @Override
    public boolean onPreferenceClick(Preference preference) {


        if (preference.getKey().equals(getResources().getString(R.string.image_preference))){

            showImagesDialogFragment();
            return true;
        }

        return false;
    }

    private void showImagesDialogFragment() {

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        // Create and show the dialog.
        ImageDialogFragment imageDialogFragment = new ImageDialogFragment();
        imageDialogFragment.show(ft, IMAGE_DIALOG_FRAGMENT);

        // Commit è fatto da show


    }

    /* ------------OnClickListener------------------------------------------------------------------- */

    @Override
    public void onClick(View v) // Intercept Navigation Button's click
    {

        if(fragmentVisible == PREFERENCES){
            fragmentVisible = GROUPLIST;
            showGroupListFragment();
        }
        else if(fragmentVisible == GROUPLIST) {
            if (!menuIsOpened) {
                //noinspection ResourceType
                mDrawerLayout.openDrawer(Gravity.START);
            } else {
                //noinspection ResourceType
                mDrawerLayout.closeDrawer(Gravity.START);
            }
        }
        else if(fragmentVisible == MAPGROUP){
            // I'm into a group
            leaveTheGroupWithoutClosingP2P();
        }
    }

    /* -------------TaskCallback--------------------------------------------------------------------*/
    // The four methods below are called by the GroupOwnerFragment or ClientFragment when new
    // progress updates or results are available. The MainActivity
    // should respond by updating its UI to indicate the change.

    @Override
    public void onPreExecute(AsyncTask<Void, String, Void> task){

        /* Determine which task i'm going to run, cast it and set methods, so doInBackground
         * won't fail
         */
        if(groupOwnerServer != null && groupOwnerServer.isAdded()){
            GroupOwnerServer.Task mTask = (GroupOwnerServer.Task) task;
            mTask.initTask(sharedLocations, mName);

        }else{

            if (clientReceiver != null && clientReceiver.isAdded())
                clientReceiver.mTask.initTask(mSocket, sharedLocations);

            if (clientSender != null && clientSender.isAdded())
                clientSender.mTask.initTask(mSocket, sharedLocations, mName);
        }
    }

    @Override
    public void onProgressUpdate(String reason) {

        // Altrimenti se la activity non è in stato running viene
        // sollevata una illegalStateException se provo a modificare la UI

        if(CLOSE_P2P_GROUP.equals(reason))
            mustCloseGroup = true;

        if(progressIntent == null)
            progressIntent = new Intent();

        progressIntent.setAction(reason);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(reason));


    }

    @Override
    public void onCancelled() {  }

    @Override
    public void onPostExecute() {  }


    /* -------------LocationListener--------------------------------------------------------------------*/

    @Override
    public void onLocationChanged(Location location) {

        mLocation = location;

        if (mName != null && sharedLocations != null && mLocation != null && mLocation.getAccuracy() <= 20){
            Log.d("NEW_VALID_LOCATION:", location.toString());
            sharedLocations.put(mName, mLocation);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }

    @Override
    public void onProviderEnabled(String s) {
        initLocationManager();
    }

    @Override
    public void onProviderDisabled(String s) {
        if(locationManager != null)
            locationManager.removeUpdates(this);
    }

    /* ------------OnMapReadyCallback------------------------------------------------------------------- */

    @Override
    public void onMapReady(GoogleMap map) {
        // From the doc: map is a non-null instance

        if (locationManager != null){

            map.clear();

            if(mLocation == null) {
                mLocation = locationManager.getLastKnownLocation(locationProvider);

                if (mLocation != null) {
                    // Animate map
                    LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }

            map.setMyLocationEnabled(true);

            if (sharedLocations == null) {
                sharedLocations = new ConcurrentHashMap<>();

                if(mName != null && mLocation != null)
                    sharedLocations.put(mName, mLocation);
            }

            for( Map.Entry<String,Location> el : sharedLocations.entrySet() ){

                if (!el.getKey().equals(mName)) {

                    LatLng friendPos = new LatLng(el.getValue().getLatitude(), el.getValue().getLongitude());

                    Marker m = myGroupFragment.getMap().addMarker(new MarkerOptions()
                                    .position(friendPos)
                                    .title(el.getKey())
                    );
                    m.setVisible(true);
                }
            }

        }
    }


    /* --------------------Utility Methods-------------------------------------------------------------------------- */

    private void initToolbar()
    {
        toolbar = (Toolbar) findViewById(R.id.myToolbar);

        if (toolbar != null)
        {
            setSupportActionBar(toolbar); // set toolbar as the new Action Bar

            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(this); // Avoid to do "new"

            if(getSupportActionBar() != null)
                getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Deletes the activity Label ( don't put it after setTitle! )
            toolbar.setTitle(R.string.toolbarTitle);
        }
    }

    private void initSlidingMenu()
    {
        FragmentManager fm = getFragmentManager();

        if (navMenuFragment == null)
            // E' un ListFragment
            navMenuFragment = (NavigationMenuFragment) fm.findFragmentById(R.id.navigation_menu_fragment);

        if(adapter == null || menuList == null)
        {
            menuList = new ArrayList<>();
            menuList.add(new MyMenuItem(R.drawable.ic_info_black, getResources().getString(R.string.info)));
            menuList.add(new MyMenuItem(R.drawable.ic_info_black, getResources().getString(R.string.preferences)));

            adapter = new SlidingMenuAdapter(this,
                    R.layout.fragment_navigation_drawer,
                    menuList);

            navMenuFragment.setListAdapter(adapter);

        }

    }

    private void initDrawer(){

        if(toolbar == null) initToolbar();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerLayout.setDrawerListener(new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);

                menuIsOpened = false;
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                menuIsOpened = true;
            }
        });
    }

    private void initChannel(){
        wifiDirectChannel = wifip2pManager.initialize(this, getMainLooper(),
                new WifiP2pManager.ChannelListener() {
                    @Override
                    public void onChannelDisconnected() {
                        Log.d("CHANNEL", "DISCONNECTED");
                        initWiFiDirect();
                    }
                });

        if(mRecyclerViewFragment != null && wifip2pManager != null)
            mRecyclerViewFragment.setWiFiState(wifip2pManager, wifiDirectChannel);
    }

    private void initWiFiDirect() {


        if(wifiState != null){

            // Recover the state

            wifip2pManager = wifiState.getWifiP2pManager();

            wifiDirectChannel = wifiState.getChannel();

        }else {
            wifip2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

            initChannel();

            wifiState = new WifiStateFragment();

            wifiState.setWifiState(wifip2pManager, wifiDirectChannel);
        }

        initDiscoverService();

        creationGroupListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.d("GROUP CREATION:", "SUCCESS");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("GROUP CREATION FAIL:", reason + "");
                String cannot_create_group = getResources().getString(R.string.cannot_create_group);
                Toast.makeText(getApplicationContext(), cannot_create_group, Toast.LENGTH_SHORT).show();


                if(failCreationRunnable == null){
                    failCreationRunnable = new Runnable() {
                        @Override
                        public void run() {
                            closeWiFiDirectGroup();
                        }
                    };
                }
                // C'è da aggiornare la UI!
                runOnUiThread(failCreationRunnable);
            }
        };

        deleteGroupListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.d("GROUP REMOVAL:", "SUCCESS");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("GROUP REMOVAL", "FAIL: " + reason);
            }
        };

    }


    private void buildGroupAccessDialog(final String groupName) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        String join_group = getResources().getString(R.string.join_group);
        alertDialogBuilder.setTitle(join_group);

        String want_join = getResources().getString(R.string.want_join);
        // set dialog message
        alertDialogBuilder
                .setMessage(want_join + " " +  groupName + "  ?")
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //join group
                        joinGroup(groupName);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).create()
                .show();

    }

    private void initCurrentFragment(){

        if (fragmentVisible == GROUPLIST) {
            showGroupListFragment(); // The group list
        }
        else if(fragmentVisible == MAPGROUP)
            showMyGroupFragment(); // The group I'm in

        else if(fragmentVisible == PREFERENCES)
            showPreferenceFragment();

    }

    private void initLocationManager() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // ho già chiesto all'utente l'accesso a internet nel manifest
        criteria.setCostAllowed(false);

        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);

        locationProvider = locationManager.getBestProvider(criteria, true);

        mLocation = locationManager.getLastKnownLocation(locationProvider);

        if(mLocation != null)
            Log.d("LAST KNOWN LOC: " , mLocation.toString());
        else
            Log.d("LAST KNOWN LOC: ", "NULL");

        System.out.println("Available providers are: ");
        for (String el : locationManager.getAllProviders()) {
            System.out.println(el);
        }

    }

    private void joinGroup(final String groupName) {

        DialogInterface.OnCancelListener listener = new DialogInterface.OnCancelListener(){
            @Override
            public void onCancel(DialogInterface dialog){
                wifip2pManager.cancelConnect(wifiDirectChannel, null);
                isDialogShowing = false;
            }
        };

        String joining = getResources().getString(R.string.joining);
        String please_wait = getResources().getString(R.string.please_wait);

        dialog = ProgressDialog.show(this, joining, please_wait, false, true, listener);
        isDialogShowing = true;
        startTimerService();


        // BACONE: groupName deve cambiare!! devo ricreare il runnable o c'è un pattern possibile per risparmiare memoria?

        Runnable joinGroupRunnable = new Runnable() {
            @Override
            public void run() {
                //Get the device
                Log.d("RECUPERO DEV DA NOME:", groupName);

                WifiP2pDevice dev = mRecyclerViewFragment.getDeviceMap().get(groupName);

                WifiP2pConfig config = new WifiP2pConfig();

                if (dev != null) {

                    config.deviceAddress = dev.deviceAddress;
                    config.groupOwnerIntent = 0;

                    Log.d("CONNECTING TO GROUP: ", dev.deviceName);

                    wifip2pManager.connect(wifiDirectChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Log.d("JOIN GROUP CONNECT", "SUCCESS");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d("JOIN GROUP CONNECT", "FAILURE " + reason);

                            // Togli il dialog di progress
                            dialog.dismiss();
                            isDialogShowing = false;
                            stopTimerService();

                            String problems = getResources().getString(R.string.connection_problems);
                            Toast.makeText(getApplicationContext(), problems, Toast.LENGTH_SHORT).show();

                            // refresh peer list
                            if (mRecyclerViewFragment != null) {
                                //Log.d("MAIN_ACTIVITY:", "RESTARTING PEERS LOADER");
                                mRecyclerViewFragment.discoveryPeers();
                                mRecyclerViewFragment.discoveryServices();
                            }
                        }
                    });
                }
            }
        };

        ((MyApplication) getApplication()).getApplicationThreadPool().execute(joinGroupRunnable);
    }


    private void registerAllReceivers() {

        // preference receiver
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        final IntentFilter inF = new IntentFilter();
        inF.addAction(GROUP_SELECTED_INTENT_ACTION);
        inF.addAction(UPDATE_THE_MAP);
        inF.addAction(TIMER_DIALOG_INTENT);
        inF.addAction(TCP_FAILED);
        inF.addAction(CLOSE_P2P_GROUP);
        inF.addAction(CONNECTION_CLOSED);
        inF.addAction(CONNECTION_ERROR);


        IntentFilter inF2 = new IntentFilter();
        inF2.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        inF2.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        inF2.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        inF2.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        inF2.addAction(OPEN_PREFERENCES);

        if (localReceiver == null) {

            localReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    switch (intent.getAction()) {

                        case CLOSE_P2P_GROUP:

                            handleCloseGroupIntent();

                            break;

                        case CONNECTION_ERROR:
                        case CONNECTION_CLOSED:

                            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);

                            stopThreads();

                            // Threads are already closing
                            isGroupConnectionInitialized = false;
                            showGroupListFragment();

                            locationManager.removeUpdates(MainActivity.this);

                            Toast.makeText(getApplicationContext() , intent.getAction(), Toast.LENGTH_SHORT).show();
                            break;

                        case TCP_FAILED:


                            String no_tcp = getResources().getString(R.string.no_tcp);
                            Toast.makeText(getApplicationContext(), no_tcp, Toast.LENGTH_LONG).show();

                            leaveTheGroupWithoutClosingP2P();

                            break;

                        case TIMER_DIALOG_INTENT:

                            int intentID = intent.getIntExtra(TIMER_ID, -1);

                            //Log.d("TIMERID and INTENTID:",  timerID + " " + intentID);

                            if(dialog != null && dialog.isShowing() && (timerID == intentID) ) {

                                String refused = getResources().getString(R.string.refused_connection);
                                Toast.makeText(getApplicationContext(), refused, Toast.LENGTH_LONG).show();
                                dialog.cancel();
                                dialog.dismiss();
                                isDialogShowing = false;
                            }
                            break;

                        case UPDATE_THE_MAP:

                            updateMap();

                            break;

                        case GROUP_SELECTED_INTENT_ACTION:

                            final String groupName = intent.getStringExtra(GROUP_NAME);

                            if (Utils.hasNetworkConnection(MainActivity.this)) {

                                if (Utils.locationStatusCheck(MainActivity.this))
                                    buildGroupAccessDialog(groupName);
                                    // Will cause an WIFI_P2P_CONNECTION_CHANGED_ACTION intent
                                    // to be sended
                                else {
                                    String enable_gps = getResources().getString(R.string.enable_gps);
                                    Toast.makeText(MainActivity.this, enable_gps, Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                String be_connected = getResources().getString(R.string.be_connected);
                                Toast.makeText(MainActivity.this, be_connected, Toast.LENGTH_SHORT).show();
                            }
                            break;

                        default:
                            break;
                    }
                }
            };
        }

        if(broadcastReceiver == null){

            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()){

                        case OPEN_PREFERENCES:

                            //noinspection ResourceType
                            mDrawerLayout.closeDrawer(Gravity.START);

                            if(fragmentVisible == MAPGROUP){
                                leaveTheGroupWithoutClosingP2P();
                            }

                            showPreferenceFragment();
                            break;

                        case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

                            String unknown = getResources().getString(R.string.unknown);

                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            String userName = sharedPref.getString(getResources().getString(R.string.name_preference), unknown);

                            if(!userName.equals(unknown))
                                mName = userName;
                            else {
                                mName = device.deviceName;
                            }

                        case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED);

                            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                                if (mRecyclerViewFragment != null) {
                                    mRecyclerViewFragment.discoveryPeers();
                                    mRecyclerViewFragment.discoveryServices();
                                }

                            }

                            break;

                        case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                            //Log.d("REQUEST PEERS:", "PEERS ARE CHANGED");
                            break;

                        case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:

                            //Log.d("MAIN_ACTIVITY", "P2P_" + "CONNECTION_CHANGED");

                            NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                            if(netInfo.isConnected()){

                                wifip2pManager.requestConnectionInfo(wifiDirectChannel, new WifiP2pManager.ConnectionInfoListener() {
                                    @Override
                                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                        if(info.groupFormed && !isGroupConnectionInitialized){

                                            if(info.isGroupOwner) {
                                                stopTimerService();

                                                // For onPreExecute
                                                groupOwner = true;

                                                sharedLocations = new ConcurrentHashMap<>();

                                                startServer();

                                                isGroupConnectionInitialized = true;

                                                showMyGroupFragment();

                                                dialog.dismiss();
                                                isDialogShowing = false;

                                                addWifiStateFragment();

                                                // TODO imposta i metri, lascia così per test dell'esame
                                                locationManager.requestLocationUpdates(locationProvider, 5000, 0, MainActivity.this);

                                            }else{
                                                //Client
                                                stopTimerService();

                                                sharedLocations = new ConcurrentHashMap<>();

                                                mGroupOwnerAddress = info.groupOwnerAddress;

                                                Log.d("CLIENT", "START CLIENT THREADS");
                                                startClient();

                                                isGroupConnectionInitialized = true;

                                                showMyGroupFragment();

                                                dialog.dismiss();
                                                isDialogShowing = false;

                                                addWifiStateFragment();

                                                // TODO imposta i metri, lascia così per test dell'esame
                                                locationManager.requestLocationUpdates(locationProvider, 5000, 0, MainActivity.this);

                                            }
                                        }
                                    }
                                });
                            /*}else {
                                Log.d("WIFI_P2P:", "NO CONNECTION");
                             */
                            }

                            break;
                    }
                }
            };
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, inF);
        registerReceiver(broadcastReceiver, inF2);


        addServiceRequest();
    }

    private void handleCloseGroupIntent() {
        mustCloseGroup = false;

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);

        closeWiFiDirectGroup();

        FragmentManager fm = getFragmentManager();

        FragmentTransaction ft = fm.beginTransaction();

        if(groupOwnerServer != null && groupOwnerServer.isAdded()) {
            ft.remove(groupOwnerServer);
        }

        if(clientSender != null && clientSender.isAdded()){
            ft.remove(clientSender);
        }

        if(clientReceiver != null && clientReceiver.isAdded()){
            ft.remove(clientReceiver);
        }

        ft.commit();

        // Received null for some reason
        if(fragmentVisible == MAPGROUP){
            groupOwner = false;

            isGroupConnectionInitialized = false;

            showGroupListFragment();
        }

        locationManager.removeUpdates(MainActivity.this);
    }

    private void addWifiStateFragment() {

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if(wifiState != null && !wifiState.isAdded())
            ft.add(wifiState, WIFI_STATE_FRAGMENT);

        ft.commit();
    }
    
    /*-------------------------------------------- BACKGROUND THREADS METHODS------------------------------------------------------------------------*/

    public void startServer(){

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (groupOwnerServer != null && groupOwnerServer.isAdded()) {
            ft.remove(groupOwnerServer);
        }

        if(groupOwnerServer == null) {
            groupOwnerServer = new GroupOwnerServer();
        }

        // This fragment will survive to activity destroy
        ft.add(groupOwnerServer, OWNER_TASK_FRAGMENT);

        ft.commit();

        sendNotify();
    }


    public void startClient(){

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (clientReceiver != null && clientReceiver.isAdded()) {
            ft.remove(clientReceiver);
        }

        if (clientSender != null && clientSender.isAdded()) {
            ft.remove(clientSender);
        }

        ft.commit();



        if (startClientRunnable == null){
            // Devo evitare l'attesa di apertura connessione TCP con il server
            startClientRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d("CLIENT", "TRIES TO CONNECT VIA SOCKET TCP on " + mGroupOwnerAddress);

                        mSocket = new Socket(mGroupOwnerAddress, SERVER_PORT);

                        Log.d("CLIENT", "CONNECTED VIA SOCKET TCP");

                        if (clientSender == null) {
                            clientSender = new ClientSender();
                        }


                        if (clientReceiver == null) {
                            clientReceiver = new ClientReceiver();
                        }

                        // This fragments will survive to activity destroy
                        if (!clientReceiver.isAdded() && !clientSender.isAdded()) {

                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.add(clientReceiver, CLIENT_TASK_RECEIVER); // initialize the task and execute it ( preExecute() will set the fields )
                            ft.add(clientSender, CLIENT_TASK_SENDER); // initialize the task and execute it ( preExecute() will set the fields )
                            ft.commit();
                        }

                    } catch (IOException e) {

                        e.printStackTrace();

                        leaveTheGroupWithoutClosingP2P();

                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent().setAction(TCP_FAILED));
                    }
                }
            };
        }

        ((MyApplication) getApplication()).getApplicationThreadPool().execute(startClientRunnable);

        sendNotify();
    }

    private void sendNotify(){

        if(startActivityIntent == null) {
            startActivityIntent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        // Per ora non ho bisogno di un id privato
        PendingIntent pi = PendingIntent.getActivity(this, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Imposta notifica
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_start_threads)
                .setLargeIcon(((BitmapDrawable) getResources().getDrawable(R.mipmap.my_launcher_no_background)).getBitmap())
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(getResources().getString(R.string.notification_content))
                .setContentIntent(pi);

        Notification n = builder.build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        nm.notify(NOTIFICATION, n);
    }

    private void leaveTheGroupWithoutClosingP2P() {
        stopThreads();

        groupOwner = false;

        isGroupConnectionInitialized = false;

        showGroupListFragment();
    }

    public void stopClient(){ // Interrupt doesn't work properly

        if(clientSender != null){
            clientSender.stop();
        }
    }

    public void stopServer(){ // Interrupt doesn't work properly

        if(groupOwnerServer != null)
            groupOwnerServer.quit();
    }

    private void stopThreads() {

        if (groupOwnerServer != null) {
            stopServer();
        }

        if(clientReceiver != null || clientSender != null)
            stopClient();
    }


    /*---------------------------------------------TIMER INTENT SERVICE--------------------------------------------------------------------------------*/


    private void startTimerService() {
        
        stopTimerService();

        timerID++;

        // Explicit intent
        timerIntent = new Intent(this, TimerIntentService.class).putExtra(TIMER_ID, timerID);

        // Started Service
        startService(timerIntent);
    }

    private void stopTimerService(){
        if(timerIntent != null) {
            stopService(timerIntent);
        }
    }



    /*---------------------------------------------END TIMER INTENT SERVICE--------------------------------------------------------------------------------*/




    /*---------------------------------------------LOCAL SERVICE METHODS--------------------------------------------------------------------------------*/

    public void initDiscoverService(){

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {

            @Override
            // Callback includes:
            // fullDomain: full domain name: e.g "printer._ipp._tcp.local."
            // record: TXT record dta as a map of key/value pairs.
            // device: The device running the advertised service.

            public void onDnsSdTxtRecordAvailable( String fullDomain, Map record, WifiP2pDevice device) {

                Log.d("DISCOVER_SERVICE", "DnsSdTxtRecord available -" + record.toString());

                swipeLayout.setRefreshing(false);

                // Controllo che sia la mia app
                if(record.containsKey("application") ) {
                    String application = (String) record.get("application");

                    if (application.equals("SAM")){

                        // GET OTHER INFO

                        String ownerName = (String) record.get("ownerName");
                        Drawable ownerIcon;
                        String isFree = "true";

                        if(record.containsKey("icon")) {
                            int id = Integer.parseInt((String) record.get("icon"));
                            ownerIcon = getResources().getDrawable(id);

                            if(ownerIcon == null)
                                ownerIcon = getResources().getDrawable(R.drawable.light_blue_nine);
                        }
                        else
                            // Retrocompatibilità versioni precedenti 1.2 (3 download alpha LOL), immagine standard
                            ownerIcon = getResources().getDrawable(R.drawable.ic_group_icon);

                        if(record.containsKey("free"))
                            isFree = (String) record.get("free");

                        ConcurrentHashMap<String, WifiP2pDevice> map = mRecyclerViewFragment.getDeviceMap();

                        // Only group owners register a local service, and then dismiss it at the end
                        map.put(ownerName, device);

                        mRecyclerViewFragment.addElement(ownerName, ownerIcon, isFree); // TODO info on locked or free ecc
                    }


                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice dev) {
                Log.d("DISCOVERY SERVICE", "TROVATO SERVICE");
            }
        };

        wifip2pManager.setDnsSdResponseListeners(wifiDirectChannel, servListener, txtListener);

    }


    private void addServiceRequest(){
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        wifip2pManager.addServiceRequest(wifiDirectChannel,
                serviceRequest, null);

                /*new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    }
                });
                */
    }

    private void removeServiceRequest(){
        wifip2pManager.clearServiceRequests(wifiDirectChannel, null);
    }

    @SuppressWarnings("all")
    public void startLocalServiceRegistration(){

        wifip2pManager.clearLocalServices(wifiDirectChannel, null);

        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("application", "SAM");
        record.put("ownerName", mName);
        record.put("groupOwner", "true");
        record.put("free", "true");

        // Get user selected image
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String userImageId = sharedPref.getString(getResources().getString(R.string.image_preference), Integer.toString(R.drawable.ic_group_icon));

        record.put("icon", userImageId);

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_samProject", "_ipp._tcp", record);

        wifip2pManager.addLocalService(wifiDirectChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.d("LOCAL SERVICE", "STARTED");

            }

            @Override
            public void onFailure(int reason) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d("LOCAL SERVICE", "START FAILED " + reason);
            }
        });
    }

    private void stopLocalServices(){
        wifip2pManager.clearLocalServices(wifiDirectChannel, null);
    }



    /*---------------------------------------------END LOCAL SERVICE--------------------------------------------------------------------------------*/



    private void updateMap() {

        if(fragmentVisible == MAPGROUP){
            myGroupFragment.getMapAsync(this);
        }
    }

    private void unregisterAllReceivers() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.unregisterOnSharedPreferenceChangeListener(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        unregisterReceiver(broadcastReceiver);

        removeServiceRequest();
    }

    private void openWiFiDirectGroup(){

        WifiManager wM = (WifiManager) getSystemService(WIFI_SERVICE);

        if(!wM.isWifiEnabled())
            Toast.makeText(this, getResources().getString(R.string.please_wifi), Toast.LENGTH_LONG).show();
        else {
            if (openWifiRunnable == null) {
                openWifiRunnable = new Runnable() {
                    @Override
                    public void run() {

                        startLocalServiceRegistration();

                        wifip2pManager.createGroup(wifiDirectChannel, creationGroupListener);
                        // UI updates are made by creationGroupListener in onSuccess
                    }
                };
            }
            ((MyApplication) getApplication()).getApplicationThreadPool().execute(openWifiRunnable);
        }

    }

    private void closeWiFiDirectGroup(){

        if (closeWifiRunnable == null){
            closeWifiRunnable = new Runnable() {
                @Override
                public void run() {

                    //Log.d("REMOVING:", "P2P GROUP");

                    stopLocalServices();

                    wifip2pManager.removeGroup(wifiDirectChannel, deleteGroupListener);
                }
            };
        }
        ((MyApplication) getApplication()).getApplicationThreadPool().execute(closeWifiRunnable);
        
        removeWifiStateFragment();
    }

    private void removeWifiStateFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if(wifiState != null && wifiState.isAdded() )
            ft.remove(wifiState);

        ft.commit();

        wifiState = null;
    }

    private void showGroupListFragment(){

        swipeLayout.setRefreshing(false);

        //Log.d("MAIN_ACTIVITY", "SHOW_GROUP_LIST");

        if (mRecyclerViewFragment == null){
            mRecyclerViewFragment = new RecyclerViewFragment();
            mRecyclerViewFragment.setWiFiState(wifip2pManager, wifiDirectChannel);
        }else
            mRecyclerViewFragment.setWiFiState(wifip2pManager, wifiDirectChannel);

        // Add mRecyclerViewFragment to Activity
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        if(!mRecyclerViewFragment.isAdded())
            ft.replace(R.id.fragments_container, mRecyclerViewFragment);

        ft.commit();

        fragmentVisible = GROUPLIST;

        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setTitle(R.string.groups);

        invalidateOptionsMenu();

        // Start a discovery
        if(mRecyclerViewFragment.getGroupList().size() == 0) {
            mRecyclerViewFragment.discoveryPeers();
            mRecyclerViewFragment.discoveryServices();
        }

    }

    private void showMyGroupFragment(){

        swipeLayout.setRefreshing(false);

        //Log.d("MAIN_ACTIVITY", "SHOW_GROUP_ROOM");

        if (myGroupFragment == null)
            myGroupFragment = new GroupRoomFragment();

        // Add myGroupFragment to Activity
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);

        if(!myGroupFragment.isAdded())
            ft.replace(R.id.fragments_container, myGroupFragment);

        ft.commit();

        fragmentVisible = MAPGROUP;

        invalidateOptionsMenu();

        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

        toolbar.setTitle(R.string.myGroup);
    }

    private void showPreferenceFragment() {

        swipeLayout.setRefreshing(false);

        if(mSettingsFragment == null)
            mSettingsFragment = new SettingsFragment();

        // Add myGroupFragment to Activity
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.fragments_container, mSettingsFragment)
                .commit();


        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

        toolbar.setTitle(R.string.preferences);

        fragmentVisible = PREFERENCES;

        invalidateOptionsMenu();
    }
}