package tetris.model.rules;

import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.piece.ActivePiece;

public class SrsRotation implements RotationStrategy {
    @Override public boolean tryRotateCW(ActivePiece p, Board b) {
        if (p.type() == TetrominoType.O) {
            return false;
        }
        p.rotateCW();
        if (b.canPlace(p)) return true;
        if (b.tryNudge(p, 1, 0) || b.tryNudge(p, -1, 0)) return true;
        for (int i = 0; i < 3; i++) p.rotateCW();
        return false;
    }
}
