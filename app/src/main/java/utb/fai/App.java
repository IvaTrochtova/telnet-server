

package utb.fai;

import java.net.ServerSocket;

public class App {

    public static void main(String[] args) {
        int port = 12345;
        int max_threads = 10;    

        // Implement input parameter processing
        if (args.length > 0) port = Integer.parseInt(args[0]);
        if (args.length > 1) max_threads = Integer.parseInt(args[1]);
        
        // Implementation of the main server loop
        Server server = new Server(port, max_threads);
        server.start();
    }
}

