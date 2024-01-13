package com.drdedd.simplechess_temp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.DataManager;
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
    public HashSet<Piece> pieces = new HashSet<>();
    public final HashMap<String, Integer> resIDs = new HashMap<>();
    private King whiteKing = null, blackKing = null;
    public Pawn enPassantPawn = null;
    private final boolean invertBlackSVGs;
//    private final String TAG = "BoardModel";

    BoardModel(Context context) {
        DataManager dataManager = new DataManager(context);
        invertBlackSVGs = dataManager.invertBlackSVGEnabled();

        resetBoard();
        resIDs.put(Player.WHITE + Rank.QUEEN.toString(), R.drawable.qw);
        resIDs.put(Player.WHITE + Rank.ROOK.toString(), R.drawable.rw);
        resIDs.put(Player.WHITE + Rank.BISHOP.toString(), R.drawable.bw);
        resIDs.put(Player.WHITE + Rank.KNIGHT.toString(), R.drawable.nw);

        if (invertBlackSVGs) {
            resIDs.put(Player.BLACK + Rank.QUEEN.toString(), R.drawable.qbi);
            resIDs.put(Player.BLACK + Rank.ROOK.toString(), R.drawable.rbi);
            resIDs.put(Player.BLACK + Rank.BISHOP.toString(), R.drawable.bbi);
            resIDs.put(Player.BLACK + Rank.KNIGHT.toString(), R.drawable.nbi);
        } else {
            resIDs.put(Player.BLACK + Rank.QUEEN.toString(), R.drawable.qb);
            resIDs.put(Player.BLACK + Rank.ROOK.toString(), R.drawable.rb);
            resIDs.put(Player.BLACK + Rank.BISHOP.toString(), R.drawable.bb);
            resIDs.put(Player.BLACK + Rank.KNIGHT.toString(), R.drawable.nb);
        }
    }

    /**
     * Resets the board to initial state
     */
    public void resetBoard() {
        int i;
        pieces.clear();
        for (i = 0; i <= 1; i++) {
            addPiece(new Rook(Player.WHITE, 0, i * 7, R.drawable.rw));
            addPiece(new Knight(Player.WHITE, 0, 1 + i * 5, R.drawable.nw));
            addPiece(new Bishop(Player.WHITE, 0, 2 + i * 3, R.drawable.bw));

            if (invertBlackSVGs) {
                addPiece(new Rook(Player.BLACK, 7, i * 7, R.drawable.rbi));
                addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, R.drawable.nbi));
                addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, R.drawable.bbi));
            } else {
                addPiece(new Rook(Player.BLACK, 7, i * 7, R.drawable.rb));
                addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, R.drawable.nb));
                addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, R.drawable.bb));
            }
        }

//        King and Queen pieces
        addPiece(new King(Player.WHITE, 0, 4, R.drawable.kw));
        addPiece(new Queen(Player.WHITE, 0, 3, R.drawable.qw));

        if (invertBlackSVGs) {
            addPiece(new King(Player.BLACK, 7, 4, R.drawable.kbi));
            addPiece(new Queen(Player.BLACK, 7, 3, R.drawable.qbi));
        } else {
            addPiece(new King(Player.BLACK, 7, 4, R.drawable.kb));
            addPiece(new Queen(Player.BLACK, 7, 3, R.drawable.qb));
        }

