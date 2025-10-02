// src/main/java/tetris/controller/state/GameOverState.java
package tetris.controller;

public class GameOverState extends BaseState {
    private final GameplayController c;
    public GameOverState(GameplayController c){ this.c = c; }

    @Override public void onEnter() {
        c.stopTimer();
        c.onGameOverDialog(); // name entry + high score screen
    }
    @Override public boolean isGameOver() { return true; }
    @Override public boolean allowsHumanInput() { return false; }
}
