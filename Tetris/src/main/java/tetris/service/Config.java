package tetris.service;

public class Config {
    public int fieldWidth;
    public int fieldHeight;
    public boolean musicOn;
    public boolean soundOn;

    public Config(int fieldWidth, int fieldHeight, boolean musicOn, boolean soundOn) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.musicOn = musicOn;
        this.soundOn = soundOn;
    }
}
