package com.drdedd.simplechess_temp;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplechess_temp.data.DataConverter.opponentPlayer;
import static com.drdedd.simplechess_temp.data.DataConverter.toCol;
import static com.drdedd.simplechess_temp.data.DataConverter.toNotation;
import static com.drdedd.simplechess_temp.data.DataConverter.toRow;
import static com.drdedd.simplechess_temp.data.Regexes.activePlayerPattern;
import static com.drdedd.simplechess_temp.fragments.HomeFragment.printTime;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.data.DataManager;
import com.drdedd.simplechess_temp.dialogs.PromoteDialog;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.interfaces.GameFragmentInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;
import com.drdedd.simplechess_temp.views.ChessBoard;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

/**
 * GameLogic to play moves and parse game
 * {@inheritDoc}
 */
public class GameLogic implements BoardInterface {
    private static final String TAG = "GameLogic";
    private final Context context;
    private final DataManager dataManager;
    private final ChessBoard chessBoard;
    private final GameFragmentInterface gameFragmentInterface;
    private final String FEN;
    private final boolean newGame;
    private int count;
    private boolean vibrationEnabled, loadingPGN, sound, animate, gameTerminated, whiteToPlay;
    private String white = "White", black = "Black", app, date, fromSquare, toSquare, termination;
    private PGN pgn;
    private BoardModel boardModel = null;
    private ChessState gameState;
    private Stack<BoardModel> boardModelStack;
    private HashMap<Piece, HashSet<Integer>> legalMoves;
    private Stack<String> FENs;
    private LinkedList<String> moves;
    public HashMap<String, String> tagsMap;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    /**
     * GameLogic for normal game setup
     *
     * @param gameFragmentInterface Game fragment interface reference
     * @param context               Context of the fragment
     * @param chessBoard            ChessBoard view
     * @param newGame               Start new game or resume saved game
     */
    public GameLogic(GameFragmentInterface gameFragmentInterface, Context context, ChessBoard chessBoard, boolean newGame) {
        this.context = context;
        dataManager = new DataManager(context);
        this.gameFragmentInterface = gameFragmentInterface;
        this.chessBoard = chessBoard;
        this.newGame = newGame;
        FEN = "";
        chessBoard.setData(this, false);
        initializeData();
        if (newGame) reset();
        updateAll();
    }

    /**
     * GameLogic with a starting position
     *
     * @param gameFragmentInterface Game fragment reference
     * @param context               Context of fragment
     * @param chessBoard            ChessBoard view
     * @param FEN                   FEN of the starting position
     */
    public GameLogic(GameFragmentInterface gameFragmentInterface, Context context, ChessBoard chessBoard, String FEN) {
        newGame = false;
        this.context = context;
        dataManager = new DataManager(context);
        this.gameFragmentInterface = gameFragmentInterface;
        this.chessBoard = chessBoard;
        this.FEN = FEN;
        chessBoard.setData(this, false);
        initializeData();
        Log.d(TAG, "GameLogic: Loading FEN: " + FEN);
        reset();
        updateAll();
    }

    /**
     * GameLogic to parse and validate PGN moves
     *
     * @param context               Context of the Fragment
     * @param tagsMap               Map of tags in the PGN
     * @param moves                 List of moves in the PGN
     * @param commentsMap           Map of comments in the PGN
     * @param moveAnnotationMap     Map of annotations for moves in the PGN
     * @param alternateMoveSequence Map of alternate move sequences for moves in the PGN
     */
    public GameLogic(Context context, HashMap<String, String> tagsMap, LinkedList<String> moves, LinkedHashMap<Integer, String> commentsMap, LinkedHashMap<Integer, String> moveAnnotationMap, LinkedHashMap<Integer, String> alternateMoveSequence, LinkedHashMap<Integer, String> evalMap) {
        this.context = context;
        this.tagsMap = tagsMap;
        this.moves = moves;
        dataManager = new DataManager(context);
        loadingPGN = true;
        gameFragmentInterface = null;
        chessBoard = null;

        initializeData();

        white = tagsMap.get(PGN.TAG_WHITE);
        black = tagsMap.get(PGN.TAG_BLACK);
        date = tagsMap.get(PGN.TAG_DATE);
        pgn.setWhiteBlack(white, black);

        FEN = tagsMap.getOrDefault(PGN.TAG_FEN, "");
        newGame = !(FEN != null && FEN.isEmpty());
        reset();

        pgn.setCommentsMap(commentsMap);
        pgn.setMoveAnnotationMap(moveAnnotationMap);
        pgn.setAlternateMoveSequence(alternateMoveSequence);
        pgn.setEvalMap(evalMap);
    }

