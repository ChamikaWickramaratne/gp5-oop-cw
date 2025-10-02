// src/main/java/tetris/controller/state/BaseState.java
package tetris.controller;

public abstract class BaseState implements GameState {
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void onTick(long now) {}
    @Override public boolean isPaused() { return false; }
    @Override public boolean isGameOver() { return false; }
    @Override public boolean allowsHumanInput() { return true; }
}
