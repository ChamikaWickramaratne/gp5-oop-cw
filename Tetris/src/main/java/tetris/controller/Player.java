package tetris.controller;

import java.util.function.Consumer;
import tetris.model.dto.PureGame;
import tetris.model.dto.OpMove;

public interface Player {
    void requestMoveAsync(PureGame game,
                          Consumer<OpMove> onReady,
                          Consumer<Throwable> onError);
}
