package com.drdedd.simplechess_temp.interfaces;

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;

public interface BoardInterface {
    /**
     * Searches for piece at given row and column <br>
     * 0 < Row & Column < 7
     *
     * @return <code>Piece|null</code>
     */
    Piece pieceAt(int row, int col);

    /**
     * Moves the piece from a position
     *
     * @return Move result
     */
    boolean movePiece(int fromRow, int fromCol, int toRow, int toCol);

    /**
     * Add move to PGN
     *
     * @param piece   <code>Piece</code> which was moved
     * @param move    Special moves (if any)
     * @param fromRow Previous row position
     * @param fromCol Previous column position
     */
    void addToPGN(Piece piece, String move, int fromRow, int fromCol);

    /**
     * Remove a piece from the board
     *
     * @return
     */
    boolean capturePiece(Piece piece);

    /**
     * Promote a pawn to higher rank
     */
    void promote(Pawn pawn, int row, int col, int fromRow, int fromCol);

    /**
     * Current <code>BoardModel</code> object
     */
    BoardModel getBoardModel();

    /**
     * Legal moves for all pieces
     */
    HashMap<Piece, HashSet<Integer>> getLegalMoves();
}