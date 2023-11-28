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
import java.util.HashMap;
import java.util.HashSet;

public class BoardModel implements Serializable, Cloneable {
    /**
     * Set of all the pieces on the board
     */
    private HashSet<Piece> pieces = new HashSet<>();
    public final HashMap<String, Integer> resIDs = new HashMap<>();

    private King whiteKing = null, blackKing = null;
    public static Pawn enPassantPawn = null;
//    private final String TAG = "BoardModel";

    BoardModel() {
        resetBoard();
        resIDs.put(Player.WHITE + Rank.QUEEN.toString(), R.drawable.qw);
        resIDs.put(Player.WHITE + Rank.ROOK.toString(), R.drawable.rw);
        resIDs.put(Player.WHITE + Rank.BISHOP.toString(), R.drawable.bw);
        resIDs.put(Player.WHITE + Rank.KNIGHT.toString(), R.drawable.nw);

        resIDs.put(Player.BLACK + Rank.QUEEN.toString(), R.drawable.qb);
        resIDs.put(Player.BLACK + Rank.ROOK.toString(), R.drawable.rb);
        resIDs.put(Player.BLACK + Rank.BISHOP.toString(), R.drawable.bb);
        resIDs.put(Player.BLACK + Rank.KNIGHT.toString(), R.drawable.nb);
    }

    /**
     * Resets the board to initial state
     */
    public void resetBoard() {
        int i;
        pieces.clear();
        for (i = 0; i <= 1; i++) {
//            Rook pieces
            addPiece(new Rook(Player.WHITE, 0, i * 7, R.drawable.rw));
            addPiece(new Rook(Player.BLACK, 7, i * 7, R.drawable.rb));

//            Bishop pieces
            addPiece(new Bishop(Player.WHITE, 0, 2 + i * 3, R.drawable.bw));
            addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, R.drawable.bb));

//            Knight pieces
            addPiece(new Knight(Player.WHITE, 0, 1 + i * 5, R.drawable.nw));
            addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, R.drawable.nb));
        }


//        King and Queen pieces
        whiteKing = new King(Player.WHITE, 0, 4, R.drawable.kw);
        blackKing = new King(Player.BLACK, 7, 4, R.drawable.kb);

        addPiece(whiteKing);
        addPiece(new Queen(Player.WHITE, 0, 3, R.drawable.qw));

        addPiece(blackKing);
        addPiece(new Queen(Player.BLACK, 7, 3, R.drawable.qb));

//        Pawn pieces
        for (i = 0; i < 8; i++) {
            addPiece(new Pawn(Player.WHITE, 1, i, R.drawable.pw));
            addPiece(new Pawn(Player.BLACK, 6, i, R.drawable.pb));
        }
    }

    public King getBlackKing() {
        if (blackKing == null)
            for (Piece piece : pieces) if (piece.isKing() && !piece.isWhite()) return (King) piece;
        return blackKing;
    }

    public King getWhiteKing() {
        if (whiteKing == null)
            for (Piece piece : pieces) if (piece.isKing() && piece.isWhite()) return (King) piece;
        return whiteKing;
    }

    public Piece pieceAt(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        for (Piece piece : pieces)
            if (piece.getCol() == col && piece.getRow() == row) return piece;
        return null;
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    public Piece promote(Piece pawn, Rank rank, int row, int col) {
        Piece piece = null;
        if (rank == Rank.QUEEN)
            piece = new Queen(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString()));
        if (rank == Rank.ROOK)
            piece = new Rook(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.ROOK.toString()));
        if (rank == Rank.BISHOP)
            piece = new Bishop(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString()));
        if (rank == Rank.KNIGHT)
            piece = new Knight(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString()));

        if (piece != null) {
            addPiece(piece);
//            Log.d(TAG, "promote: Promoted " + pawn.getPosition().charAt(1) + " file pawn to " + piece.getRank());
        }
        removePiece(pawn);
        return piece;
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
                if (tempPiece == null) board.append("- ");
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

    public String toFEN() {
        StringBuilder FEN = new StringBuilder();
        int i, j, c = 0;
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 8; j++) {
                Piece tempPiece = pieceAt(7 - i, j);
                char ch = 0;
                if (tempPiece == null) c++;
                else {
                    if (c > 0) {
                        FEN.append(c);
                        c = 0;
                    }
                    Rank r = tempPiece.getRank();
                    if (r == Rank.PAWN) ch = 'p';
                    else if (r == Rank.BISHOP) ch = 'b';
                    else if (r == Rank.KNIGHT) ch = 'n';
                    else if (r == Rank.ROOK) ch = 'r';
                    else if (r == Rank.QUEEN) ch = 'q';
                    else if (r == Rank.KING) ch = 'k';
                    if (tempPiece.isWhite()) ch = Character.toUpperCase(ch);
                    FEN.append(ch);
                }
            }
            if (c > 0) {
                FEN.append(c);
                c = 0;
            }
            FEN.append("/");
        }
        return String.valueOf(FEN);
    }

    @NonNull
    @Override
    public BoardModel clone() {
        try {
            BoardModel boardModelClone = (BoardModel) super.clone();

            boardModelClone.pieces = new HashSet<>();
            for (Piece piece : pieces) boardModelClone.pieces.add(piece.clone());

            boardModelClone.whiteKing = (King) whiteKing.clone();
            boardModelClone.blackKing = (King) blackKing.clone();

            return boardModelClone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}