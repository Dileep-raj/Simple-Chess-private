package com.drdedd.simplechess_temp;

import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;

public interface BoardInterface {
    /**
     * Searches for piece at given row and column <br>
     * 0 < Row & Column < 7
     *
     * @return Piece | null
     */
    Piece pieceAt(int row, int col);

    /**
     * Moves the piece from a position
     *
     * @return Move result
     */
    boolean movePiece(int fromRow, int fromCol, int toRow, int toCol);

    void addToPGN(Piece piece, String move, int fromRow, int fromCol);

    void removePiece(Piece piece);

    void promote(Pawn pawn, int row, int col, int fromRow, int fromCol);

    BoardModel getBoardModel();

    HashMap<Piece, HashSet<Integer>> getLegalMoves();
}