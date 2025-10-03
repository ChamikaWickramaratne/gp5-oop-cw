package tetris.controller;

import java.util.function.Consumer;
import javafx.application.Platform;
import tetris.model.dto.OpMove;
import tetris.model.dto.PureGame;

public class ExternalPlayer implements Player, NetworkListener {
    private final INetwork net;
    private Consumer<OpMove> onReady;
    private Consumer<Throwable> onError;

    public ExternalPlayer(INetwork net) {
        this.net = net;
        net.setListener(this);
    }

    @Override
    public void requestMoveAsync(PureGame game,
                                 Consumer<OpMove> onReady,
                                 Consumer<Throwable> onError) {
        this.onReady = onReady;
        this.onError = onError;
        net.sendGameAsync(game);
    }

    @Override public void onMoveReceived(OpMove move) {
        if (onReady != null) Platform.runLater(() -> onReady.accept(move));
    }

    @Override public void onProtocolError(String msg, Throwable t) {
        if (onError != null) Platform.runLater(() -> onError.accept(t));
    }

    @Override public void onConnectionLost()      { }
    @Override public void onConnectionRecovered() { }
}
