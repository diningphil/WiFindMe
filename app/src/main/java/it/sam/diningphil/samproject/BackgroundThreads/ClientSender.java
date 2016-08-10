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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import it.sam.diningphil.samproject.DataStructures.NameAndLocation;
import it.sam.diningphil.samproject.R;
import it.sam.diningphil.samproject.TaskCallbacks;


/** -------- See ClientReceiver for a better explanation ------------ */

public class ClientSender extends Fragment {

    private TaskCallbacks mCallbacks;
    public Task mTask;

    public ClientSender(){
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        mTask = new Task();

        /* If you want to run multiple AsyncTasks in parallel, you can call
         * executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
         * instead of execute() on your task. By default, AsyncTasks run in serial, first come first serve.
         */
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public void stop(){
        mTask.stop = true;
    }

    public ConcurrentHashMap<String,Location> getLocations() {
        if(mTask != null)
            return mTask.locations;
        else
            return null;
    }

    /*-------------------------------------------------------- Inner class Task ------------------------------------------*/

    public class Task extends AsyncTask<Void, String, Void> {

        private static final int WAITING_TIME = 1500;
        private static final String UPDATE = "Update_message";
        private static final String LEAVING = "Leaving";
        private static final String NO_UPDATE = "NO_UPDATE";

        private ConcurrentHashMap<String, Location> locations;

        private Socket socket;
        private String mName;

        public volatile boolean stop;

        public void initTask(Socket socket, ConcurrentHashMap<String, Location> sharedLocations, String myName){
            this.socket = socket;
            mName = myName;
            locations = sharedLocations;
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute(this);
            }
        }

        @Override
        protected Void doInBackground(Void... ignore) {

            stop = false;

            BufferedWriter writer;

            try {

                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Server first of all waits for my name
                sendMyName(writer, mName);

                while (!stop) {

                    if(locations.containsKey(mName) && locations.get(mName) != null) { // Si può evitare la seconda cond ? controlla.

                        String message = buildMessage();

                        sendUpdate(writer, message);

                    }else{
                        // Il mio nome non è stato ancora inserito (la posizione potrebbe essere null? credo di no, risponde alla domanda sopra)

                        sendNoUpdate(writer);
                    }

                    sleep(WAITING_TIME);
                }

                /* L'eccezione in questo caso e' un comportamento atteso quando è il server a chiudere,
                 * e in ogni caso devo morire, il receiver ha ricevuto msg di chiusura.
                 */
                try {
                    sendLeavingMessage(writer);
                }catch (IOException e){ e.getMessage(); }

            }catch (IOException e) {
                // Vale il discorso fatto sopra, anche se potrebbe essere successa qualsiasi cosa
                e.getMessage();
            }

            return null;
        }

        private void sendNoUpdate(BufferedWriter writer) throws IOException{
            writer.write(NO_UPDATE);
            writer.newLine();
            writer.flush();
        }

        private void sendLeavingMessage(BufferedWriter writer) throws IOException{
            writer.write(LEAVING);
            writer.newLine();
            writer.flush();
        }

        private void sleep(int waitingTime) {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) { e.getMessage(); }
        }

        private void sendUpdate(BufferedWriter writer, String message) throws IOException{
            writer.write(UPDATE);
            writer.newLine();
            writer.write(message);
            writer.newLine();
            writer.flush();
        }

        private String buildMessage(){

            NameAndLocation nameAndLocation = new NameAndLocation(mName, locations.get(mName));

            // Contains latitude and longitude
            JSONArray array = nameAndLocation.getJsonArrayForLocation();

            JSONObject update = new JSONObject();
            try {
                update.put(mName, array);
            } catch (JSONException e) {
                return null;
            }

            return update.toString();
        }

        private void sendMyName(BufferedWriter writer, String mName) throws IOException{
            if (mName != null)
                writer.write(mName);
            else
                writer.write(getResources().getString(R.string.unknown));
            writer.newLine();
            writer.flush();

            Log.d("CLIENT_SENDER:", "SENT MY NAME");
        }

        @Override
        protected void onProgressUpdate(String... args) {
            if (mCallbacks != null && args.length > 0) {
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
