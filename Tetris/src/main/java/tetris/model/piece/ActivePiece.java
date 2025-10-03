package tetris.model.piece;

import java.util.List;
import tetris.model.TetrominoType;
import tetris.model.Vec;

public class ActivePiece extends Piece {
    private Vec origin;

    public ActivePiece(TetrominoType t, Vec origin) { super(t); this.origin = origin; }
    
    public void moveBy(int dx, int dy) { origin = new Vec(origin.x() + dx, origin.y() + dy); }
    public void rotateCW() { rotation = (rotation + 1) & 3; }

    public List<Vec> worldCells() {
        return localCells().stream().map(v -> new Vec(v.x() + origin.x(), v.y() + origin.y())).toList();
    }
}