    /**
     * Initializes game data and objects: MediaPlayer, Vibrator, PGN, BoardModel
     */
    @SuppressWarnings("unchecked")
    private void initializeData() {
        mediaPlayer = MediaPlayer.create(context, R.raw.move_sound);
        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        gameTerminated = false;

        vibrationEnabled = dataManager.getVibration();
        sound = dataManager.getSound();
        animate = dataManager.getAnimation();

        SimpleDateFormat pgnDate = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);

        white = dataManager.getWhite();
        black = dataManager.getBlack();

        app = context.getResources().getString(R.string.app_name);
        date = pgnDate.format(new Date());


        if (!newGame) {
            boardModel = (BoardModel) dataManager.readObject(DataManager.BOARD_FILE);
            pgn = (PGN) dataManager.readObject(DataManager.PGN_FILE);
            boardModelStack = (Stack<BoardModel>) dataManager.readObject(DataManager.STACK_FILE);
            FENs = (Stack<String>) dataManager.readObject(DataManager.FENS_LIST_FILE);
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel(context, true, loadingPGN);
            boardModelStack = new Stack<>();
            FENs = new Stack<>();
            boardModelStack.push(boardModel);
            FENs.push(boardModel.toFEN(this));
            pgn = new PGN(app, white, black, date, true);
        }

