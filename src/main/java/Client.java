import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    private volatile boolean done;

    public Client() {
        done = false;
    }

    public void start() {
        try {
            client = new Socket("127.0.0.1", 15001);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while (!done && (inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (Exception e) {}
    }

    public void shutdown() {
        done = true;
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if ("/quit".equals(message)) {
                        out.println(message);
                        inReader.close();
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {}
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
