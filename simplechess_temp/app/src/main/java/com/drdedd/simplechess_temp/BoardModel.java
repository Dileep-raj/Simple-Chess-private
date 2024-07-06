package com.drdedd.simplechess_temp;

import static com.drdedd.simplechess_temp.data.DataConverter.getPieceChar;
import static com.drdedd.simplechess_temp.data.DataConverter.toCol;
import static com.drdedd.simplechess_temp.data.DataConverter.toRow;
import static com.drdedd.simplechess_temp.data.Regexes.FENPattern;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.pieces.Bishop;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Knight;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;
import com.drdedd.simplechess_temp.pieces.Queen;
import com.drdedd.simplechess_temp.pieces.Rook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Stores pieces location, enPassant square and other board UI data<br>
 */
public class BoardModel implements Serializable, Cloneable {
    /**
     * Set of all the pieces on the board
     */
    public LinkedHashSet<Piece> pieces = new LinkedHashSet<>();
    public final HashMap<String, Integer> resIDs = new HashMap<>();
    private King whiteKing = null, blackKing = null;
    public Pawn enPassantPawn = null;
    public String enPassantSquare = "", fromSquare = "", toSquare = "";
    private final boolean invertBlackSVGs;
    private final HashMap<String, String> unicodes = new HashMap<>();
    private static final String TAG = "BoardModel";

    public BoardModel(Context context, boolean initializeBoard, boolean loadingPGN) {
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);

        DataManager dataManager = new DataManager(context);
        invertBlackSVGs = !loadingPGN && dataManager.invertBlackSVGEnabled();

        Resources res = context.getResources();
        String[] unicodesArray = res.getStringArray(R.array.unicodes);
        unicodes.put("QW", unicodesArray[1]);
        unicodes.put("RW", unicodesArray[2]);
        unicodes.put("BW", unicodesArray[3]);
        unicodes.put("NW", unicodesArray[4]);

        unicodes.put("QB", unicodesArray[7]);
        unicodes.put("RB", unicodesArray[8]);
        unicodes.put("BB", unicodesArray[9]);
        unicodes.put("NB", unicodesArray[10]);

        if (initializeBoard) resetBoard(res);

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
    public void resetBoard(Resources res) {
        int i;
        pieces.clear();
        for (i = 0; i <= 1; i++) {
            addPiece(new Rook(Player.WHITE, 0, i * 7, R.drawable.rw, res.getString(R.string.unicode_rw)));
            addPiece(new Knight(Player.WHITE, 0, 1 + i * 5, R.drawable.nw, res.getString(R.string.unicode_nw)));
            addPiece(new Bishop(Player.WHITE, 0, 2 + i * 3, R.drawable.bw, res.getString(R.string.unicode_bw)));

            if (invertBlackSVGs) {
                addPiece(new Rook(Player.BLACK, 7, i * 7, R.drawable.rbi, res.getString(R.string.unicode_rb)));
                addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, R.drawable.nbi, res.getString(R.string.unicode_nb)));
                addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, R.drawable.bbi, res.getString(R.string.unicode_bb)));
            } else {
                addPiece(new Rook(Player.BLACK, 7, i * 7, R.drawable.rb, res.getString(R.string.unicode_rb)));
                addPiece(new Knight(Player.BLACK, 7, 1 + i * 5, R.drawable.nb, res.getString(R.string.unicode_nb)));
                addPiece(new Bishop(Player.BLACK, 7, 2 + i * 3, R.drawable.bb, res.getString(R.string.unicode_bb)));
            }
        }

//        King and Queen pieces
        addPiece(new King(Player.WHITE, 0, 4, R.drawable.kw, res.getString(R.string.unicode_kw)));
        addPiece(new Queen(Player.WHITE, 0, 3, R.drawable.qw, res.getString(R.string.unicode_qw)));

        if (invertBlackSVGs) {
            addPiece(new King(Player.BLACK, 7, 4, R.drawable.kbi, res.getString(R.string.unicode_kb)));
            addPiece(new Queen(Player.BLACK, 7, 3, R.drawable.qbi, res.getString(R.string.unicode_qb)));
        } else {
            addPiece(new King(Player.BLACK, 7, 4, R.drawable.kb, res.getString(R.string.unicode_kb)));
            addPiece(new Queen(Player.BLACK, 7, 3, R.drawable.qb, res.getString(R.string.unicode_qb)));
        }

