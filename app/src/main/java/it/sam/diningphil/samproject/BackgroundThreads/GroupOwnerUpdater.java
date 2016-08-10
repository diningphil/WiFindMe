package it.sam.diningphil.samproject.BackgroundThreads;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import it.sam.diningphil.samproject.DataStructures.NameAndLocation;
import it.sam.diningphil.samproject.DataStructures.NameAndSocket;

public class GroupOwnerUpdater extends Thread {

    private static final int WAITING_TIME = 1500;
    private static final String UPDATE = "Update_message";
    private static final String CLOSING_GROUP = "Closing_message";
    private static final String NO_UPDATE = "NO_UPDATE";

    private CopyOnWriteArrayList<NameAndSocket> clients; // writes are rare

    private ConcurrentHashMap<String, Location> locations;

    public AtomicBoolean end;

    private  GroupOwnerServer.Task mainTask;
    

    public GroupOwnerUpdater(ConcurrentHashMap<String, Location> sharedLocations, CopyOnWriteArrayList<NameAndSocket> cls, GroupOwnerServer.Task mainTask){

        this.mainTask = mainTask;
        locations = sharedLocations;
        end = new AtomicBoolean();
        clients = cls;
    }

    @Override
    public void run() {
        super.run();

        end.set(false);

        BufferedWriter writer = null;

        HashMap<String, BufferedWriter> updateMap = new HashMap<>();

        while (!end.get()) { // swapped by Server

            if (clients.size() > 0) {

                JSONObject jsonObject = new JSONObject();

                jsonObject = buildUpdateToSend(jsonObject);

                for (NameAndSocket client : clients) {

                    try { // don't wanna die for a fu**** client

                        writer = buildWriter(client, updateMap);

                        sendUpdate(writer, jsonObject);

                    } catch (IOException e) {
                        //e.printStackTrace();

                        if (writer != null)
                            try {
                                writer.close();
                            } catch (IOException e1) { e.getMessage(); }

                        if (updateMap.containsKey(client.getName()))
                            updateMap.remove(client.getName());
                    }

                }
            }

            // Don't wanna send updates every moment (batch updates)
            mSleep(WAITING_TIME);
        }

        for (NameAndSocket client : clients) {
            try {

                writer = buildWriter(client, updateMap);

                if (writer != null) {
                    writer.write(CLOSING_GROUP);
                    writer.newLine();
                    writer.flush();
                }

            } catch (IOException e) {
                e.getMessage();
            }
        }

        Log.d("GROUP_OWNER_UPDATER:", "QUITS");

        mainTask.endReceiver(); // Avverto il receiver che può anche liberare tutte le strutture
    }

    private void sendUpdate(BufferedWriter writer, JSONObject jsonObject) throws IOException{

        if(locations.size() > 0) {
            writer.write(UPDATE);
            writer.newLine();
            writer.write(jsonObject.toString());
            writer.newLine();
            writer.flush();
        }else{
            writer.write(NO_UPDATE);
            writer.newLine();
            writer.flush();
        }
    }

    private BufferedWriter buildWriter(NameAndSocket client, HashMap<String, BufferedWriter> updateMap) throws IOException{
        BufferedWriter writer;

        if (updateMap.containsKey(client.getName()))
            writer = updateMap.get(client.getName());
        else {
            writer = new BufferedWriter(new OutputStreamWriter(client.getSocket().getOutputStream()));
            updateMap.put(client.getName(), writer);
        }
        return writer;
    }

    private JSONObject buildUpdateToSend(JSONObject jsonObject) {

        // Build the array to serialize
        for (Map.Entry<String, Location> loc : locations.entrySet()) {

            NameAndLocation update = new NameAndLocation(loc.getKey(), loc.getValue());

            // Contains latitude and longitude
            JSONArray array = update.getJsonArrayForLocation();

            try {
                jsonObject.put(loc.getKey(), array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    private void mSleep(int waitingTime) {
        try { Thread.sleep(waitingTime); } catch (InterruptedException e) { e.getMessage(); }
    }

    // This is called from the server
    public void addClient(Socket newSocket) {

        try {

            // I don't want to block indefinitely, neither GroupOwnerReceiver
            newSocket.setSoTimeout(15000);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));

            String name = bufferedReader.readLine();

            clients.add(new NameAndSocket(name, newSocket));

            mainTask.sendJoinUpdateToUI(name);
            //Log.d("TESTING:", "FOUND CLIENT NAME: " + name);

            newSocket.setSoTimeout(10000);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.getMessage();
        }
    }

}