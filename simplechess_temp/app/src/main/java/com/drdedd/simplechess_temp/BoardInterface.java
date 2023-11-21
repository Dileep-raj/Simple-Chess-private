package com.drdedd.simplechess_temp;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.pieces.Piece;

public interface BoardInterface {
    /**
     * Searches for piece at given row and column
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

    void addToPGN(Piece piece, String move);

    void removePiece(Piece piece);
}
