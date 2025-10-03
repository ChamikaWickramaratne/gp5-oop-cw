package tetris.controller;
import tetris.model.dto.OpMove;

public interface NetworkListener {
    void onMoveReceived(OpMove move);
    void onConnectionLost();
    void onConnectionRecovered();
    void onProtocolError(String message, Throwable t);
}