//        Pawn pieces
        for (i = 0; i < 8; i++) {
            addPiece(new Pawn(Player.WHITE, 1, i, R.drawable.pw, res.getString(R.string.unicode_pw)));
            if (invertBlackSVGs)
                addPiece(new Pawn(Player.BLACK, 6, i, R.drawable.pbi, res.getString(R.string.unicode_pb)));
            else
                addPiece(new Pawn(Player.BLACK, 6, i, R.drawable.pb, res.getString(R.string.unicode_pb)));
        }
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
            if (!piece.isCaptured() && piece.getCol() == col && piece.getRow() == row) return piece;
        return null;
    }

    /**
     * Search for piece from a specific row
     *
     * @param boardInterface BoardInterface
     * @param player         Player of the piece
     * @param rank           Rank of the piece
     * @param row            Row to be searched
     * @param destRow        Destination row
     * @param destCol        Destination column
     * @return <code>Piece|null</code>
     */
    public Piece searchRow(BoardInterface boardInterface, Player player, Rank rank, int row, int destRow, int destCol) {
        for (Piece piece : pieces) {
            if (piece.getPlayer() != player || piece.isCaptured()) continue;
            if (piece.getRank() == rank && row == piece.getRow() && piece.canMoveTo(boardInterface, destRow, destCol))
                return piece;
        }
        return null;
    }

    /**
     * Search for piece from a specific column
     *
     * @param boardInterface BoardInterface
     * @param player         Player of the piece
     * @param rank           Rank of the piece
     * @param col            Column to be searched
     * @param destRow        Destination row
     * @param destCol        Destination column
     * @return <code>Piece|null</code>
     */
    public Piece searchCol(BoardInterface boardInterface, Player player, Rank rank, int col, int destRow, int destCol) {
        for (Piece piece : pieces) {
            if (piece.getPlayer() != player || piece.isCaptured()) continue;
            if (piece.getRank() == rank && col == piece.getCol() && piece.canMoveTo(boardInterface, destRow, destCol))
                return piece;
        }
        return null;
    }

    /**
     * Search for piece from a specific position
     *
     * @param boardInterface BoardInterface
     * @param player         Player of the piece
     * @param rank           Rank of the piece
     * @param row            Row to be searched
     * @param col            Col to be searched
     * @return <code>Piece|null</code>
     */
    public Piece searchPiece(BoardInterface boardInterface, Player player, Rank rank, int row, int col) {
        for (Piece piece : pieces) {
            if (piece.getPlayer() != player || piece.isCaptured()) continue;
            if (piece.getRank() == rank && piece.canMoveTo(boardInterface, row, col)) return piece;
        }
        return null;
    }

    /**
     * Captures the piece and removes it from board view
     *
     * @param piece <code>Piece</code> to be captured
     */
    public boolean capturePiece(Piece piece) {
        piece.setCaptured(true);
        return piece.isCaptured();
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
        Integer queen = resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString());
        Integer rook = resIDs.get(pawn.getPlayer() + Rank.ROOK.toString());
        Integer bishop = resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString());
        Integer knight = resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString());

        if (rank == Rank.QUEEN && queen != null)
            piece = new Queen(pawn.getPlayer(), row, col, queen, unicodes.get("Q" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.ROOK && rook != null)
            piece = new Rook(pawn.getPlayer(), row, col, rook, unicodes.get("R" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.BISHOP && bishop != null)
            piece = new Bishop(pawn.getPlayer(), row, col, bishop, unicodes.get("B" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.KNIGHT && knight != null)
            piece = new Knight(pawn.getPlayer(), row, col, knight, unicodes.get("N" + pawn.getPlayer().toString().charAt(0)));

        if (piece != null) {
            addPiece(piece);
//            Log.d(TAG, "promote: Promoted " + pawn.getPosition().charAt(1) + " file pawn to " + piece.getRank());
        }
        pieces.remove(pawn);
//        removePiece(pawn);
        return piece;
    }

    /**
     * Converts the <code>BoardModel</code> to <code>String</code> type <br>
     * <ul>
     * <li>UpperCase letter represents White Piece <br></li>
     * <li>LowerCase letter represents Black Piece <br></li>
     * <li>Hyphen (-) represents empty square</li>
     * </ul>
     *
     * @return Standard board notation
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
                else board.append(getPieceChar(tempPiece)).append(' ');
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
            boardModelClone.pieces = new LinkedHashSet<>();
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
     *
     * @return <code>String</code> - FEN of the <code>BoardModel</code>
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">More about FEN</a>
     */
    public String toFEN(BoardInterface boardInterface) {
        String[] fenStrings = toFENStrings(boardInterface);
        return String.format(Locale.ENGLISH, "%s %s %s %s", fenStrings[0], fenStrings[1], fenStrings[2], fenStrings[3]);
    }

    private String[] toFENStrings(BoardInterface boardInterface) {
        String[] FEN = new String[4];

        StringBuilder position = new StringBuilder();
        int i, j, c = 0;
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                Piece tempPiece = pieceAt(i, j);
                if (tempPiece == null) c++;
                else {
                    if (c > 0) {
                        position.append(c);
                        c = 0;
                    }
                    position.append(getPieceChar(tempPiece));
                }
            }
            if (c > 0) {
                position.append(c);
                c = 0;
            }
            if (i != 0) position.append("/");
        }

        FEN[0] = String.valueOf(position);

        if (boardInterface.isWhiteToPlay()) FEN[1] = "w";
        else FEN[1] = "b";

        StringBuilder castleRights = getCastleRights();
        if (castleRights.length() == 0) FEN[2] = "-";
        else FEN[2] = String.valueOf(castleRights);

        if (enPassantSquare.isEmpty()) FEN[3] = "-";
        else FEN[3] = enPassantSquare;

        return FEN;
    }

    private StringBuilder getCastleRights() {
        King whiteKing = getWhiteKing(), blackKing = getBlackKing();
        StringBuilder castleRights = new StringBuilder();
        if (whiteKing != null) {
            if (whiteKing.isNotShortCastled()) castleRights.append('K');
            if (whiteKing.isNotLongCastled()) castleRights.append('Q');
        }
        if (blackKing != null) {
            if (blackKing.isNotShortCastled()) castleRights.append('k');
            if (blackKing.isNotLongCastled()) castleRights.append('q');
        }
        return castleRights;
    }

    /**
     * Parses the valid FEN to BoardModel
     *
     * @param FEN     Valid FEN String
     * @param context Context for resources
     * @return <code>BoardModel|null</code>
     */
    @Nullable
    public static BoardModel parseFEN(String FEN, Context context) {
        Resources res = context.getResources();
        BoardModel boardModel = new BoardModel(context, false, false);
        Matcher matcher = FENPattern.matcher(FEN);
        if (!matcher.find()) {
            Log.d(TAG, "parseFEN: Invalid FEN! FEN didn't match the pattern");
            return null;
        }
        StringTokenizer FENTokens = new StringTokenizer(FEN, " ");
        int tokens = FENTokens.countTokens();
        if (tokens > 6 || tokens < 4) {
            Log.d(TAG, "parseFEN: Invalid FEN! found " + tokens + " fields");
            return null;
        }
        String board = FENTokens.nextToken();
        String nextPlayer = FENTokens.nextToken();
        String castlingAvailability = FENTokens.nextToken();
        String enPassantSquare = FENTokens.nextToken();
        String halfMoveClock = "", fullMoveNumber = "";
        Player activePlayer;
        boolean whiteShortCastle = false, whiteLongCastle = false, blackShortCastle = false, blackLongCastle = false;

        if (FENTokens.hasMoreTokens()) halfMoveClock = FENTokens.nextToken();
        if (FENTokens.hasMoreTokens()) fullMoveNumber = FENTokens.nextToken();

        StringTokenizer boardTokens = new StringTokenizer(board, "/");
        if (boardTokens.countTokens() != 8) {
            Log.d(TAG, "parseFEN: Invalid FEN! found " + tokens + " rows in board field");
            return null;
        }

//        Convert pieces to BoardModel
        int i, row = 7, col;
        while (boardTokens.hasMoreTokens()) {
            String rank = boardTokens.nextToken();
            for (i = 0, col = 0; i < rank.length(); i++) {
                Player player;
                Piece piece;
                char ch = rank.charAt(i);
                if (Character.isDigit(ch)) {
                    col += ch - '0';
                    continue;
                }
                if (i > 8) {
                    Log.d(TAG, "parseFEN: Invalid FEN! found " + col + " columns in rank " + (i + 1));
                    return null;
                }
                player = Character.isUpperCase(ch) ? Player.WHITE : Player.BLACK;
                boolean isWhite = player == Player.WHITE;
                switch (Character.toLowerCase(ch)) {
                    case 'k':
                        piece = new King(player, row, col, isWhite ? R.drawable.kw : R.drawable.kb, res.getString(isWhite ? R.string.unicode_kw : R.string.unicode_kb));
                        break;
                    case 'q':
                        piece = new Queen(player, row, col, isWhite ? R.drawable.qw : R.drawable.qb, res.getString(isWhite ? R.string.unicode_qw : R.string.unicode_qb));
                        break;
                    case 'r':
                        piece = new Rook(player, row, col, isWhite ? R.drawable.rw : R.drawable.rb, res.getString(isWhite ? R.string.unicode_rw : R.string.unicode_rb));
                        break;
                    case 'b':
                        piece = new Bishop(player, row, col, isWhite ? R.drawable.bw : R.drawable.bb, res.getString(isWhite ? R.string.unicode_bw : R.string.unicode_bb));
                        break;
                    case 'n':
                        piece = new Knight(player, row, col, isWhite ? R.drawable.nw : R.drawable.nb, res.getString(isWhite ? R.string.unicode_nw : R.string.unicode_nb));
                        break;
                    case 'p':
                        piece = new Pawn(player, row, col, isWhite ? R.drawable.pw : R.drawable.pb, res.getString(isWhite ? R.string.unicode_pw : R.string.unicode_pb));
                        if (row != (isWhite ? 1 : 6)) piece.setMoved(true);
                        break;
                    default:
                        Log.d(TAG, "parseFEN: Invalid FEN! found invalid character " + ch);
                        return null;
                }
                boardModel.addPiece(piece);
                col++;
            }
            row--;
        }

//        Player to play next move
        activePlayer = nextPlayer.equals("w") ? Player.WHITE : Player.BLACK;

//        Castling availability for each player
        if (!castlingAvailability.equals("-")) {
            whiteShortCastle = castlingAvailability.contains("K");
            whiteLongCastle = castlingAvailability.contains("Q");
            blackShortCastle = castlingAvailability.contains("k");
            blackLongCastle = castlingAvailability.contains("q");
        }

        if (!enPassantSquare.equals("-")) {
            boardModel.enPassantSquare = enPassantSquare;
            boardModel.enPassantPawn = (Pawn) boardModel.pieceAt(toRow(enPassantSquare) - (activePlayer == Player.WHITE ? 1 : -1), toCol(enPassantSquare));
        }
        Log.v(TAG, "parseFEN: Successfully parsed FEN to BoardModel");

//        Log.v(TAG, String.format(Locale.ENGLISH, "\nGiven FEN: %s\nConverted BoardModel:%s\nConverted BoardModel FEN: %s\nPlayer to play: %s\nWhite ShortCastle: %b\tLongCastle: %b\nBlack ShortCastle: %b\tLongCastle: %b\nEnPassantPawn: %s", FEN, boardModel, boardModel.toFENStrings()[0], activePlayer, whiteShortCastle, whiteLongCastle, blackShortCastle, blackLongCastle, boardModel.enPassantPawn == null ? "-" : boardModel.enPassantPawn.getPosition()));
        if (!halfMoveClock.isEmpty() && !fullMoveNumber.isEmpty())
            Log.v(TAG, String.format("parseFEN: Half move clock: %s, Full move count: %s", halfMoveClock, fullMoveNumber));

        Log.i(TAG, "parseFEN: Valid FEN\n");
        return boardModel;
    }

    /**
     * @return List of all captured pieces
     */
    public ArrayList<Piece> getCapturedPieces() {
        ArrayList<Piece> capturedPieces = new ArrayList<>();
        for (Piece piece : pieces) if (piece.isCaptured()) capturedPieces.add(piece);
        return capturedPieces;
    }
}