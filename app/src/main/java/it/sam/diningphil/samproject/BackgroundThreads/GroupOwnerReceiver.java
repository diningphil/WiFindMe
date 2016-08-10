package it.sam.diningphil.samproject.BackgroundThreads;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import it.sam.diningphil.samproject.DataStructures.NameAndSocket;

public class GroupOwnerReceiver extends Thread{

    private static final int WAITING_TIME = 1500;
    private static final String UPDATE = "Update_message";
    private static final String LEAVING = "Leaving";

    private static final String UPDATE_THE_MAP = "Update_Map";
    private static final String NO_UPDATE = "NO_UPDATE";
    private final GroupOwnerServer.Task mainTask;

    private CopyOnWriteArrayList<NameAndSocket> clients; // writes are rare
    private ConcurrentHashMap<String, Location> locations;
    public AtomicBoolean end;

    public GroupOwnerReceiver(ConcurrentHashMap<String, Location> sharedLocations, CopyOnWriteArrayList<NameAndSocket> cls, GroupOwnerServer.Task mainTask){
        locations = sharedLocations;
        end = new AtomicBoolean();
        clients = cls;
        this.mainTask = mainTask;
    }

    @Override
    public void run() {
        super.run();

        end.set(false);

        HashMap<String, BufferedReader> updateMap = new HashMap<>();

        try {
            while (!end.get()) { // swapped by MainActivity

                BufferedReader reader;

                // Listen to each socket
                for (NameAndSocket client : clients) {

                    //if(!end.get()){

                    try {

                        reader = buildReader(client, updateMap);

                        // Read the update
                        String action = reader.readLine();

                        Log.d("GROUP_OWNER_RECEIVER:", "RECEIVED " + action);

                        if(action != null) {
                            switch (action) {

                                case NO_UPDATE:

                                    break;

                                case UPDATE:

                                    // Read the content
                                    String received = reader.readLine();

                                    if (received != null) {
                                        processMessage(received);

                                    } else {
                                        // Client has leaved, has closed the stream ( posso aver inviato messaggio di fine gruppo )

                                        removeClient(client, updateMap);
                                    }
                                    break;

                                case LEAVING:
                                    removeClient(client, updateMap);

                                    break;
                            }
                        }else{
                            // It's likely I'm closing my group, sending quit requests, clients are closing sockets
                            removeClient(client, updateMap);
                        }
                    } catch (IOException e) {

                        removeClient(client, updateMap);
                    }

                }

                if (clients.size() > 0) {
                    mainTask.sendUpdateToUI(UPDATE_THE_MAP);
                } else {
                    // This sleep is really important!! prevent the activity from squeeeeezing
                    mySleep(WAITING_TIME);
                }
            }
        }finally {

            for (NameAndSocket client : clients){
                try {
                    updateMap.get(client.getName()).close();
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }

        Log.d("GROUP_OWNER_RECEIVER:", "QUITS");
    }

    private void mySleep(int waitingTime) {
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(String received) {

        try{
            JSONObject update = new JSONObject(received);

            // Every key is a device name, every value contains (name, latitude, longitude);
            Iterator<String> iter = update.keys();
            while (iter.hasNext()) {
                String key = iter.next();

                JSONArray value = (JSONArray) update.get(key);

                double latitude = value.getDouble(0);
                double longitude = value.getDouble(1);

                Location loc = new Location("");
                loc.setLatitude(latitude);
                loc.setLongitude(longitude);

                if (locations.containsKey(key))
                    locations.replace(key, loc);
                else
                    locations.put(key, loc);

            }
        } catch (JSONException e) {
            // Something went wrong!
        }
    }

    private BufferedReader buildReader(NameAndSocket client, HashMap<String, BufferedReader> updateMap) throws IOException{
        BufferedReader reader;

        if (updateMap.containsKey(client.getName()))
            reader = updateMap.get(client.getName());
        else {
            //Log.d("SERVER_RECEIVER","CREO NUOVO READER PER " + client.getName());

            reader = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
            updateMap.put(client.getName(), reader);
        }
        return reader;
    }

    private void removeClient(NameAndSocket client, HashMap<String, BufferedReader> updateMap){
        // Client has leaved
        Log.d(client.getName(), "HAS LEAVED");

        mainTask.sendLeaveUpdateToUI(client.getName());
        try {
            if (!client.getSocket().isClosed())
                client.getSocket().close();
        } catch (IOException e) {
            e.getMessage();
        }

        clients.remove(client);

        if(locations.containsKey(client.getName()))
            locations.remove(client.getName());

        if(updateMap.containsKey(client.getName()))
            updateMap.remove(client.getName());

        if (clients.size() == 0)
            mainTask.sendUpdateToUI(UPDATE_THE_MAP);
    }
}
