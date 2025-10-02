// src/main/java/tetris/controller/state/RunningState.java
package tetris.controller;

public class RunningState extends BaseState {
    private final GameplayController c;
    public RunningState(GameplayController c){ this.c = c; }

    @Override public void onEnter() {
        c.resumeMusicIfEnabled();
        c.resetDropTimer(); // avoid instant drop spike after resume
    }

    @Override public void onTick(long now) {
        // Mirrors your old "if (!paused && !gameOver) { ... }" tick body
        c.applyAutoBoostIfNeeded();
        c.tryReconnectIfNeeded();

        if (c.getLastDropTime() == 0) {
            c.setLastDropTime(now);
            return;
        }

        if (now - c.getLastDropTime() > c.getDropSpeedNanos()) {
            c.stepBrainsOnce();            // AI/External micro-step
            boolean fell = c.tryGravity(); // drop exactly one row

            if (!fell) {
                c.lockPieceAndSpawn();     // also clears flags
            }
            c.setLastDropTime(now);
        }
    }
}
