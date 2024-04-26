package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Abstract class for chess piece
 */
public abstract class Piece implements Serializable, Cloneable {
    private final Player player;
    private int col, row;
    private final int resID;
    private final Rank rank;
    protected boolean moved, captured;
    private final String unicode;

    /**
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param rank   {@link Rank} of the piece
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    protected Piece(Player player, int row, int col, Rank rank, int resID, String unicode) {
        this.player = player;
        this.row = row;
        this.col = col;
        this.rank = rank;
        this.resID = resID;
        this.unicode = unicode;
        this.captured = false;
    }

    public String getUnicode() {
        return unicode;
    }

    /**
     * Returns Player type of the piece
     *
     * @return {@link Player} - <code>WHITE|BLACK</code>
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns column number of the piece
     *
     * @return <code>int 0-7</code>
     */
    public int getCol() {
        return col;
    }

    /**
     * Returns row number of the piece
     *
     * @return <code>int 0-7</code>
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns {@link Rank} of the piece
     *
     * @return <code>King|Queen|Rook|Knight|Bishop|Pawn</code>
     */
    public Rank getRank() {
        return rank;
    }

    /**
     * Returns whether the piece is white piece or not
     *
     * @return <code>True|False</code>
     */
    public boolean isWhite() {
        return player == Player.WHITE;
    }

    /**
     * Returns Resource ID of the piece
     *
     * @return <code>int</code>
     */
    public int getResID() {
        return resID;
    }

    /**
     * Converts logical position to standard notation
     *
     * @return Standard algebraic notation of the position
     */
    public String getPosition() {
        char ch = rank == Rank.KNIGHT ? 'N' : rank.toString().charAt(0);
        return "" + ch + (char) ('a' + col) + (row + 1);
    }

    /**
     * Moves the piece to the given position
     *
     * @param row Row number of new position
     * @param col Column number of new position
     */
    public void moveTo(int row, int col) {
        this.row = row;
        this.col = col;
        this.moved = true;
    }

    /**
     * Checks if a piece can move to a new location on the board
     *
     * @return <code>True|False</code>
     */
    public abstract boolean canMoveTo(BoardInterface boardInterface, int row, int col);

    /**
     * Checks if a piece can capture another piece on the board
     *
     * @return <code>True|False</code>
     */
    public abstract boolean canCapture(BoardInterface boardInterface, Piece capturingPiece);

    /**
     * Returns whether the {@link Piece} has moved or not
     *
     * @return <code>True|False</code>
     */
    public boolean hasNotMoved() {
        return !moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    /**
     * Adds a move to possible moves of the piece
     *
     * @param possibleMoves    <code>HashSet</code> of possible moves of the piece
     * @param obstructingPiece <code>Piece</code> obstructing further moves
     * @param row              Row number of the move
     * @param col              Column number of the move
     * @return <code>Boolean</code> - Continue adding moves without obstruction
     */
    protected boolean addMove(HashSet<Integer> possibleMoves, Piece obstructingPiece, int row, int col) {
        if (obstructingPiece == null) {
            possibleMoves.add(row * 8 + col);
            return true;
        } else if (obstructingPiece.getPlayer() != getPlayer()) possibleMoves.add(row * 8 + col);
        return false;
    }

    /**
     * Returns whether the piece is <code>King</code> or not
     *
     * @return <code>True|False</code>
     */
    public boolean isKing() {
        return rank == Rank.KING;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    @NonNull
    @Override
    public String toString() {
        return unicode;
    }

    /**
     * Finds all possible moves of the piece on the board
     *
     * @param boardInterface BoardInterface of the current board
     * @return <code>HashSet</code> of possible positions of the piece
     */
    public abstract HashSet<Integer> getPossibleMoves(BoardInterface boardInterface);

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