package tetris.controller;

import java.util.function.Consumer;
import tetris.model.dto.PureGame;
import tetris.model.dto.OpMove;

public class HumanPlayer implements Player {

    @Override
    public void requestMoveAsync(PureGame game,
                                 Consumer<OpMove> onReady,
                                 Consumer<Throwable> onError) {
    }
}
