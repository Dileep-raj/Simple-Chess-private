package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.io.Serializable;
import java.util.HashSet;

public abstract class Piece implements Serializable, Cloneable {
    private final Player player;
    private int col, row;
    private final int resID;
    private final Rank rank;
    protected boolean moved;

    protected Piece(Player player, int row, int col, Rank rank, int resID) {
        this.player = player;
        this.row = row;
        this.col = col;
        this.rank = rank;
        this.resID = resID;
    }

    public Player getPlayer() {
        return player;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Rank getRank() {
        return rank;
    }

    public boolean isWhite() {
        return player == Player.WHITE;
    }

    public int getResID() {
        return resID;
    }

    /**
     * Converts logical position to standard notation
     *
     * @return Standard algebraic notation of the position
     */
    public String getPosition() {
        char ch = rank.toString().charAt(0);
        if (rank == Rank.KNIGHT) ch = 'N';
        return "" + ch + (char) ('a' + col) + (row + 1);
    }

    public void moveTo(int row, int col) {
        this.row = row;
        this.col = col;
        this.moved = true;
    }

    /**
     * Checks if a piece can move to a new location on the board
     *
     * @return true if piece can move, false otherwise
     */
    public abstract boolean canMoveTo(BoardInterface boardInterface, int row, int col);

    /**
     * Checks if a piece can capture another piece on the board
     *
     * @return true if the piece is captureable, false otherwise
     */
    public abstract boolean canCapture(BoardInterface boardInterface, Piece capturingPiece);

    /**
     * Finds all legal moves of the piece on the board
     *
     * @return Set of legal positions of the piece
     */
    public abstract HashSet<Integer> getPossibleMoves(BoardInterface boardInterface);

    public boolean hasNotMoved() {
        return !moved;
    }

    public boolean addMove(HashSet<Integer> legalMoves, Piece piece, int row, int col) {
        if (piece == null) {
            legalMoves.add(row * 8 + col);
            return true;
        } else if (isKing() && piece.isKing()) return false;
        else if (piece.getPlayer() != getPlayer()) legalMoves.add(row * 8 + col);
        return false;
    }

    public boolean isKing() {
        return rank == Rank.KING;
    }

    @NonNull
    @Override
    public Piece clone() {
        try {
            return (Piece) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}