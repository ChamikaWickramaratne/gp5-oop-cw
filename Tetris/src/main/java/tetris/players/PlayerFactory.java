package tetris.players;

import tetris.net.ExternalPlayerClient;
import tetris.net.INetwork;

public class PlayerFactory {
    public static void configureGameplayForType(
            tetris.ui.Gameplay gameplay,
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
