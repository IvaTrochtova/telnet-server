package utb.fai;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Server{

    private final int port;
    private final ExecutorService service;
    private final Set<Socket> activeSockets;
    private final Set<Socket> rejectedSockets;
    private ServerSocket serverSocket;

    private final int maxThreads;

    public Server(int port){
        this(port, 10);
    }

    public Server(int port, int maxThreads){
        this.port = port;
        this.maxThreads = maxThreads;
        this.service = Executors.newFixedThreadPool(maxThreads);
        this.activeSockets = ConcurrentHashMap.newKeySet();
        this.rejectedSockets = ConcurrentHashMap.newKeySet();
    }

    public void start(){

        serverTerminal();

        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            this.serverSocket = serverSocket;
            System.out.println("Server běží na portu: " + serverSocket.getLocalPort());

            while (!this.service.isShutdown()) {
                Socket socket = serverSocket.accept();
                if (this.activeSockets.size() >= this.maxThreads) {
                    this.rejectedSockets.add(socket);
                    Thread ignoredClient = new Thread(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                            while (in.readLine() != null) {
                                // Ignore messages from clients over the limit.
                            }
                        } catch (IOException ignored) {
                        } finally {
                            this.rejectedSockets.remove(socket);
                            try { socket.close(); } catch (IOException ignored) { }
                        }
                    });
                    ignoredClient.setDaemon(true);
                    ignoredClient.start();
                    continue;
                }
                this.activeSockets.add(socket);
                System.out.println("Připojil se nový klSient: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket,this.activeSockets);
                try {
                    this.service.execute(handler);    
                } catch (RejectedExecutionException ex) {
                    System.out.println("Server je přetížený");
                    socket.close();
                    this.activeSockets.remove(socket);
                }
                
            }
            
        } catch (IOException e) {
            if(this.serverSocket != null && this.serverSocket.isClosed()){
                System.out.println("Server socket byl ukončen");
            }else{
                e.printStackTrace();
            }
        } finally{
            this.close();
        }
    }

    public synchronized void close(){
        try {
            if(this.serverSocket != null && !this.serverSocket.isClosed())
                this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Socket s : this.activeSockets){
            try {
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Socket s : this.rejectedSockets) {
            try { s.close(); } catch (IOException ignored) { }
        }

        this.service.shutdown();
        try {
            if(!this.service.awaitTermination(60,TimeUnit.SECONDS)){
                this.service.shutdown();
                if(!this.service.awaitTermination(60,TimeUnit.SECONDS)){
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            this.service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void serverTerminal(){
        Thread terminalThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while(scanner.hasNextLine()){
                String line = scanner.nextLine().trim();
                if(line.equalsIgnoreCase("quit")){
                    System.out.println("Ukončuju server!");
                    close();
                    break;
                }
            }
            scanner.close();
        });
        terminalThread.setDaemon(true);
        terminalThread.start();
    }
}