        gameState = ChessState.ONGOING;
        whiteToPlay = pgn.isWhiteToPlay();
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names
    }

    public void reset() {
        if (chessBoard != null) chessBoard.clearSelection();
        gameTerminated = false;
        whiteToPlay = true;

        if (FEN.isEmpty()) {
            boardModel = new BoardModel(context, true, loadingPGN);
            pgn = new PGN(app, white, black, date, true);
        } else {
            long start = System.nanoTime();
            boardModel = BoardModel.parseFEN(FEN, context);

            Matcher player = activePlayerPattern.matcher(FEN);
            if (player.find()) whiteToPlay = player.group().trim().equals("w");
            long end = System.nanoTime();
            printTime(TAG, "parsing FEN", end - start, FEN.length());
            pgn = new PGN(app, white, black, date, whiteToPlay, FEN);
        }
        boardModelStack = new Stack<>();
        FENs = new Stack<>();
        fromSquare = "";
        toSquare = "";
        pushToStack();
    }

    /**
     * Parses and converts PGN to game objects
     *
     * @return <code>true|false</code> - Whether all PGN moves are valid
     */
    public boolean parsePGN() {
        char ch;
        int i, startRow, startCol, destRow, destCol;
        boolean promotion;
        Rank rank = null, promotionRank;
        Piece piece;
        Player player;

        for (String move : moves) {
            move = move.trim();
            Log.v(TAG, "parsePGN: Move: " + move);
            startRow = -1;
            startCol = -1;
            destRow = -1;
            destCol = -1;
            promotion = false;
            promotionRank = null;
            piece = null;
            player = playerToPlay();

            try {
                if (move.equals(PGN.SHORT_CASTLE)) {
                    King king = whiteToPlay ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canShortCastle(this))
                        move(king.getRow(), king.getCol(), king.getRow(), king.getCol() + 2);
                    continue;
                } else if (move.equals(PGN.LONG_CASTLE)) {
                    King king = whiteToPlay ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canLongCastle(this))
                        move(king.getRow(), king.getCol(), king.getRow(), king.getCol() - 2);
                    continue;
                }

                ch = move.charAt(0);
                if (Character.isLetter(ch)) switch (ch) {
                    case 'K':
                        rank = Rank.KING;
                        break;
                    case 'Q':
                        rank = Rank.QUEEN;
                        break;
                    case 'R':
                        rank = Rank.ROOK;
                        break;
                    case 'N':
                        rank = Rank.KNIGHT;
                        break;
                    case 'B':
                        rank = Rank.BISHOP;
                        break;
                    case 'P':
                    default:
                        rank = Rank.PAWN;
                }
                label:
                for (i = 0; i < move.length(); i++) {
                    ch = move.charAt(i);
                    switch (ch) {
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                        case 'g':
                        case 'h':
                            if (destCol != -1) startCol = destCol;
                            destCol = ch - 'a';
                            break;

                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                            if (destRow != -1) startRow = destRow;
                            destRow = ch - '1';
                            break;

                        case '=':
                            promotion = true;
                            ch = move.charAt(i + 1);
                            Log.v(TAG, "parsePGN: Promotion");
                            switch (ch) {
                                case 'Q':
                                    promotionRank = Rank.QUEEN;
                                    break;
                                case 'R':
                                    promotionRank = Rank.ROOK;
                                    break;
                                case 'N':
                                    promotionRank = Rank.KNIGHT;
                                    break;
                                case 'B':
                                    promotionRank = Rank.BISHOP;
                                    break;
                            }
                            Log.v(TAG, "parsePGN: Promotion rank: " + promotionRank);
                            if (promotionRank == null) return false;
                            break label;

                        case 'Q':
                        case 'R':
                        case 'N':
                        case 'B':
                            if (destCol != -1 && destRow != -1) {
                                promotion = true;
                                Log.v(TAG, "parsePGN: Promotion");
                                switch (ch) {
                                    case 'Q':
                                        promotionRank = Rank.QUEEN;
                                        break;
                                    case 'R':
                                        promotionRank = Rank.ROOK;
                                        break;
                                    case 'N':
                                        promotionRank = Rank.KNIGHT;
                                        break;
                                    case 'B':
                                        promotionRank = Rank.BISHOP;
                                        break;
                                }
                                Log.v(TAG, "parsePGN: Promotion rank: " + promotionRank);
                                break label;
                            }

                        case 'K':
                        case 'P':
                        case 'x':
                        case '+':
                        case '#':
                            break;
                    }
                }

                if (startRow != -1 && startCol != -1) {
                    piece = boardModel.pieceAt(startRow, startCol);
                    Log.d(TAG, String.format("parsePGN: piece at %s piece: %s", toNotation(startRow, startCol), piece));
                } else if (startCol != -1) {
                    piece = boardModel.searchCol(this, player, rank, startCol, destRow, destCol);
                    Log.d(TAG, String.format("parsePGN: searched col: %d piece:%s", startCol, piece));
                } else if (startRow != -1) {
                    piece = boardModel.searchRow(this, player, rank, startRow, destRow, destCol);
                    Log.d(TAG, String.format("parsePGN: searched row: %d piece: %s", startRow, piece));
                }

                if (piece == null) {
                    piece = boardModel.searchPiece(this, player, rank, destRow, destCol);
                    Log.d(TAG, "parsePGN: piece searched");
                }

                if (piece != null && promotion) {
                    Pawn pawn = (Pawn) piece;
                    if (promote(pawn, destRow, destCol, startRow, startCol, promotionRank)) {
                        Log.d(TAG, String.format("parsePGN: Promoted to %s at %s", promotionRank, toNotation(destRow, destCol)));
                        continue;
                    }
                }

                if (piece != null) {
                    if (move(piece.getRow(), piece.getCol(), destRow, destCol))
                        Log.d(TAG, String.format("parsePGN: Move success %s", move));
                    else {
                        Log.d(TAG, "parsePGN: Second search!");
                        LinkedHashSet<Piece> pieces = boardModel.pieces, tempPieces = new LinkedHashSet<>();
                        for (Piece tempPiece : pieces)
                            if (tempPiece.getPlayer() == player && tempPiece.getRank() == rank) {
                                if (startRow != -1 && tempPiece.getRow() == startRow)
                                    tempPieces.add(tempPiece);
                                else if (startCol != -1 && tempPiece.getCol() == startCol)
                                    tempPieces.add(tempPiece);
                                else tempPieces.add(tempPiece);
                            }

                        for (Piece tempPiece : tempPieces)
                            if (getLegalMoves().containsKey(tempPiece) && Objects.requireNonNull(getLegalMoves().get(tempPiece)).contains(destCol + destRow * 8))
                                piece = tempPiece;

                        if (piece != null && move(piece.getRow(), piece.getCol(), destRow, destCol))
                            Log.d(TAG, "parsePGN: Move success after 2nd search! " + move);
                        else {
                            StringBuilder legalMoves = new StringBuilder();
                            HashSet<Integer> pieceLegalMoves = getLegalMoves().get(piece);
                            if (pieceLegalMoves != null) for (int legalMove : pieceLegalMoves)
                                legalMoves.append(toNotation(legalMove)).append(' ');
                            Log.d(TAG, String.format("parsePGN: Move failed: %s%nPiece: %s%nLegalMoves: %s", move, piece, legalMoves));
                            return false;
                        }
                    }
                } else {
                    Log.d(TAG, String.format("parsePGN: Move invalid! Piece not found! %s %s (%d,%d) -> %s move: %s", player, rank, startRow, startCol, toNotation(destRow, destCol), move));
//                    Toast.makeText(context, "Invalid move " + move, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (Exception e) {
//                Toast.makeText(context, "Error occurred after move " + move, Toast.LENGTH_LONG).show();
                Log.e(TAG, "parsePGN: Error occurred after move " + move, e);
                return false;
            }
        }

        Set<String> tags = tagsMap.keySet();
        for (String tag : tags) {
            String value = tagsMap.get(tag);
            if (value != null && !value.isEmpty()) pgn.addTag(tag, value);
            Log.d(TAG, String.format("parsePGN: Tag: [%s \"%s\"]", tag, value));
        }

