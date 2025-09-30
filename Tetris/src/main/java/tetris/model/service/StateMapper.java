package tetris.model.service;

import tetris.model.dto.PureGame;
import tetris.model.Board;
import tetris.model.TetrominoType;
import tetris.model.piece.ActivePiece;

public final class StateMapper {
    public static PureGame snapshot(Board board, ActivePiece active, TetrominoType next) {
        PureGame p = new PureGame();
        p.width  = board.getWidth();
        p.height = board.getHeight();
//        p.cells  = board.copyCells();                 // provide a deep copy
//        p.currentShape = active.toMatrix();           // build from ActivePiece+rotation
//        p.nextShape    = Piece.toMatrix(next, 0);     // 0 = canonical rotation
        return p;
    }
}
