package tetris.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import tetris.model.dto.OpMove;
import tetris.model.dto.PureGame;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExternalPlayerClient implements INetwork {
    private final String host; private final int port;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile boolean running;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private NetworkListener listener;

    private ExecutorService readerExec; // for the read loop

    public ExternalPlayerClient(String host, int port) {
        this.host = host; this.port = port;
    }

    @Override
    public void connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // auto-flush
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            running = true;
            System.out.println("[client] connected to " + host + ":" + port);
            if (listener != null) listener.onConnectionRecovered();

            readerExec = Executors.newSingleThreadExecutor();
            readerExec.submit(this::readLoop);

        } catch (IOException e) {
            System.out.println("[client] connect failed: " + e);
            if (listener != null) listener.onConnectionLost();
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                System.out.println("[client] <- " + line);
                OpMove mv = mapper.readValue(line, OpMove.class);
                if (listener != null) listener.onMoveReceived(mv);
            }
        } catch (IOException e) {
            System.out.println("[client] read loop ended: " + e);
            if (listener != null) listener.onConnectionLost();
        } finally {
            closeQuietly();
        }
    }

    @Override
    public void sendGameAsync(PureGame game) {
        io.submit(() -> {
            try {
                if (out == null) {
                    System.out.println("[client] out is null; dropping snapshot");
                    return;
                }
                String payload = mapper.writeValueAsString(game);
                System.out.println("[client] -> " + payload);
                out.println(payload);
            } catch (Exception e) {
                if (listener != null) listener.onProtocolError("send failed", e);
            }
        });
    }

    @Override
    public void disconnect() {
        running = false;
        closeQuietly();
        if (readerExec != null) readerExec.shutdownNow();
        io.shutdownNow();
    }

    @Override public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    @Override public void setListener(NetworkListener l) { this.listener = l; }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
