package tetris.players;

import java.util.function.Consumer;
import tetris.dto.PureGame;
import tetris.dto.OpMove;

public interface Player {
    void requestMoveAsync(PureGame game,
                          Consumer<OpMove> onReady,
                          Consumer<Throwable> onError);
}
