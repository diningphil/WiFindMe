package it.sam.diningphil.samproject.BackgroundThreads;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import it.sam.diningphil.samproject.TaskCallbacks;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 *
 *  this is called a FRAGMENT RETAIN DESIGN PATTERN
 *
 * ON ATTACH IS USED FOR GETTING AN ACTIVITY REFERENCE AND USE THE CALLBACK MECHANISM
 */
public class ClientReceiver extends Fragment {

    // L'activity i cui metodi chiamerò passando l'istanza del task, cosicchè la possa inizializzare
    private TaskCallbacks mCallbacks;

    public Task mTask;

    public ClientReceiver(){
        super();
    }

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*
         * The Android framework
         * will pass us a reference to the newly created Activity after
         * each configuration change.
         */
         mCallbacks = (TaskCallbacks) activity; // activity implements TaskCallback
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes. onCreate and onDestroy are not called anymore.
        setRetainInstance(true);

        mTask = new Task();

         /* If you want to run multiple AsyncTasks in parallel, you can call
         * executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
         * instead of execute() on your task. By default, AsyncTasks run in serial, first come first serve.
         */
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public ConcurrentHashMap<String,Location> getLocations() {
        if(mTask != null)
            return mTask.locations;
        else
            return null;
    }

    /*-------------------------------------------------------- Inner class Task ------------------------------------------*/

    /**
     * Note that we need to check if the callbacks are null in each
     * method in case they are invoked after the Activity's and
     * Fragment's onDestroy() method have been called.
     */
    public class Task extends AsyncTask<Void, String, Void> {

        private static final String UPDATE_THE_MAP = "Update_Map";
        private static final String CONNECTION_ERROR = "Connection error with the server";
        private static final String CONNECTION_CLOSED = "Server has closed the connection!";
        private static final String UPDATE = "Update_message";
        private static final String CLOSING_GROUP = "Closing_message";
        private static final String CLOSE_P2P_GROUP = "Please_Close_p2p_group";
        private static final String NO_UPDATE = "NO_UPDATE";

        private Socket socket;

        private ConcurrentHashMap<String, Location> locations;

        public volatile boolean stop;

        /* Called by the TaskCallbacks object */
        public void initTask(Socket socket, ConcurrentHashMap<String, Location> sharedLocations){
            this.socket = socket;
            locations = sharedLocations;
            stop = false;
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute(this);
                /* My callback (activity) can now set Task istance state because I pass its reference using initTask() method */
            }
        }

        /**
         * Note that we do NOT call the callback object's methods
         * directly from the background thread, as this could result
         * in a race condition.
         *
         * ONLY THIS AND PUBLISH PROGRESS ARE DONE IN THE BACKGROUND THREAD
         */
        @Override
        protected Void doInBackground(Void... ignore) {

            stop = false;

            BufferedReader reader;

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(!stop){

                    String action = reader.readLine();

                    Log.d("CLIENT_RECEIVER:", "RECEIVED " + action);

                    if(action != null) {
                         switch (action) {

                             case NO_UPDATE:

                                 locations.clear();

                                 publishProgress(UPDATE_THE_MAP);
                                 break;

                             case UPDATE:
                                 String received = reader.readLine();

                                 if (received != null) {

                                     updateSharedTable(received);

                                     publishProgress(UPDATE_THE_MAP);

                                     //Log.d("CLIENT_RECEIVER:", "RICEVUTO OGGETTO DA GROUP OWNER!");
                                     //System.out.println(globalUpdate.toString());
                                 }
                                 break;

                             case CLOSING_GROUP:
                                 //Log.d("CLIENT RECEIVER", "CONNECTION CLOSED BY SERVER");
                                 stop = true;

                                 publishProgress(CONNECTION_CLOSED);
                                 break;

                             default:
                                 stop = true;

                                 publishProgress(CONNECTION_CLOSED);
                                 break;
                         }
                    }else{

                        /* Questa situazione si presenta quando il client invia messaggio di abbandono gruppo e la socket viene chiusa lato server
                        *  Il client sender non chiude le socket, aspetta che le chiuda il receiver */

                        //Log.d("CLIENT RECEIVER", "STREAM CLOSED BY SENDER");

                        stop = true;

                        if (socket != null && !socket.isClosed())
                            socket.close();
                    }
                }

            } catch (IOException e) {

                System.err.println("CLIENT RECEIVER ERROR: " + e.getMessage());

                //e.printStackTrace();

                if (socket != null && !socket.isClosed())
                    try { socket.close(); } catch (IOException e1) { e.getMessage(); }

                publishProgress(CONNECTION_ERROR);

            }finally {

                Log.d("CLIENT_RECEIVER:", "QUITS");

                if (socket != null && !socket.isClosed())
                    try { socket.close(); } catch (IOException e1) { e1.getMessage(); }

                publishProgress(CLOSE_P2P_GROUP); // Per chiamare una removeGroup, se lo facessi prima avrei problemi perchè socket viene chiusa prematuramente
            }

            return null; // Il parametro di ritorno dell'AsyncTask è Void
        }

        private void updateSharedTable(String received) {

            try{
                JSONObject globalUpdate = new JSONObject(received);

                Iterator<String> iter = globalUpdate.keys();

                while (iter.hasNext()) {
                    String key = iter.next();

                    JSONArray value = (JSONArray) globalUpdate.get(key);

                    double latitude = value.getDouble(0);
                    double longitude = value.getDouble(1);

                    Location loc = new Location("");
                    loc.setLatitude(latitude);
                    loc.setLongitude(longitude);

                    locations.put(key, loc);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onProgressUpdate(String... args) {
            if (mCallbacks != null && args.length > 0) { // Check if I have a message to show to the user
                mCallbacks.onProgressUpdate(args[0]);
            }
        }

        @Override
        protected void onCancelled() {
            stop = true;

            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(Void ignore) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute();
            }
        }
    }
}
