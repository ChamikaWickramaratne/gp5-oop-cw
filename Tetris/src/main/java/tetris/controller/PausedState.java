package tetris.controller;

public class PausedState extends BaseState {
    private final GameplayController c;
    public PausedState(GameplayController c){ this.c = c; }

    @Override public void onEnter() { c.pauseMusicIfEnabled(); }
    @Override public boolean isPaused() { return true; }
    @Override public boolean allowsHumanInput() { return false; }
}
