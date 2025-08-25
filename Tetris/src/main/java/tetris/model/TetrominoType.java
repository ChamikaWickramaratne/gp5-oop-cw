package tetris.model;

public enum TetrominoType { I, O, T, S, Z, J, L;

    // ENHANCED SWITCH – canonical offsets at rotation 0
    public Vec[] offsets() {
        return switch (this) {
            case I -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(3,0) };
            case O -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(0,1), new Vec(1,1) };
            case T -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(1,1) };
            case L -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(2,1) };
            case J -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(2,0), new Vec(0,1) };
            case S -> new Vec[]{ new Vec(1,0), new Vec(2,0), new Vec(0,1), new Vec(1,1) };
            case Z -> new Vec[]{ new Vec(0,0), new Vec(1,0), new Vec(1,1), new Vec(2,1) };
        };
    }
}
