package tetris.controller;

public interface GameState {
    void onEnter();
    void onExit();
    void onTick(long now);
    boolean isPaused();
    boolean isGameOver();
    boolean allowsHumanInput();
}
