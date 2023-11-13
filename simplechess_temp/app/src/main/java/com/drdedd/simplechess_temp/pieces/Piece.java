package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.io.Serializable;
import java.util.Set;

public abstract class Piece implements Serializable {
    private final Player player;
    private int col, row, absolutePosition;
    private final int resID;
    private final Rank rank;

    protected Piece(Player player, int row, int col, int absolutePosition, Rank rank, int resID) {
        this.player = player;
        this.row = row;
        this.col = col;
        this.absolutePosition = absolutePosition;
        this.rank = rank;
        this.resID = resID;
    }

    public Player getPlayerType() {
        return player;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getAbsolutePosition() {
        return absolutePosition;
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
        char ch = 'P';
        if (rank == Rank.BISHOP) ch = 'B';
        else if (rank == Rank.KNIGHT) ch = 'N';
        else if (rank == Rank.ROOK) ch = 'R';
        else if (rank == Rank.QUEEN) ch = 'Q';
        else if (rank == Rank.KING) ch = 'K';
        return "" + ch + (char) ('a' + col) + (row + 1);
    }

    public void moveTo(int row, int col) {
        moveTo(col + row * 8);
        this.row = row;
        this.col = col;
    }

    public void moveTo(int absolutePosition) {
        this.absolutePosition = absolutePosition;
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
    public abstract Set<Integer> getLegalMoves(BoardInterface boardInterface);

}