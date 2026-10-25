package utb.fai;

import java.io.*;
import java.net.*;
import java.util.Set;


public class ClientHandler implements Runnable{
    
    private Socket socket;
    private final Set<Socket> activeSockets;

    public ClientHandler(Socket socket,Set<Socket> activeSockets){
        this.socket = socket;
        this.activeSockets = activeSockets;
    }
    
    @Override 
    public void run(){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);


            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Klient (" + socket.getInetAddress() + ") napsal: " + line);
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                this.socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            this.activeSockets.remove(socket);
            System.out.println("Klient (" + this.socket.getInetAddress() + ") byl odpojen!");
        }

    }
    
}