package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;

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

    /**
     * @return <code>String</code> - Unicode character of the piece
     */
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
     * Column number of the piece
     *
     * @return <code>int 0-7</code>
     */
    public int getCol() {
        return col;
    }

    /**
     * Row number of the piece
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
     * @return <code>true|false</code> - Piece belongs to white
     */
    public boolean isWhite() {
        return player == Player.WHITE;
    }

    /**
     *
     * @return <code>int</code> - Resource ID of the piece
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
        return String.format(Locale.ENGLISH, "%s%s%d", getRankChar(), (char) ('a' + col), row + 1);
    }

    /**
     * Character of rank of the piece
     *
     * @return <code>K|Q|R|B|N|P</code>
     */
    public char getRankChar() {
        return rank.getLetter();
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
     * @param boardInterface BoardInterface of the game
     * @param row Row number
     * @param col Column number
     * @return <code>true|false</code> - Piece can move to the position on the board
     */
    public abstract boolean canMoveTo(BoardInterface boardInterface, int row, int col);

    /**
     * @return <code>true|false</code> - Piece can capture another piece on the board
     */
    public abstract boolean canCapture(BoardInterface boardInterface, Piece capturingPiece);

    /**
     * @return <code>true|false</code> - Piece has moved
     */
    public boolean hasNotMoved() {
        return !moved;
    }

    /**
     * Sets piece has moved
     *
     * @param moved Piece has moved
     */
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
     * @return <code>true|false</code> - Piece is <code>{@link King King}</code>
     */
    public boolean isKing() {
        return rank == Rank.KING;
    }

    /**
     * @return <code>true|false</code> - Piece is captured
     */
    public boolean isCaptured() {
        return captured;
    }

    /**
     * Sets whether piece is captured
     *
     * @param captured Piece captured
     */
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