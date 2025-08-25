package tetris.model.rules;

import tetris.model.Board;
import tetris.model.piece.ActivePiece;

public interface RotationStrategy {  // INTERFACE
    boolean tryRotateCW(ActivePiece piece, Board board);
}