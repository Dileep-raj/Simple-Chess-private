package com.drdedd.simplechess_temp.data;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.pieces.Piece;

public class DataConverter {

    /**
     * Converts absolute position to column number
     */
    public static int toCol(int position) {
        return position % 8;
    }

    /**
     * Converts absolute position to row number
     */
    public static int toRow(int position) {
        return position / 8;
    }

    /**
     * Converts notation to column number
     */
    public static int toCol(String position) {
        return position.charAt(0) - 'a';
    }

    /**
     * Converts notation to row number
     */
    public static int toRow(String position) {
        return position.charAt(1) - '1';
    }

    /**
     * Converts absolute position to Standard Notation
     */
    public static String toNotation(int position) {
        return "" + (char) ('a' + position % 8) + (position / 8 + 1);
    }

    /**
     * Converts row and column numbers to Standard Notation
     */
    public static String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    /**
     * Opponent player for the given <code>Player</code>
     *
     * @return <code>White|Black</code>
     */
    public static Player opponentPlayer(Player player) {
        return player == Player.WHITE ? Player.BLACK : Player.WHITE;
    }

    /**
     * Converts piece to letter for FEN representation
     *
     * @param piece Piece to be converted
     * @return <code>K|Q|R|N|B|P</code> - Uppercase or Lowercase
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">More about FEN</a>
     */
    public static char getPieceChar(@NonNull Piece piece) {
        char ch = piece.getRank().getLetter();
        if (!piece.isWhite()) ch = Character.toLowerCase(ch);
        return ch;
    }
}