// src/main/java/tetris/controller/state/GameState.java
package tetris.controller;

public interface GameState {
    void onEnter();
    void onExit();
    void onTick(long now);       // called every animation frame
    boolean isPaused();
    boolean isGameOver();
    boolean allowsHumanInput(); // should human movement keys do anything?
}
