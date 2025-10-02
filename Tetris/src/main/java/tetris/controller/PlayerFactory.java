package tetris.controller;

import tetris.config.PlayerType;
//import tetris.view.Gameplay;

public final class PlayerFactory {
    private PlayerFactory() {}

    public static void configureForType(GameplayController gameplay,
                                        PlayerType type,
                                        String host, int port) {
        switch (type) {
            case HUMAN -> {}
            case AI     -> gameplay.enableAI(new tetris.model.ai.BetterHeuristic());
            case EXTERNAL -> gameplay.enableExternal(host, port);
        }
    }

    public static void configureForType(GamePane pane,
                                        PlayerType type,
                                        String host, int port) throws Exception {
        switch (type) {
            case HUMAN -> {}
            case AI     -> pane.enableAI(new tetris.model.ai.BetterHeuristic());
            case EXTERNAL -> pane.enableExternal(host, port);
        }
    }
}
