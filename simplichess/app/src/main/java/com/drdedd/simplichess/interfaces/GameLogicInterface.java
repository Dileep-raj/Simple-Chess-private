package com.drdedd.simplichess.interfaces;

import com.drdedd.simplichess.game.BoardModel;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Interface for chess board and game logic
 */
public interface GameLogicInterface {
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
    boolean move(int fromRow, int fromCol, int toRow, int toCol);

    /**
     * Remove a piece from the board
     *
     * @return Result of capturing piece
     */
    boolean capturePiece(Piece piece);

    /**
     * Promote a pawn to higher rank
     *
     * @param pawn    Pawn to be promoted
     * @param row     Row of the promotion square
     * @param col     Column of the promotion square
     * @param fromRow Starting row of the pawn
     * @param fromCol Starting column of the pawn
     */
    void promote(Pawn pawn, int row, int col, int fromRow, int fromCol);

    /**
     * Terminate game due to timeout of one player
     */
    void terminateByTimeOut(Player player);

    /**
     * Current <code>BoardModel</code> object
     */
    BoardModel getBoardModel();

    /**
     * Legal moves for all pieces
     */
    HashMap<Piece, HashSet<Integer>> getLegalMoves();

    /**
     * @return <code>true|false</code> - White to play
     */
    boolean isWhiteToPlay();

    /**
     * @return <code>true|false</code> - Game terminated
     */
    boolean isGameTerminated();

    /**
     * @param piece Piece to check
     * @return <code>true|false</code> - Whether piece belongs to active player
     */
    boolean isPieceToPlay(Piece piece);
}