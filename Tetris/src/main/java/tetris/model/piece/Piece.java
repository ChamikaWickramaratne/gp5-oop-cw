package tetris.model.piece;

import java.util.ArrayList;
import java.util.List;
import tetris.model.TetrominoType;
import tetris.model.Vec;

/** ABSTRACT base class (Rubric) */
public abstract class Piece {
    protected final TetrominoType type;
    protected int rotation; // 0..3

    protected Piece(TetrominoType type) { this.type = type; }

    public TetrominoType type() { return type; }
    public int rotation() { return rotation; }

    /** Local cells after rotation – uses ENHANCED FOR. */
    public List<Vec> localCells() {
        var list = new ArrayList<Vec>();
        for (var o : type.offsets()) list.add(rotate(o, rotation));
        return list;
    }

    protected static Vec rotate(Vec v, int rot) {
        Vec r = v;
        for (int i = 0; i < rot; i++) r = new Vec(r.y(), -r.x()); // 90° CW
        return r;
    }
}
