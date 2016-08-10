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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import it.sam.diningphil.samproject.DataStructures.NameAndSocket;
import it.sam.diningphil.samproject.R;
import it.sam.diningphil.samproject.TaskCallbacks;

public class GroupOwnerServer extends Fragment {

    private TaskCallbacks mCallbacks;
    public Task mTask;

    public GroupOwnerServer(){ super(); }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallbacks = (TaskCallbacks) activity; // activity implements TaskCallback

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTask = new Task();

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        mTask.execute(); //Server starts
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void quit(){
        mTask.end();
    }

    public ConcurrentHashMap<String,Location> getLocations() {
        if(mTask != null)
            return mTask.locations;
        else
            return null;
    }

    public class Task extends AsyncTask<Void, String, Void> {

        public static final int SERVER_PORT = 5467;
        private static final String CONNECTION_ERROR = "Connection error with the server";
        private static final String CLOSE_P2P_GROUP = "Please_Close_p2p_group";

        public ServerSocket serverSocket;

        public GroupOwnerUpdater groupOwnerUpdater;
        public GroupOwnerReceiver groupOwnerReceiver;

        public AtomicBoolean end;

        public String mName;

        public ConcurrentHashMap<String, Location> locations;


        public void initTask(ConcurrentHashMap<String, Location> sharedLocations, String myName) {
            end = new AtomicBoolean();

            if (myName == null)
                mName = "Unknown";
            else
                mName = myName;

            locations = sharedLocations;
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute(this);
            }
        }


        public void sendUpdateToUI(String reason){
            publishProgress(reason);
        }

        @Override
        protected Void doInBackground(Void... ignore) {

            end.set(false);

            try {
                serverSocket = new ServerSocket(SERVER_PORT);

                CopyOnWriteArrayList<NameAndSocket> clients = new CopyOnWriteArrayList<>(); // shared data structure

                groupOwnerUpdater = new GroupOwnerUpdater(locations, clients, this);

                groupOwnerReceiver = new GroupOwnerReceiver(locations, clients, this);

                // Start my async threads
                groupOwnerUpdater.start();

                groupOwnerReceiver.start();

                while (!end.get()) {

                    try {
                        Socket newSocket = serverSocket.accept();

                        groupOwnerUpdater.addClient(newSocket);

                    } catch (IOException e) {
                         e.getMessage();
                    }
                }

            }catch (IOException e){
                Log.d("Server socket error: ", e.getMessage());

                publishProgress(CONNECTION_ERROR);

            }finally {

                if(serverSocket != null && !serverSocket.isClosed())
                    try { serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
            }

            // Devo aspettare il receiver per poter pubblicare l'ultimo aggiornamento e far tornare l'utente alla schermata inziale
            try {
                groupOwnerReceiver.join();
            } catch (InterruptedException e) {
                e.getMessage();
            }

            Log.d("SERVER: ", "QUITS");
            publishProgress(CLOSE_P2P_GROUP);

            return null;
        }

        public void end(){
            end.set(true);

            if (groupOwnerUpdater != null && groupOwnerUpdater.isAlive())
                groupOwnerUpdater.end.set(true);

            if(serverSocket != null && !serverSocket.isClosed())
                try { serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        @Override
        protected void onProgressUpdate(String... args) {
            if (mCallbacks != null && args.length > 0) {
                mCallbacks.onProgressUpdate(args[0]);
            }
        }

        @Override
        protected void onCancelled() {
            end();

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

        public void endReceiver() {
            if (groupOwnerReceiver != null && groupOwnerReceiver.isAlive())
                groupOwnerReceiver.end.set(true);
        }

        public void sendJoinUpdateToUI(String name) {
            mTask.sendUpdateToUI(name + " " + getResources().getString(R.string.has_joined));
        }

        public void sendLeaveUpdateToUI(String name) {
            sendUpdateToUI(name + getResources().getString(R.string.has_leaved));
        }
    }
}
