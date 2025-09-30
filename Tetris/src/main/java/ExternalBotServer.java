import java.io.*;
import java.net.*;
import java.util.*;

public class ExternalBotServer {
    public static void main(String[] args) throws Exception {
        int port = 3000;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("listening on 127.0.0.1:" + port);
            while (true) {
                Socket sock = server.accept();
                new Thread(() -> handle(sock)).start();
            }
        }
    }
    static void handle(Socket sock) {
        System.out.println("client connected: " + sock.getRemoteSocketAddress());
        try (sock;
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true)) {
            Random rnd = new Random();
            String line;
            while ((line = in.readLine()) != null) {
                // Parse width crudely to keep dependencies out:
                int width = 10;
                int idx = line.indexOf("\"width\"");
                if (idx >= 0) {
                    int colon = line.indexOf(':', idx);
                    if (colon >= 0) {
                        int end = line.indexOf(',', colon);
                        String num = (end > 0 ? line.substring(colon+1, end) : line.substring(colon+1)).trim();
                        try { width = Integer.parseInt(num); } catch (Exception ignore) {}
                    }
                }
                int opX = Math.max(0, Math.min(width - 1, rnd.nextInt(width)));
                int opR = rnd.nextInt(4);
                out.println("{\"opX\":" + opX + ",\"opRotate\":" + opR + "}");
            }
        } catch (IOException ignored) {}
        System.out.println("client closed.");
    }
}
