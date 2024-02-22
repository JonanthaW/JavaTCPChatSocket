import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connectionList;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connectionList = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
                server = new ServerSocket(15001);
                pool = Executors.newCachedThreadPool();
                while (!done) {
                    Socket client = server.accept();
                    ConnectionHandler handler = new ConnectionHandler(client);
                    connectionList.add(handler);
                    pool.execute(handler);
            }
        } catch (IOException e) {}
    }


    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch: connectionList) {
                ch.shutdown();
            }
        } catch (IOException e) {}
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname");
                nickname = in.readLine(); // Do validation if necessary
                System.out.println("\n" + nickname + " connected!");
                broadcast(nickname + " Joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split("", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " changed to " + messageSplit[1]);
                            System.out.println(nickname + " changed to" + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("nickname changed successfully");
                        } else {
                            out.println("No nickname provided");
                        }
                    } else if (message.startsWith("/quit")) {
                       broadcast(nickname + " Left the chat");
                       connectionList.remove(this);
                       shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }

            } catch (IOException e) {}

        }

        public void broadcast(String message) {
            for (ConnectionHandler ch: connectionList) {
                if(ch != null) {
                    ch.sendMessage(message);
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                if (in != null) {
                    in.close();
                    out.close();
                    if (client != null  && !client.isClosed()) {
                        client.close();
                    }
                }} catch (IOException e) {}
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
    }

}
