package tetris.model.rules;

import tetris.model.Board;
import tetris.model.piece.ActivePiece;

public class SrsRotation implements RotationStrategy {
    @Override public boolean tryRotateCW(ActivePiece p, Board b) {
        int before = p.rotation();
        p.rotateCW();
        if (b.canPlace(p)) return true;
        // simple wall-kick: try left, right
        if (b.tryNudge(p, 1, 0) || b.tryNudge(p, -1, 0)) return true;
        // revert
        for (int i = 0; i < 3; i++) p.rotateCW();
        return false;
    }
}
