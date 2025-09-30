package tetris.controller;
import tetris.model.dto.PureGame;

public interface INetwork {
    void connect();
    void disconnect();
    boolean isConnected();
    void sendGameAsync(PureGame game);
    void setListener(NetworkListener l);
}

