package tetris.net;
import tetris.dto.PureGame;

public interface INetwork {
    void connect();
    void disconnect();
    boolean isConnected();
    void sendGameAsync(PureGame game);
    void setListener(NetworkListener l);
}

