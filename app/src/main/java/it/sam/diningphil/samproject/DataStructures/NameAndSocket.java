package it.sam.diningphil.samproject.DataStructures;

import java.net.Socket;

public class NameAndSocket {

    private Socket socket;
    private String name;


    public NameAndSocket(String n, Socket s){
        name = n;
        socket = s;
    }

    public Socket getSocket(){ return socket; }
    public String getName(){ return name; }
}
