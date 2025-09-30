package tetris.controller;

import tetris.model.types.PlayerType;
import tetris.view.Gameplay;

public class PlayerFactory {
    public static void configureGameplayForType(
            Gameplay gameplay,
            PlayerType type,
            String host, int port
    ) {
        switch (type) {
            case HUMAN -> {
                // nothing to do
            }
//            case EXTERNAL -> {
//                gameplay.enableExternal(host, port); // add helper in step 2
//            }
        }
    }
}
