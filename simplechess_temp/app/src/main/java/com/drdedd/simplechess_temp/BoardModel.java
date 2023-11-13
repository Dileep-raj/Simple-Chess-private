package com.drdedd.simplechess_temp;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.pieces.Bishop;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Knight;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;
import com.drdedd.simplechess_temp.pieces.Queen;
import com.drdedd.simplechess_temp.pieces.Rook;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class BoardModel implements Serializable {
    /**
     * Set of all the pieces on the board
     */
    private final Set<Piece> pieces = new HashSet<>();

    BoardModel() {
        resetBoard();
    }

    /**
     * Resets the board to initial state
     */
    public void resetBoard() {
        int i;
        pieces.clear();
        for (i = 0; i <= 1; i++) {
//            Rook pieces
            addPiece(new Rook(Player.WHITE, 0, i * 7, i * 7, R.drawable.rw));
            addPiece(new Rook(Player.BLACK, 7, i * 7, i * 7 + 56, R.drawable.rb));

//            Bishop pieces
            addPiece(new Bishop(Player.WHITE, 0, 2 + i * 3, 2 + i * 3, R.drawable.bw));
            addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, 2 + i * 3 + 56, R.drawable.bb));

//            Knight pieces
            addPiece(new Knight(Player.WHITE, 0, 1 + i * 5, 1 + i * 5, R.drawable.nw));
            addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, 1 + i * 5 + 56, R.drawable.nb));
        }

//        King and Queen pieces
        addPiece(new King(Player.WHITE, 0, 4, 4, R.drawable.kw));
        addPiece(new Queen(Player.WHITE, 0, 3, 3, R.drawable.qw));
        addPiece(new King(Player.BLACK, 7, 4, 4 + 56, R.drawable.kb));
        addPiece(new Queen(Player.BLACK, 7, 3, 3 + 56, R.drawable.qb));

//        Pawn pieces
        for (i = 0; i < 8; i++) {
            addPiece(new Pawn(Player.WHITE, 1, i, i + 8, R.drawable.pw));
            addPiece(new Pawn(Player.BLACK, 6, i, i + 48, R.drawable.pb));
        }
    }

    public Piece pieceAt(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        for (Piece piece : pieces) {
            if (piece.getCol() == col && piece.getRow() == row) {
                return piece;
            }
        }
        return null;
    }

    /**
     * Converts logical position to standard notation
     *
     * @return Standard algebraic notation of a position
     */
    public String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder board = new StringBuilder("\n");
        int i, j;
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 8; j++) {
                Piece tempPiece = pieceAt(7 - i, j);
                char ch = 0;
                if (tempPiece == null) board.append(". ");
                else {
                    Rank r = tempPiece.getRank();
                    if (r == Rank.PAWN) ch = 'p';
                    else if (r == Rank.BISHOP) ch = 'b';
                    else if (r == Rank.KNIGHT) ch = 'n';
                    else if (r == Rank.ROOK) ch = 'r';
                    else if (r == Rank.QUEEN) ch = 'q';
                    else if (r == Rank.KING) ch = 'k';
                    if (tempPiece.isWhite()) ch = Character.toUpperCase(ch);
                    board.append(ch).append(" ");
                }
            }
            board.append("\n");
        }
        return String.valueOf(board);
    }
}