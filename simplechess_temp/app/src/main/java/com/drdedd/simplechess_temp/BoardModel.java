package com.drdedd.simplechess_temp;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.fragments.game.GameFragment;
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
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BoardModel implements Serializable, Cloneable {
    /**
     * Set of all the pieces on the board
     */
    public HashSet<Piece> pieces = new HashSet<>();
    public final HashMap<String, Integer> resIDs = new HashMap<>();
    private King whiteKing = null, blackKing = null;
    public Pawn enPassantPawn = null;
    public String enPassantSquare;
    private final boolean invertBlackSVGs, unicodeEnabled;
    private final HashMap<String, String> unicodes = new HashMap<>();
    private static final String TAG = "BoardModel";

    public BoardModel(Context context, boolean initializeBoard) {
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);

        DataManager dataManager = new DataManager(context);
        invertBlackSVGs = dataManager.invertBlackSVGEnabled();
        unicodeEnabled = dataManager.getUnicode();

        Resources res = context.getResources();
        String[] unicodesArray = res.getStringArray(R.array.unicodes);
        unicodes.put("KW", unicodesArray[0]);
        unicodes.put("QW", unicodesArray[1]);
        unicodes.put("RW", unicodesArray[2]);
        unicodes.put("BW", unicodesArray[3]);
        unicodes.put("NW", unicodesArray[4]);
        unicodes.put("PW", unicodesArray[5]);

        unicodes.put("KB", unicodesArray[6]);
        unicodes.put("QB", unicodesArray[7]);
        unicodes.put("RB", unicodesArray[8]);
        unicodes.put("BB", unicodesArray[9]);
        unicodes.put("NB", unicodesArray[10]);
        unicodes.put("PB", unicodesArray[11]);

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
            piece = new Queen(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString()), unicodes.get("Q" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.ROOK)
            piece = new Rook(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.ROOK.toString()), unicodes.get("R" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.BISHOP)
            piece = new Bishop(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString()), unicodes.get("B" + pawn.getPlayer().toString().charAt(0)));
        if (rank == Rank.KNIGHT)
            piece = new Knight(pawn.getPlayer(), row, col, resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString()), unicodes.get("N" + pawn.getPlayer().toString().charAt(0)));

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
                else if (unicodeEnabled) board.append(tempPiece).append(' ');
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
     *
     * @return <code>String</code> - FEN of the <code>BoardModel</code>
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">More about FEN</a>
     */
    public String toFEN() {
        String[] fenStrings = toFENStrings();
        return fenStrings[0] + " " + fenStrings[1] + " " + fenStrings[2] + " " + fenStrings[3];
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String[] toFENStrings() {
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

        if (GameFragment.getGameState() == ChessState.WHITE_TO_PLAY) FEN[1] = " w ";
        else if (GameFragment.getGameState() == ChessState.BLACK_TO_PLAY) FEN[1] = " b ";

        StringBuilder castleRights = getCastleRights();
        if (castleRights.length() == 0) FEN[2] = " - ";
        else FEN[2] = String.valueOf(castleRights);

        if (enPassantPawn != null)
            FEN[3] = String.valueOf(GameFragment.colToChar(enPassantPawn.getCol())) + (enPassantPawn.getRow() + 1 - enPassantPawn.direction);
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

    private char getPieceChar(Piece piece) {
        char ch;
        if (piece.getRank() == Rank.KNIGHT) ch = 'N';
        else ch = piece.getRank().toString().charAt(0);
        if (!piece.isWhite()) ch = Character.toLowerCase(ch);
        return ch;
    }

    public static BoardModel parseFEN(String FEN, Context context) {
        Resources res = context.getResources();
        BoardModel boardModel = new BoardModel(context, false);
        Pattern pattern = Pattern.compile("[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+/[kqbnrp1-8]+ [wb] [-kq]+ (-|[a-h][1-8]) [0-9]+ [0-9]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(FEN);
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
        String halfMoveClock, fullMoveNumber;

//        Log.d(TAG, String.format(Locale.ENGLISH, "parseFEN: Tokens: %s %s %s %s", board, nextPlayer, castlingAvailability, enPassantSquare));

        Player playerToPlay;
        boolean whiteShortCastle = false, whiteLongCastle = false, blackShortCastle = false, blackLongCastle = false;

        if (FENTokens.hasMoreTokens()) halfMoveClock = FENTokens.nextToken();
        if (FENTokens.hasMoreTokens()) fullMoveNumber = FENTokens.nextToken();

        StringTokenizer boardTokens = new StringTokenizer(board, "/");
        if (boardTokens.countTokens() != 8) {
            Log.d(TAG, "parseFEN: Invalid FEN! found " + tokens + " rows in board field");
            return null;
        }

//        Convert pieces to BoardModel
        int i, rowNo = 7, c;
        while (boardTokens.hasMoreTokens()) {
            String row = boardTokens.nextToken();
            for (i = 0, c = 0; i < row.length(); i++) {
                Player player;
                Piece piece;
                char ch = row.charAt(i);
                if (Character.isDigit(ch)) {
                    c += ch - '0';
                    continue;
                }
                if (i > 8) {
                    Log.d(TAG, "parseFEN: Invalid FEN! found " + c + " columns in rank " + (i + 1));
                    return null;
                }
                player = Character.isUpperCase(ch) ? Player.WHITE : Player.BLACK;
                boolean isWhite = player == Player.WHITE;
                switch (Character.toLowerCase(ch)) {
                    case 'k':
                        piece = new King(player, rowNo, c, isWhite ? R.drawable.kw : R.drawable.kb, res.getString(isWhite ? R.string.unicode_kw : R.string.unicode_kb));
                        break;
                    case 'q':
                        piece = new Queen(player, rowNo, c, isWhite ? R.drawable.qw : R.drawable.qb, res.getString(isWhite ? R.string.unicode_qw : R.string.unicode_qb));
                        break;
                    case 'r':
                        piece = new Rook(player, rowNo, c, isWhite ? R.drawable.rw : R.drawable.rb, res.getString(isWhite ? R.string.unicode_rw : R.string.unicode_rb));
                        break;
                    case 'b':
                        piece = new Bishop(player, rowNo, c, isWhite ? R.drawable.bw : R.drawable.bb, res.getString(isWhite ? R.string.unicode_bw : R.string.unicode_bb));
                        break;
                    case 'n':
                        piece = new Knight(player, rowNo, c, isWhite ? R.drawable.nw : R.drawable.nb, res.getString(isWhite ? R.string.unicode_nw : R.string.unicode_nb));
                        break;
                    case 'p':
                        piece = new Pawn(player, rowNo, c, isWhite ? R.drawable.pw : R.drawable.pb, res.getString(isWhite ? R.string.unicode_pw : R.string.unicode_pb));
                        break;
                    default:
                        Log.d(TAG, "parseFEN: Invalid FEN! found invalid character " + ch);
                        return null;
                }
                boardModel.addPiece(piece);
//                Log.d(TAG, String.format(Locale.ENGLISH, "parseFEN: %s %s at %s (%d,%d)", piece.getPlayer(), piece.getRank(), piece.getPosition(), rowNo, i));
                c++;
            }
            rowNo--;
        }

//        Player to play next move
        playerToPlay = nextPlayer.equals("w") ? Player.WHITE : Player.BLACK;

//        Castling availability for each player
        if (!castlingAvailability.equals("-")) {
            whiteShortCastle = castlingAvailability.contains("K");
            whiteLongCastle = castlingAvailability.contains("Q");
            blackShortCastle = castlingAvailability.contains("k");
            blackLongCastle = castlingAvailability.contains("q");
        }

        if (!enPassantSquare.equals("-")) {
            boardModel.enPassantSquare = enPassantSquare;
            boardModel.enPassantPawn = (Pawn) boardModel.pieceAt(GameFragment.toRow(enPassantSquare) - (playerToPlay == Player.WHITE ? 1 : -1), GameFragment.toCol(enPassantSquare));
        }
        Log.d(TAG, "parseFEN: Successfully parsed FEN to BoardModel");

        Log.d(TAG, String.format(Locale.ENGLISH, "\nGiven FEN: %s\nConverted BoardModel:%s\nConverted BoardModel FEN: %s\nPlayer to play: %s\nWhite ShortCastle: %b\tLongCastle: %b\nBlack ShortCastle: %b\tLongCastle: %b\nEnPassantPawn: %s", FEN, boardModel, boardModel.toFENStrings()[0], playerToPlay, whiteShortCastle, whiteLongCastle, blackShortCastle, blackLongCastle, boardModel.enPassantPawn == null ? "-" : boardModel.enPassantPawn.getPosition()));
        Log.d(TAG, "parseFEN: Valid FEN\n");
        return boardModel;
    }
}