//        loadGame();
        return true;
    }

    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    @Override
    public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameTerminated) return false;
        if (dataManager.cheatModeEnabled()) {
            Piece movingPiece = boardModel.pieceAt(fromRow, fromCol);
            if (movingPiece != null) {
                Piece toPiece = boardModel.pieceAt(toRow, toCol);
                if (toPiece != null) {
                    if (toPiece.getPlayer() == movingPiece.getPlayer()) return false;
                    else boardModel.capturePiece(toPiece);
                }
                movingPiece.moveTo(toRow, toCol);
                return true;
            }
            return false;
        } else {
            Piece movingPiece = pieceAt(fromRow, fromCol);
            if (movingPiece == null) return false;

            if (isPieceToPlay(movingPiece)) if (legalMoves.get(movingPiece) != null) {
                HashSet<Integer> pieceLegalMoves = legalMoves.get(movingPiece);
                if (pieceLegalMoves != null)
                    if (!pieceLegalMoves.contains(toCol + toRow * 8)) return false;
            }
            boolean result = makeMove(fromRow, fromCol, toRow, toCol);
            if (result) {
                if (!loadingPGN && sound) mediaPlayer.start();
                boardModel.fromSquare = toNotation(fromRow, fromCol);
                boardModel.toSquare = toNotation(toRow, toCol);

                boardModel.enPassantPawn = null;
                boardModel.enPassantSquare = "";

                if (movingPiece.getRank() == Rank.PAWN && Math.abs(fromRow - toRow) == 2) {
                    Pawn enPassantPawn = (Pawn) movingPiece;
                    boardModel.enPassantPawn = enPassantPawn;
                    boardModel.enPassantSquare = toNotation(enPassantPawn.getRow() - enPassantPawn.direction, enPassantPawn.getCol());
                    //Log.d(TAG, "move: EnPassantPawn: " + boardModel.enPassantPawn.getPosition() + " EnPassantSquare: " + boardModel.enPassantSquare);
                }
                fromSquare = toNotation(fromRow, fromCol);
                toSquare = toNotation(toRow, toCol);
                toggleGameState();
                pushToStack();
                if (playerToPlay().isInCheck()) printLegalMoves();
            }
            return result;
        }
    }

    /**
     * Checks for move validity and performs move
     *
     * @param fromRow Starting row of the piece
     * @param fromCol Starting column of the piece
     * @param toRow   Ending row of the piece
     * @param toCol   Ending column of the piece
     * @return Move result
     */
    private boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (isGameTerminated()) return false;

        Piece movingPiece = pieceAt(fromRow, fromCol);
        if (movingPiece == null || fromRow == toRow && fromCol == toCol || toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7 || !isPieceToPlay(movingPiece))
            return false;

        Piece toPiece = pieceAt(toRow, toCol);
        if (toPiece != null) if (toPiece.isKing()) return false;
        else if (movingPiece.getPlayer() != toPiece.getPlayer() && movingPiece.canCapture(this, toPiece)) {
            if (movingPiece.getRank() == Rank.PAWN) {
                Pawn pawn = (Pawn) movingPiece;
                if (pawn.canPromote()) {
                    promote(pawn, toRow, toCol, fromRow, fromCol);
                    //Log.d(TAG, "makeMove: Pawn promotion");
                    return false;
                }
            }
            movingPiece.moveTo(toRow, toCol);
            capturePiece(toPiece);
            addToPGN(movingPiece, PGN.CAPTURE, fromRow, fromCol);
            return true;
        }
        if (toPiece == null) {
            if (movingPiece.getRank() == Rank.KING) {
                King king = (King) movingPiece;
                if (!king.isCastled() && king.canMoveTo(this, toRow, toCol)) {
                    if (toCol - fromCol == -2 && king.canLongCastle(this)) {
                        king.longCastle(this);
                        addToPGN(movingPiece, PGN.LONG_CASTLE, fromRow, fromCol);
                        return true;
                    }
                    if (toCol - fromCol == 2 && king.canShortCastle(this)) {
                        king.shortCastle(this);
                        addToPGN(movingPiece, PGN.SHORT_CASTLE, fromRow, fromCol);
                        return true;
                    }
                }
            }
            if (movingPiece.canMoveTo(this, toRow, toCol)) {
                if (movingPiece.getRank() == Rank.PAWN) {
                    Pawn pawn = (Pawn) movingPiece;
                    if (pawn.canCaptureEnPassant(this))
                        if (getBoardModel().enPassantSquare.equals(toNotation(toRow, toCol)))
                            if (capturePiece(pieceAt(toRow - pawn.direction, toCol))) {
                                //Log.d(TAG, "makeMove: EnPassant Capture");
                                movingPiece.moveTo(toRow, toCol);
                                addToPGN(pawn, PGN.CAPTURE, fromRow, fromCol);
                                return true;
                            }
                    if (pawn.canPromote()) {
                        promote(pawn, toRow, toCol, fromRow, fromCol);
                        //Log.d(TAG, "makeMove: Pawn promotion");
                        return false;
                    }
                }
                movingPiece.moveTo(toRow, toCol);
                addToPGN(movingPiece, "", fromRow, fromCol);
                return true;
            }
        }
        //Log.d(TAG, "makeMove: Illegal move");
        return false;   //Default return false
    }

    /**
     * Saves game data objects after each move
     */
    private void saveGame() {
        if (gameTerminated || loadingPGN) return;
        dataManager.saveData(boardModel, pgn, boardModelStack, FENs);
    }

    @Override
    public void addToPGN(Piece piece, String move, int fromRow, int fromCol) {
        String position, capture = "";
        StringBuilder moveStringBuilder = new StringBuilder(piece.getPosition());
        position = toNotation(fromRow, fromCol);
        if (move.contains(PGN.CAPTURE)) capture = "x";
        moveStringBuilder.insert(1, position + capture);

        if (move.contains(PGN.PROMOTE)) {
            moveStringBuilder.append('=').append(piece.getRankChar());
            moveStringBuilder.deleteCharAt(0);
        }

        if (move.equals(PGN.LONG_CASTLE) || move.equals(PGN.SHORT_CASTLE))
            moveStringBuilder = new StringBuilder(move);

        pgn.addToPGN(piece, moveStringBuilder.toString());
    }

    @Override
    public boolean capturePiece(Piece piece) {
        return boardModel.capturePiece(piece);
    }

    @Override
    public void promote(Pawn pawn, int row, int col, int fromRow, int fromCol) {
        PromoteDialog promoteDialog = new PromoteDialog(context);
        promoteDialog.show();

//        Set image buttons as respective color pieces
        Integer queenResID = boardModel.resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString());
        Integer rookResID = boardModel.resIDs.get(pawn.getPlayer() + Rank.ROOK.toString());
        Integer bishopResID = boardModel.resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString());
        Integer knightResID = boardModel.resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString());
        if (queenResID != null)
            promoteDialog.findViewById(R.id.promote_to_queen).setBackgroundResource(queenResID);
        if (rookResID != null)
            promoteDialog.findViewById(R.id.promote_to_rook).setBackgroundResource(rookResID);
        if (bishopResID != null)
            promoteDialog.findViewById(R.id.promote_to_bishop).setBackgroundResource(bishopResID);
        if (knightResID != null)
            promoteDialog.findViewById(R.id.promote_to_knight).setBackgroundResource(knightResID);

