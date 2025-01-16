package com.drdedd.simplichess.game;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplichess.data.Regexes.activePlayerPattern;
import static com.drdedd.simplichess.fragments.HomeFragment.printTime;
import static com.drdedd.simplichess.misc.MiscMethods.opponentPlayer;
import static com.drdedd.simplichess.misc.MiscMethods.toCol;
import static com.drdedd.simplichess.misc.MiscMethods.toColChar;
import static com.drdedd.simplichess.misc.MiscMethods.toNotation;
import static com.drdedd.simplichess.misc.MiscMethods.toRow;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.dialogs.PromoteDialog;
import com.drdedd.simplichess.game.gameData.ChessState;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.gameData.Rank;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.game.pgn.PGNData;
import com.drdedd.simplichess.game.pieces.King;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.interfaces.GameLogicInterface;
import com.drdedd.simplichess.interfaces.GameUI;
import com.drdedd.simplichess.views.ChessBoard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

/**
 * GameLogic to play moves and parse game
 * {@inheritDoc}
 */
public class GameLogic implements GameLogicInterface {
    private static final String TAG = "GameLogic";
    private final Context context;
    private final DataManager dataManager;
    private final ChessBoard chessBoard;
    private final GameUI gameUI;
    private final Handler gameFragmentHandler;
    private final String FEN;
    private final Random random = new Random();
    private final boolean newGame, saveProgress;
    private int count, halfMove, fullMove;
    private boolean vibrationEnabled, loadingPGN, sound, animate, gameTerminated, whiteToPlay, singlePlayer, infinitePlay;
    private Player botPlayer;
    private String white, black, app, date, fromSquare, toSquare, termination;
    private PGN pgn;
    private BoardModel boardModel = null;
    private ChessState gameState;
    private Stack<BoardModel> boardModelStack;
    private HashMap<String, HashSet<Integer>> allLegalMoves;
    private Stack<String> FENs;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Thread randomMoveThread;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * GameLogic for normal game setup
     *
     * @param gameUI     Game fragment interface reference
     * @param context    Context of the fragment
     * @param chessBoard ChessBoard view
     * @param newGame    Start new game or resume saved game
     */
    public GameLogic(GameUI gameUI, Context context, ChessBoard chessBoard, boolean newGame) {
        saveProgress = gameUI.saveProgress();
        dataManager = new DataManager(context);
        this.context = context;
        this.gameUI = gameUI;
        gameFragmentHandler = new Handler(Looper.getMainLooper());
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
     * @param gameUI     Game fragment reference
     * @param context    Context of fragment
     * @param chessBoard ChessBoard view
     * @param FEN        FEN of the starting position
     */
    public GameLogic(GameUI gameUI, Context context, ChessBoard chessBoard, String FEN) {
        newGame = false;
        saveProgress = gameUI.saveProgress();
        dataManager = new DataManager(context);
        this.context = context;
        this.gameUI = gameUI;
        gameFragmentHandler = new Handler(Looper.getMainLooper());
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
     * @param context Context of the Fragment
     */
    public GameLogic(Context context, PGNData pgnData) {
        this.context = context;
        dataManager = new DataManager(context);
        loadingPGN = true;
        gameUI = null;
        gameFragmentHandler = null;
        chessBoard = null;
        saveProgress = false;

        initializeData();

        white = pgnData.getTagOrDefault(PGN.TAG_WHITE, "White");
        black = pgnData.getTagOrDefault(PGN.TAG_BLACK, "Black");
        date = pgnData.getTagOrDefault(PGN.TAG_DATE, "?");
        pgn.setWhiteBlack(white, black);
        pgn.addAllTags(pgnData.getTagsMap());

        FEN = pgnData.getTagOrDefault(PGN.TAG_FEN, "");
        newGame = !(FEN != null && FEN.isEmpty());
        reset();

        pgn.setPGNData(pgnData);
    }

    /**
     * Initializes game data and objects: MediaPlayer, Vibrator, PGN, BoardModel
     */
    @SuppressWarnings("unchecked")
    private void initializeData() {
        mediaPlayer = MediaPlayer.create(context, R.raw.move_sound);
        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        gameTerminated = false;

        vibrationEnabled = dataManager.getBoolean(DataManager.VIBRATION);
        sound = dataManager.getBoolean(DataManager.SOUND);
        animate = dataManager.getBoolean(DataManager.ANIMATION);

        SimpleDateFormat pgnDate = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);

        white = dataManager.getString(DataManager.WHITE);
        black = dataManager.getString(DataManager.BLACK);

        app = context.getResources().getString(R.string.app_name);
        date = pgnDate.format(new Date());

        if (!newGame) {
            try {
                boardModel = (BoardModel) dataManager.readObject(DataManager.BOARD_FILE);
                pgn = (PGN) dataManager.readObject(DataManager.PGN_FILE);
                boardModelStack = (Stack<BoardModel>) dataManager.readObject(DataManager.STACK_FILE);
                FENs = (Stack<String>) dataManager.readObject(DataManager.FENS_LIST_FILE);
            } catch (Exception e) {
                Log.e(TAG, "initializeData: Exception occurred while reading game files!", e);
                if (dataManager.deleteGameFiles())
                    Log.d(TAG, "initializeData: Deleted game files!");
            }
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel(context, true);
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
        halfMove = 0;
        fullMove = 1;

        stopInfinitePlay();
        if (chessBoard != null) chessBoard.clearSelection();
        gameTerminated = false;
        whiteToPlay = true;

        if (FEN.isEmpty()) {
            boardModel = new BoardModel(context, true);
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

    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    public void playRandomMove() {
        try {
            if (randomMoveThread == null) {
                randomMoveThread = getNewThread();
                randomMoveThread.start();
            } else Log.wtf(TAG, "playRandomMove: Already playing random move!");
        } catch (Exception e) {
            Log.e(TAG, "playRandomMove: Exception!", e);
        }
    }

    private Thread getNewThread() {

        return new Thread(() -> {
            try {
                // Disable manual moves until random move is performed
                chessBoard.setViewOnly(true);
                Thread.sleep(infinitePlay ? 400 : 700);
                Set<String> squares = allLegalMoves.keySet();
                ArrayList<String> array = new ArrayList<>(squares);
                Collections.shuffle(array);

                // Pick a random piece square
                String square = array.get(random.nextInt(array.size()));
                if (square != null) {
                    HashSet<Integer> moves = allLegalMoves.get(square);

                    // If piece has no legal moves pick another piece
                    if (moves == null || moves.isEmpty()) for (String p : squares) {
                        moves = allLegalMoves.get(p);
                        if (moves != null && !moves.isEmpty()) {
                            square = p;
                            break;
                        }
                    }

                    // If legal moves found for a piece
                    if (moves != null && !moves.isEmpty()) {
                        Piece piece = pieceAt(toRow(square), toCol(square));
                        ArrayList<Integer> legalMoves = new ArrayList<>(moves);
                        int position = legalMoves.get(random.nextInt(legalMoves.size()));
                        int fromRow = piece.getRow(), fromCol = piece.getCol(), row = position / 8, col = position % 8;

                        // If move is promotion promote to random rank
                        if (piece.getRank() == Rank.PAWN) {
                            Pawn pawn = (Pawn) piece;
                            Rank[] ranks = {Rank.QUEEN, Rank.ROOK, Rank.BISHOP, Rank.KNIGHT};
                            if (pawn.canPromote() && promote(pawn, row, col, fromRow, fromCol, ranks[random.nextInt(ranks.length)]))
                                return;
                        }

                        // Perform the randomly picked move
                        String finalSquare = square;
                        handler.post(() -> Log.i(TAG, String.format("run: Move %s: %s %s->%s", move(fromRow, fromCol, row, col) ? "played" : "failed!", piece.getUnicode(), finalSquare, piece.getSquare())));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "run: Exception occurred!", e);
            }
            Log.d(TAG, "run: Thread stopped");
            chessBoard.setViewOnly(false);
            randomMoveThread = null;
        });
    }

    public void toggleInfinitePlay() {
        if (infinitePlay) stopInfinitePlay();
        else {
            singlePlayer = false;
            infinitePlay = true;
            playRandomMove();
        }
    }

    public void stopInfinitePlay() {
        infinitePlay = false;
        try {
            if (randomMoveThread != null) randomMoveThread.join();
        } catch (Exception e) {
            Log.e(TAG, "stopInfinitePlay: Exception occurred while stopping random move thread!", e);
        }
    }

    @Override
    public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameTerminated) return false;
        if (dataManager.getBoolean(DataManager.CHEAT_MODE)) {
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
        }

        Piece movingPiece = pieceAt(fromRow, fromCol);
        if (movingPiece == null) return false;

        // Check if the piece belongs to the active player
        if (isPieceToPlay(movingPiece) && allLegalMoves.get(movingPiece.getSquare()) != null) {
            HashSet<Integer> pieceLegalMoves = allLegalMoves.get(movingPiece.getSquare());
            if (pieceLegalMoves != null && !pieceLegalMoves.contains(toCol + toRow * 8))
                return false; // Return false if the move is an illegal move
        }
        boolean result = makeMove(movingPiece, fromRow, fromCol, toRow, toCol);
        if (result) {
            if (movingPiece.getRank() == Rank.PAWN || pgn.getMoves().getLast().contains(PGN.CAPTURE))
                halfMove = 0;
            else halfMove = boardModel.getHalfMove() + 1;

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

    /**
     * Checks for move validity and performs move
     *
     * @param fromRow Starting row of the piece
     * @param fromCol Starting column of the piece
     * @param toRow   Ending row of the piece
     * @param toCol   Ending column of the piece
     * @return Move result
     */
    private boolean makeMove(Piece movingPiece, int fromRow, int fromCol, int toRow, int toCol) {
        if (isGameTerminated()) return false;

//        Piece movingPiece = pieceAt(fromRow, fromCol);
        if (movingPiece == null || fromRow == toRow && fromCol == toCol || toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7 || !isPieceToPlay(movingPiece))
            return false;

        Piece toPiece = pieceAt(toRow, toCol);
        String uciMove = getUCIMove(fromRow, fromCol, toRow, toCol, null);
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
            String sanMove = getSANMove(movingPiece, fromRow, fromCol, toRow, toCol, PGN.CAPTURE, null);
            movingPiece.moveTo(toRow, toCol);
            capturePiece(toPiece);
            addMove(sanMove, uciMove);
//            addToPGN(movingPiece, PGN.CAPTURE, fromRow, fromCol);
            return true;
        }
        if (toPiece == null) {
            if (movingPiece.getRank() == Rank.KING) {
                King king = (King) movingPiece;
                if (!king.isCastled() && king.canMoveTo(this, toRow, toCol)) {
                    if (toCol - fromCol == -2 && king.canLongCastle(this)) {
                        king.longCastle(this);
                        String sanMove = PGN.LONG_CASTLE;
                        addMove(sanMove, uciMove);
//                        addToPGN(movingPiece, PGN.LONG_CASTLE, fromRow, fromCol);
                        return true;
                    }
                    if (toCol - fromCol == 2 && king.canShortCastle(this)) {
                        king.shortCastle(this);
                        String sanMove = PGN.SHORT_CASTLE;
                        addMove(sanMove, uciMove);
//                        addToPGN(movingPiece, PGN.SHORT_CASTLE, fromRow, fromCol);
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
                                String sanMove = getSANMove(pawn, fromRow, fromCol, toRow, toCol, PGN.CAPTURE, null);
                                movingPiece.moveTo(toRow, toCol);
                                addMove(sanMove, uciMove);
//                                addToPGN(pawn, PGN.CAPTURE, fromRow, fromCol);
                                return true;
                            }
                    if (pawn.canPromote()) {
                        promote(pawn, toRow, toCol, fromRow, fromCol);
                        //Log.d(TAG, "makeMove: Pawn promotion");
                        return false;
                    }
                }
                String sanMove = getSANMove(movingPiece, fromRow, fromCol, toRow, toCol, "", null);
                movingPiece.moveTo(toRow, toCol);
                addMove(sanMove, uciMove);
//                addToPGN(movingPiece, "", fromRow, fromCol);
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
        if (gameTerminated || !saveProgress) return;
        dataManager.saveData(boardModel, pgn, boardModelStack, FENs);
    }

    private String getSANMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol, String capture, Rank promotionRank) {
        LinkedHashSet<Piece> pieces = boardModel.pieces;
        String pieceChar;
        String startCol;
        if (piece.getRank() == Rank.PAWN) {
            pieceChar = "";
            if (!capture.isEmpty()) startCol = String.valueOf(toColChar(fromCol));
            else startCol = "";
        } else {
            pieceChar = String.valueOf(piece.getRank().getLetter());
            startCol = "";
        }
        String startRow = "";
        String promotion = promotionRank == null ? "" : "=" + promotionRank.getLetter();

        for (Piece tempPiece : pieces) {
            if (!startRow.isEmpty() && !startCol.isEmpty()) break;
            if (tempPiece.isCaptured() || tempPiece == piece) continue;
            if (tempPiece.getPlayer() == piece.getPlayer() && tempPiece.getRank() == piece.getRank()) {
                HashSet<Integer> tempPieceMoves = allLegalMoves.get(tempPiece.getSquare());
                if (tempPieceMoves != null && tempPieceMoves.contains(toRow * 8 + toCol))
                    if (piece.getRank() == Rank.KNIGHT) {
                        if (startCol.isEmpty() && piece.getCol() != tempPiece.getCol()) {
                            startCol = String.valueOf(toColChar(fromCol));
                            continue;
                        }
                        if (startRow.isEmpty() && piece.getRow() != tempPiece.getRow() && piece.getCol() == tempPiece.getCol())
                            startRow = String.valueOf(fromRow + 1);
                    } else {
                        if (startCol.isEmpty() && piece.getRow() == tempPiece.getRow())
                            startCol = String.valueOf(toColChar(fromCol));
                        if (startRow.isEmpty() && piece.getCol() == tempPiece.getCol())
                            startRow = String.valueOf(fromRow + 1);
                    }
            }
        }
        return String.format("%s%s%s%s%s%s", pieceChar, startCol, startRow, capture, toNotation(toRow, toCol), promotion);
    }

    private String getUCIMove(int fromRow, int fromCol, int toRow, int toCol, Rank promotionRank) {
        return String.format("%s%s%s", toNotation(fromRow, fromCol), toNotation(toRow, toCol), promotionRank == null ? "" : Character.toLowerCase(promotionRank.getLetter()));
    }

    private void addMove(String sanMove, String uciMove) {
        pgn.addMove(sanMove, uciMove);
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
            String sanMove = getSANMove(pawn, fromRow, fromCol, row, col, tempPiece == null ? "" : PGN.CAPTURE, rank);
            String uciMove = getUCIMove(fromRow, fromCol, row, col, rank);
            Piece promotedPiece = boardModel.promote(pawn, rank, row, col);
            if (tempPiece != null) {
                if (tempPiece.getPlayer() != promotedPiece.getPlayer()) {
                    capturePiece(tempPiece);
//                    addToPGN(promotedPiece, PGN.PROMOTE + PGN.CAPTURE, fromRow, fromCol);
                }
            }
//            else addToPGN(promotedPiece, PGN.PROMOTE, fromRow, fromCol);
            addMove(sanMove, uciMove);
            Log.v(TAG, String.format("promote: Promoted to %s %s->%s", rank, toNotation(fromRow, fromCol), toNotation(row, col)));
            fromSquare = toNotation(fromRow, fromCol);
            toSquare = toNotation(row, col);

            halfMove = 0;
            if (!pawn.isWhite()) fullMove = boardModel.getFullMove() + 1;

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
    public boolean promote(Pawn pawn, int row, int col, int fromRow, int fromCol, Rank rank) {
        boolean promoted = false;
        Piece tempPiece = pieceAt(row, col);
        String sanMove = getSANMove(pawn, fromRow, fromCol, row, col, tempPiece == null ? "" : PGN.CAPTURE, rank);
        String uciMove = getUCIMove(fromRow, fromCol, row, col, rank);
        Piece promotedPiece = boardModel.promote(pawn, rank, row, col);
        if (tempPiece != null) {
            if (tempPiece.getPlayer() != promotedPiece.getPlayer()) {
                capturePiece(tempPiece);
//                addToPGN(promotedPiece, PGN.PROMOTE + PGN.CAPTURE, fromRow, fromCol);
                promoted = true;
            }
        } else {
//            addToPGN(promotedPiece, PGN.PROMOTE, fromRow, fromCol);
            promoted = true;
        }
        if (promoted) {
            halfMove = 0;
            if (!pawn.isWhite()) fullMove = boardModel.getFullMove() + 1;
            addMove(sanMove, uciMove);
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
            if (animate) chessBoard.initializeAnimation(boardModel.fromSquare, boardModel.toSquare);
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
        boardModel.setMoveClocks(halfMove, fullMove);
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
        if (saveProgress) saveGame();
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

        if (gameUI != null) gameUI.updateViews();
        if (chessBoard != null) chessBoard.invalidate();
        Log.d(TAG, "updateAll: Updated and saved game");
        randomMoveThread = null;
        if (singlePlayer && playerToPlay() == botPlayer || infinitePlay) playRandomMove();
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
                termination = opponentPlayer(playerToPlay()) + " won by Checkmate";
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
        Set<Map.Entry<String, HashSet<Integer>>> legalMoves = allLegalMoves.entrySet();
        for (Map.Entry<String, HashSet<Integer>> entry : legalMoves)
            if (!entry.getValue().isEmpty()) return false;
        Log.d(TAG, "noLegalMoves: No legal moves for " + playerToPlay());
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
        if (saveProgress && dataManager.deleteGameFiles())
            Log.d(TAG, "deleteGameFiles: Game files deleted successfully!");
        gameTerminated = true;

        if (pgn.getPlyCount() == 0)
            Toast.makeText(context, "Game aborted", Toast.LENGTH_SHORT).show();

        pgn.setResult(getResult());
        if (saveProgress && pgn.getPlyCount() != 0) {
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
            String name = "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn";
            if (dataManager.savePGN(pgn, name)) {
                Log.d(TAG, String.format("terminateGame: Game PGN saved successfully!\n%s%n----------------------------------------", pgn.toString()));
            }
        }

        Log.v(TAG, "terminateGame: Game terminated by: " + terminationState);
        if (gameUI != null) gameFragmentHandler.post(() -> gameUI.terminateGame(termination));
    }

    @Override
    public void terminateByTimeOut(Player player) {
        termination = opponentPlayer(player) + " won on time";
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
                    if (positions[i].equals(positions[j])) return true;
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
        if (allLegalMoves == null) return;
        Set<Map.Entry<String, HashSet<Integer>>> pieces = allLegalMoves.entrySet();
        for (Map.Entry<String, HashSet<Integer>> entry : pieces) {
            HashSet<Integer> moves = entry.getValue();
            if (!moves.isEmpty()) {
                StringBuilder allMoves = new StringBuilder();
                for (int move : moves)
                    allMoves.append(toNotation(move)).append(" ");
                //Log.d(TAG, "printLegalMoves: Legal Moves for " + piece.getPosition() + ": " + allMoves);
            } else Log.v(TAG, "printLegalMoves: No legal moves for " + entry.getKey());
        }
    }

    /**
     * Computes and updates all legal moves for the player to play
     */
    private void computeLegalMoves() {
        allLegalMoves = new HashMap<>();
        LinkedHashSet<Piece> pieces = boardModel.pieces;
        for (Piece piece : pieces) {
            if (!isPieceToPlay(piece) || piece.isCaptured()) continue;

            HashSet<Integer> possibleMoves = piece.getPossibleMoves(this), illegalMoves = new HashSet<>();
            for (int move : possibleMoves) {
                if (isIllegalMove(piece, move)) illegalMoves.add(move);
                count++;
            }

            possibleMoves.removeAll(illegalMoves);
            allLegalMoves.put(piece.getSquare(), possibleMoves);
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
        TempGameLogicInterface tempBoardInterface = new TempGameLogicInterface();
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

    public HashMap<String, HashSet<Integer>> getAllLegalMoves() {
        return allLegalMoves;
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

    public void setBotPlayer(Player botPlayer) {
        if (botPlayer == null) return;
        this.botPlayer = botPlayer;
        singlePlayer = true;
        updateAll();
    }

    public static String getTAG() {
        return TAG;
    }

    /**
     * Temporary GameLogicInterface for computing Legal Moves
     */
    static class TempGameLogicInterface implements GameLogicInterface {
        private static final String TAG = "TempGameLogicInterface";
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

        public HashMap<String, HashSet<Integer>> getAllLegalMoves() {
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