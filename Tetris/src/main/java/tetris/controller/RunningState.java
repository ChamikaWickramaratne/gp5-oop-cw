package tetris.controller;

public class RunningState extends BaseState {
    private final GameplayController c;
    public RunningState(GameplayController c){ this.c = c; }

    @Override public void onEnter() {
        c.resumeMusicIfEnabled();
        c.resetDropTimer();
    }

    @Override public void onTick(long now) {
        c.applyAutoBoostIfNeeded();
        c.tryReconnectIfNeeded();

        if (c.getLastDropTime() == 0) {
            c.setLastDropTime(now);
            return;
        }

        if (now - c.getLastDropTime() > c.getDropSpeedNanos()) {
            c.stepBrainsOnce();
            boolean fell = c.tryGravity();

            if (!fell) {
                c.lockPieceAndSpawn();
            }
            c.setLastDropTime(now);
        }
    }
}