//        Invalidate chess board to show new promoted piece
        promoteDialog.setOnDismissListener(dialogInterface -> {
            Piece tempPiece = pieceAt(row, col);
            Rank rank = promoteDialog.getRank();
            Piece promotedPiece = boardModel.promote(pawn, rank, row, col);
            if (tempPiece != null) {
                if (tempPiece.getPlayer() != promotedPiece.getPlayer()) {
                    capturePiece(tempPiece);
                    addToPGN(promotedPiece, PGN.PROMOTE + PGN.CAPTURE, fromRow, fromCol);
                }
            } else addToPGN(promotedPiece, PGN.PROMOTE, fromRow, fromCol);
            Log.v(TAG, String.format("promote: Promoted to %s %s->%s", rank, toNotation(fromRow, fromCol), toNotation(row, col)));
//            fromSquare = "";
//            toSquare = "";
            fromSquare = toNotation(fromRow, fromCol);
            toSquare = toNotation(row, col);
            toggleGameState();
            pushToStack();
        });
    }

    /**
     * Promotion of pawn to a higher rank
     *
     * @param pawn    Pawn to be promoted
     * @param row     Row of the promotion square
     * @param col     Column of the promotion square
     * @param fromRow Starting row of the pawn
     * @param fromCol Starting column of the pawn
     * @param rank    Rank to be promoted
     * @return <code>true|false</code> - Promotion result
     */
    private boolean promote(Pawn pawn, int row, int col, int fromRow, int fromCol, Rank rank) {
        boolean promoted = false;
        Piece tempPiece = pieceAt(row, col);
        Piece promotedPiece = boardModel.promote(pawn, rank, row, col);
        if (tempPiece != null) {
            if (tempPiece.getPlayer() != promotedPiece.getPlayer()) {
                capturePiece(tempPiece);
                addToPGN(promotedPiece, PGN.PROMOTE + PGN.CAPTURE, fromRow, fromCol);
                promoted = true;
            }
        } else {
            addToPGN(promotedPiece, PGN.PROMOTE, fromRow, fromCol);
            promoted = true;
        }
        if (promoted) {
            fromSquare = toNotation(fromRow, fromCol);
            toSquare = toNotation(row, col);
            toggleGameState();
            pushToStack();
        }
        return promoted;
    }

    /**
     * Toggles game state and updates board view
     */
    private void toggleGameState() {
        whiteToPlay = !whiteToPlay;
        pgn.setWhiteToPlay(whiteToPlay);
        if (chessBoard != null) {
            chessBoard.clearSelection();
            if (animate) chessBoard.initializeAnimation();
        }
    }

    /**
     * Saves game objects to stack and updates game
     */
    private void pushToStack() {
        boardModelStack.push(boardModel.clone());
        FENs.push(boardModel.toFEN(this));
        boardModel.fromSquare = fromSquare;
        boardModel.toSquare = toSquare;
        fromSquare = "";
        toSquare = "";
        updateAll();
    }

    /**
     * Revert the last move
     */
    public void undoLastMove() {
        if (gameTerminated) return;
        pgn.removeLast();
        if (boardModelStack.size() > 1) {
            boardModelStack.pop();
            FENs.pop();
            boardModel = boardModelStack.peek().clone();
            if (boardModel.enPassantPawn != null)
                Log.d(TAG, "undoLastMove: EnPassantPawn: " + boardModel.enPassantPawn.getPosition() + " EnPassantSquare: " + boardModel.enPassantSquare);
            toggleGameState();
            updateAll();
        }
    }

    /**
     * Updates and saves game status, necessary fields and views
     */
    private void updateAll() {
        saveGame();
        long start, end;

        count = 0;
        start = System.nanoTime();
        computeLegalMoves();
        end = System.nanoTime();
        isChecked();
        printTime(TAG, "updating LegalMoves", end - start, count);

        start = System.nanoTime();
        checkGameTermination();
        end = System.nanoTime();
        printTime(TAG, "checking Game Termination", end - start, -1);

        if (gameFragmentInterface != null) gameFragmentInterface.updateViews();
        if (chessBoard != null) chessBoard.invalidate();
        Log.d(TAG, "updateAll: Updated and saved game");
    }

    /**
     * Checks for termination of the game after each move
     */
    private void checkGameTermination() {
        ChessState terminationState;
//        if (!loadingPGN) {
//      Check for draw by insufficient material
        if (drawByInsufficientMaterial()) {
            termination = "Draw by insufficient material";
            terminationState = ChessState.DRAW;
            pgn.setTermination(termination);
            terminateGame(terminationState);
            return;
        }

//      Check for draw by repetition
        if (drawByRepetition()) {
            termination = "Draw by repetition";
            terminationState = ChessState.DRAW;
            pgn.setTermination(termination);
            terminateGame(terminationState);
            return;
        }
//        }
        if (noLegalMoves()) {
            //Log.d(TAG, "checkGameTermination: No Legal Moves for: " + playerToPlay());
            isChecked();

            if (!playerToPlay().isInCheck()) {
                termination = "Draw by Stalemate";
                terminationState = ChessState.STALEMATE;
            } else {
                termination = opponentPlayer(playerToPlay()).getName() + " won by Checkmate";
                terminationState = ChessState.CHECKMATE;
            }

            pgn.setTermination(termination);
            terminateGame(terminationState);
        }
    }

    /**
     * Checks whether the active player has no legal moves
     *
     * @return <code>true|false</code> - Player has no legal moves
     */
    private boolean noLegalMoves() {
        Set<Map.Entry<Piece, HashSet<Integer>>> pieces = legalMoves.entrySet();
        for (Map.Entry<Piece, HashSet<Integer>> entry : pieces)
            if (!entry.getValue().isEmpty()) return false;
        return true;
    }

    /**
     * Terminates the game and displays Game Over dialog
     *
     * @param terminationState State of the termination
     */
    public void terminateGame(ChessState terminationState) {
        saveGame();
        gameState = terminationState;
        if (termination == null || termination.isEmpty()) termination = pgn.getTermination();
        if (!loadingPGN && dataManager.deleteGameFiles())
            Log.d(TAG, "deleteGameFiles: Game files deleted successfully!");
        gameTerminated = true;

        if (pgn.getMoveCount() == 0)
            Toast.makeText(context, "Game aborted", Toast.LENGTH_SHORT).show();

        pgn.setResult(getResult());
        if (!loadingPGN && pgn.getMoveCount() != 0) {
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
            String name = "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn";
            if (dataManager.savePGN(pgn, name)) {
                Log.d(TAG, String.format("terminateGame: Game PGN saved successfully!\n%s----------------------------------------", pgn.toString()));
            }
        }

        Log.v(TAG, "terminateGame: Game terminated by: " + terminationState);
        if (gameFragmentInterface != null) gameFragmentInterface.terminateGame(termination);
    }

    @Override
    public void terminateByTimeOut(Player player) {
        termination = opponentPlayer(playerToPlay()).getName() + " won on time";
        pgn.setTermination(termination);
        terminateGame(ChessState.TIMEOUT);
    }

    /**
     * Checks if the game is draw due to insufficient material to checkmate
     *
     * @return <code>true|false</code>
     */
    private boolean drawByInsufficientMaterial() {
        boolean KB = false, kb = false, BLight = false, bLight = false;
        LinkedHashSet<Piece> pieces = boardModel.pieces;
        HashSet<Piece> whitePieces = new HashSet<>(), blackPieces = new HashSet<>();
        for (Piece piece : pieces) {
            if (piece.isCaptured()) continue;
            if (piece.isWhite()) whitePieces.add(piece);
            else blackPieces.add(piece);
        }

        if (whitePieces.size() == 1 && blackPieces.size() == 1) return true;
        else if (whitePieces.size() <= 2 && blackPieces.size() == 1 || whitePieces.size() == 1 && blackPieces.size() <= 2) {
            for (Piece whitePiece : whitePieces)
                if (whitePiece.getRank() == Rank.BISHOP || whitePiece.getRank() == Rank.KNIGHT)
                    return true;
            for (Piece blackPiece : blackPieces)
                if (blackPiece.getRank() == Rank.BISHOP || blackPiece.getRank() == Rank.KNIGHT)
                    return true;
        } else if (whitePieces.size() <= 2 && blackPieces.size() <= 2) {
            for (Piece whitePiece : whitePieces)
                if (whitePiece.getRank() == Rank.BISHOP) {
                    KB = true;
                    BLight = (whitePiece.getRow() + whitePiece.getCol()) % 2 == 0;
                }
            for (Piece blackPiece : blackPieces)
                if (blackPiece.getRank() == Rank.BISHOP) {
                    kb = true;
                    bLight = (blackPiece.getRow() + blackPiece.getCol()) % 2 == 0;
                }
            return KB && kb && BLight == bLight;
        }
        return false;
    }

    /**
     * Checks if the game is draw by repetition
     *
     * @return <code>true|false</code>
     */
    private boolean drawByRepetition() {
        int i = 0, j, l = FENs.size();
        String[] positions = new String[l];
        for (String FEN : FENs) positions[l - i++ - 1] = FEN;

        String lastPosition = positions[l - 1];
        for (i = 0; i < l - 2; i++)
            if (lastPosition.equals(positions[i])) {
                //Log.d(TAG, String.format("drawByRepetition: One repetition found:\n%d: %s\n%d: %s", i / 2 + 1, positions[i], (l - 1) / 2 + 1, lastPosition));
                for (j = i + 1; j < l - 1; j++)
                    if (positions[i].equals(positions[j])) {
                        //Log.d(TAG, "Draw by repetition");
                        //Log.d(TAG, String.format("Position : %d, %d & %d", i / 2 + 1, j / 2 + 1, (l - 1) / 2 + 1));
                        //Log.d(TAG, String.format("Repeated moves FEN:\n%d - %s\n%d - %s\n%d - %s", i / 2 + 1, positions[i], j / 2 + 1, positions[j], (l - 1) / 2 + 1, lastPosition));
                        return true;
                    }
                positions[i] = "";
            }
        return false;
    }

    /**
     * Checks if any of the player is checked
     */
    private void isChecked() {
        boolean isChecked = false;
        King whiteKing = boardModel.getWhiteKing();
        King blackKing = boardModel.getBlackKing();
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);

        if (whiteKing.isChecked(this)) {
            isChecked = true;
            Player.WHITE.setInCheck(true);
            //Log.d(TAG, "isChecked: White King checked");
        }
        if (blackKing.isChecked(this)) {
            isChecked = true;
            Player.BLACK.setInCheck(true);
            //Log.d(TAG, "isChecked: Black King checked");
        }

        if (!loadingPGN && vibrationEnabled && isChecked) {
            long vibrationDuration = 150;
            vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    /**
     * Prints all legal moves for the player in check
     */
    private void printLegalMoves() {
        if (legalMoves == null) return;
        Set<Map.Entry<Piece, HashSet<Integer>>> pieces = legalMoves.entrySet();
        for (Map.Entry<Piece, HashSet<Integer>> entry : pieces) {
            Piece piece = entry.getKey();
            HashSet<Integer> moves = entry.getValue();
            if (!moves.isEmpty()) {
                StringBuilder allMoves = new StringBuilder();
                for (int move : moves)
                    allMoves.append(toNotation(move)).append(" ");
                //Log.d(TAG, "printLegalMoves: Legal Moves for " + piece.getPosition() + ": " + allMoves);
            } else Log.d(TAG, "printLegalMoves: No legal moves for " + piece.getPosition());
        }
    }

    /**
     * Computes and updates all legal moves for the player to play
     */
    private void computeLegalMoves() {
        legalMoves = new HashMap<>();
        LinkedHashSet<Piece> pieces = boardModel.pieces;
        for (Piece piece : pieces) {
            if (!isPieceToPlay(piece) || piece.isCaptured()) continue;

            HashSet<Integer> possibleMoves = piece.getPossibleMoves(this), illegalMoves = new HashSet<>();
            for (int move : possibleMoves) {
                if (isIllegalMove(piece, move)) illegalMoves.add(move);
                count++;
            }

            possibleMoves.removeAll(illegalMoves);
            legalMoves.put(piece, possibleMoves);
        }
    }

    /**
     * Finds if a move is illegal for the given piece
     *
     * @param piece <code>Piece</code> to move
     * @param move  Move for the piece
     * @return <code>True|False</code>
     */
    private boolean isIllegalMove(Piece piece, int move) {
        TempBoardInterface tempBoardInterface = new TempBoardInterface();
        tempBoardInterface.tempBoardModel = boardModel.clone();
        boolean isChecked;
        int row = piece.getRow(), col = piece.getCol(), toRow = toRow(move), toCol = toCol(move);
        tempBoardInterface.move(row, col, toRow, toCol);
        if (piece.isWhite())
            isChecked = tempBoardInterface.tempBoardModel.getWhiteKing().isChecked(tempBoardInterface);
        else
            isChecked = tempBoardInterface.tempBoardModel.getBlackKing().isChecked(tempBoardInterface);
        return isChecked;
    }

    /**
     * Returns result of the game
     *
     * @return <code> * | 0-1 | 1-0 | 1/2-1/2 </code>
     */
    public String getResult() {
        switch (gameState) {
            case ONGOING:
                return PGN.RESULT_ONGOING;
            case RESIGN:
            case TIMEOUT:
                return termination.contains(Player.WHITE.getName()) ? PGN.RESULT_WHITE_WON : PGN.RESULT_BLACK_WON;
            case CHECKMATE:
                return Player.WHITE.isInCheck() ? PGN.RESULT_BLACK_WON : PGN.RESULT_WHITE_WON;
            case STALEMATE:
            case DRAW:
                return PGN.RESULT_DRAW;
        }
        return PGN.RESULT_ONGOING;
    }

    @Override
    public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
        return legalMoves;
    }

    @Override
    public BoardModel getBoardModel() {
        return boardModel;
    }

    public Stack<BoardModel> getBoardModelStack() {
        return boardModelStack;
    }

    public Stack<String> getFENs() {
        return FENs;
    }

    public PGN getPGN() {
        return pgn;
    }

    /**
     * Returns the current player to play
     *
     * @return <code>White|Black</code>
     */
    public Player playerToPlay() {
        return whiteToPlay ? Player.WHITE : Player.BLACK;
    }

    /**
     * Returns whether the piece belongs to the current player
     *
     * @param piece <code>Piece</code> to check
     * @return <code>True|False</code>
     */
    @Override
    public boolean isPieceToPlay(@NonNull Piece piece) {
        return piece.getPlayer() == playerToPlay();
    }

    @Override
    public boolean isWhiteToPlay() {
        return whiteToPlay;
    }

    @Override
    public boolean isGameTerminated() {
        return gameTerminated;
    }

    public static String getTAG() {
        return TAG;
    }

    /**
     * Temporary BoardInterface for computing Legal Moves
     */
    static class TempBoardInterface implements BoardInterface {
        private static final String TAG = "TempBoardInterface";
        private BoardModel tempBoardModel;

        @Override
        public Piece pieceAt(int row, int col) {
            return tempBoardModel.pieceAt(row, col);
        }

        @Override
        public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
            Piece opponentPiece = pieceAt(toRow, toCol), movingPiece = pieceAt(fromRow, fromCol);
            if (movingPiece != null) movingPiece.moveTo(toRow, toCol);
            else Log.d(TAG, "move: Error! movingPiece is null");
            if (opponentPiece != null) tempBoardModel.capturePiece(opponentPiece);
            return true;
        }

        @Override
        public void addToPGN(Piece piece, String move, int fromRow, int fromCol) {
        }

        @Override
        public boolean capturePiece(Piece piece) {
            return false;
        }

        @Override
        public void promote(Pawn pawn, int row, int col, int fromRow, int fromCol) {
        }

        @Override
        public void terminateByTimeOut(Player player) {
        }

        @Override
        public BoardModel getBoardModel() {
            return tempBoardModel;
        }

        @Override
        public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
            return null;
        }

        @Override
        public boolean isWhiteToPlay() {
            return false;
        }

        @Override
        public boolean isGameTerminated() {
            return false;
        }

        @Override
        public boolean isPieceToPlay(Piece piece) {
            return false;
        }
    }
}