package tetris.config;

import tetris.model.PlayerType;

public class TetrisConfig {

    private static volatile TetrisConfig instance;

    private int fieldWidth  = 10;
    private int fieldHeight = 20;
    private int gameLevel   = 1;
    private boolean music = true;
    private boolean soundEffect = true;
    private boolean extendMode  = false;
    private PlayerType player1Type = PlayerType.HUMAN;
    private PlayerType player2Type = PlayerType.HUMAN;

    public TetrisConfig() {}

    public static TetrisConfig getInstance() {
        if (instance == null) {
            synchronized (TetrisConfig.class) {
                if (instance == null) {
                    instance = ConfigService.load();
                }
            }
        }
        return instance;
    }

    public static void setInstance(TetrisConfig cfg) {
        instance = (cfg != null) ? cfg : new TetrisConfig();
    }

    public int getFieldWidth() { return fieldWidth; }
    public void setFieldWidth(int v) { fieldWidth = v; }

    public int getFieldHeight() { return fieldHeight; }
    public void setFieldHeight(int v) { fieldHeight = v; }

    public int getGameLevel() { return gameLevel; }
    public void setGameLevel(int v) { gameLevel = v; }

    public boolean isMusic() { return music; }
    public void setMusic(boolean v) { music = v; }

    public boolean isSoundEffect() { return soundEffect; }
    public void setSoundEffect(boolean v) { soundEffect = v; }

    public boolean isExtendMode() { return extendMode; }
    public void setExtendMode(boolean v) { extendMode = v; }

    public PlayerType getPlayer1Type() { return player1Type; }
    public void setPlayer1Type(PlayerType t) { player1Type = t; }

    public PlayerType getPlayer2Type() { return player2Type; }
    public void setPlayer2Type(PlayerType t) { player2Type = t; }
}
