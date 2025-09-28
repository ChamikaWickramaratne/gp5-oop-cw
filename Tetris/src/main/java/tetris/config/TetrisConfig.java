package tetris.config;

public class TetrisConfig {

    private int fieldWidth = 10;
    private int fieldHeight = 20;
    private int gameLevel = 1;
    private boolean music = true;
    private boolean soundEffect = true;
    private boolean aiPlay = false;
    private boolean extendMode = false;

    public int getFieldWidth() { return fieldWidth; }
    public void setFieldWidth(int fieldWidth) { this.fieldWidth = fieldWidth; }

    public int getFieldHeight() { return fieldHeight; }
    public void setFieldHeight(int fieldHeight) { this.fieldHeight = fieldHeight; }

    public int getGameLevel() { return gameLevel; }
    public void setGameLevel(int gameLevel) { this.gameLevel = gameLevel; }

    public boolean isMusic() { return music; }
    public void setMusic(boolean music) { this.music = music; }

    public boolean isSoundEffect() { return soundEffect; }
    public void setSoundEffect(boolean soundEffect) { this.soundEffect = soundEffect; }

    public boolean isAiPlay() { return aiPlay; }
    public void setAiPlay(boolean aiPlay) { this.aiPlay = aiPlay; }

    public boolean isExtendMode() { return extendMode; }
    public void setExtendMode(boolean extendMode) { this.extendMode = extendMode; }
}