//        Pawn pieces
        for (i = 0; i < 8; i++) {
            addPiece(new Pawn(Player.WHITE, 1, i, R.drawable.pw));
            if (invertBlackSVGs) addPiece(new Pawn(Player.BLACK, 6, i, R.drawable.pbi));
            else addPiece(new Pawn(Player.BLACK, 6, i, R.drawable.pb));
        }
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);
    }

    /**
     * Returns Black king from <code>{@link BoardModel#pieces}</code> set
     *
     * @return <code>{@link King}</code>
     */
    public King getBlackKing() {
        for (Piece piece : pieces)
            if (piece.isKing() && !piece.isWhite()) {
                blackKing = (King) piece;
                break;
            }
        return blackKing;
    }

    /**
     * Returns White king from <code>{@link BoardModel#pieces}</code> set
     *
     * @return <code>{@link  King}</code>
     */
    public King getWhiteKing() {
        for (Piece piece : pieces)
            if (piece.isKing() && piece.isWhite()) {
                whiteKing = (King) piece;
                break;
            }
        return whiteKing;
    }

    /**
     * Returns piece at a given position
     *
     * @param row Row number
     * @param col Column number
     * @return <code>{@link Piece}|null</code>
     */
    public Piece pieceAt(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        for (Piece piece : pieces)
            if (piece.getCol() == col && piece.getRow() == row) return piece;
        return null;
    }

    /**
     * Removes the piece from <code>{@link BoardModel#pieces}</code> set
     *
     * @param piece <code>Piece</code> to be removed
     */
    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    /**
     * Adds the given piece into <code>{@link BoardModel#pieces}</code> set
     *
     * @param piece <code>Piece</code> to be added
     */
    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    /**
     * Promotes a pawn to higher rank on reaching last rank
     *
     * @param pawn <code>Pawn</code> to be promoted
     * @param rank <code>Queen|Rook|Knight|Bishop</code>
     * @param row  Row of the pawn
     * @param col  Column of the pawn
     * @return <code>{@link Piece}</code> - Promoted piece
     */
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

    /**
     * Converts the <code>BoardModel</code> to <code>String</code> type <br>
     * <ul>
     * <li>UpperCase letter represents White Piece <br></li>
     * <li>LowerCase letter Piece represents Black Piece <br></li>
     * <li>Hyphen (-) represents empty square</li>
     * </ul>
     *
     * @return <code>String</code>
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder board = new StringBuilder("\n");
        int i, j;
        for (i = 7; i >= 0; i--) {
            board.append(' ').append(i + 1).append(' ');
            for (j = 0; j < 8; j++) {
                Piece tempPiece = pieceAt(i, j);
                if (tempPiece == null) board.append("- ");
                else {
                    char ch = tempPiece.getRank().toString().charAt(0);
                    if (tempPiece.getRank() == Rank.KNIGHT) ch = 'N';
                    if (!tempPiece.isWhite()) ch = Character.toLowerCase(ch);
                    board.append(ch).append(' ');
                }
            }
            board.append("\n");
        }
        board.append("   a b c d e f g h");
        return String.valueOf(board);
    }

    @NonNull
    @Override
    public BoardModel clone() {
        try {
            BoardModel boardModelClone = (BoardModel) super.clone();
            boardModelClone.pieces = new HashSet<>();
            for (Piece piece : pieces) boardModelClone.pieces.add(piece.clone());

            if (enPassantPawn != null) boardModelClone.enPassantPawn = (Pawn) enPassantPawn.clone();
            else boardModelClone.enPassantPawn = null;

            return boardModelClone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Converts current position to FEN Notation <br>
     * @return <code>String</code> - FEN of the <code>BoardModel</code>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">More about FEN</a>
     */
    public String toFEN() {
        StringBuilder FEN = new StringBuilder();
        int i, j, c = 0;
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                Piece tempPiece = pieceAt(i, j);
                if (tempPiece == null) c++;
                else {
                    if (c > 0) {
                        FEN.append(c);
                        c = 0;
                    }
                    char ch = tempPiece.getRank().toString().charAt(0);
                    if (tempPiece.getRank() == Rank.KNIGHT) ch = 'N';
                    if (!tempPiece.isWhite()) ch = Character.toLowerCase(ch);
                    FEN.append(ch);
                }
            }
            if (c > 0) {
                FEN.append(c);
                c = 0;
            }
            FEN.append("/");
        }

        if (GameActivity.getGameState() == ChessState.WHITE_TO_PLAY) FEN.append(" w ");
        else if (GameActivity.getGameState() == ChessState.BLACK_TO_PLAY) FEN.append(" b ");

        King whiteKing = getWhiteKing(), blackKing = getBlackKing();
        StringBuilder castleRights = new StringBuilder();
        if (whiteKing != null) {
//            Log.d(TAG, "toFEN: White King Short Castled: " + whiteKing.isShortCastled() + " Long Castled: " + whiteKing.isLongCastled());
            if (whiteKing.isNotShortCastled()) castleRights.append('K');
            if (whiteKing.isNotLongCastled()) castleRights.append('Q');
        }
        if (blackKing != null) {
//            Log.d(TAG, "toFEN: Black King Short Castled: " + blackKing.isShortCastled() + " Long Castled: " + blackKing.isLongCastled());
            if (blackKing.isNotShortCastled()) castleRights.append('k');
            if (blackKing.isNotLongCastled()) castleRights.append('q');
        }
        if (castleRights.length() == 0) FEN.append(" - ");
        else FEN.append(castleRights);

        if (enPassantPawn != null)
            FEN.append(' ').append(enPassantPawn.getPosition().charAt(1)).append(enPassantPawn.getRow() + 1 - enPassantPawn.direction);
        return String.valueOf(FEN);
    }
}