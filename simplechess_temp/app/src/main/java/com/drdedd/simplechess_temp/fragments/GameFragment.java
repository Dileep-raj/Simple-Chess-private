package com.drdedd.simplechess_temp.fragments;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplechess_temp.data.Regexes.activePlayerPattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.ChessBoard;
import com.drdedd.simplechess_temp.ChessTimer;
import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.databinding.FragmentGameBinding;
import com.drdedd.simplechess_temp.dialogs.GameOverDialog;
import com.drdedd.simplechess_temp.dialogs.PromoteDialog;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

/**
 * {@inheritDoc}
 * Fragment to view, play, load and save chess game
 */
@SuppressLint("NewApi")
public class GameFragment extends Fragment implements BoardInterface, View.OnClickListener {
    private final static String TAG = "GameFragment";
    protected static final String FEN_KEY = "ARG_FEN", NEW_GAME_KEY = "NewGame", LOAD_GAME_KEY = "LoadGame", LOAD_PGN_KEY = "LoadPGN", LOAD_GAME_FRAGMENT_KEY = "LoadGameFragmentOBJ";
    private String FEN;
    private FragmentGameBinding binding;
    private String white = "White", black = "Black", app, date, termination;
    private PGN pgn;
    protected BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private ImageButton btn_undo_move, btn_resign;
    public TextView PGN_textView, gameStateView, whiteName, blackName, whiteCaptured, blackCaptured, whiteValue, blackValue, whiteTimeTV, blackTimeTV;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private static ChessState gameState;
    private static boolean gameTerminated;
    protected Stack<BoardModel> boardModelStack;
    protected ClipboardManager clipboard;
    protected HashMap<Piece, HashSet<Integer>> legalMoves;
    private LinkedList<String[]> FENs;
    private boolean timerEnabled, vibrationEnabled, newGame, loadingPGN;
    public LinearLayout whiteTimeLayout, blackTimeLayout;
    private ChessTimer chessTimer;
    private Vibrator vibrator;
    private int count;
    private LoadGameFragment loadGameFragment;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        navController = Navigation.findNavController(view);

        chessBoard = binding.chessBoard;
        btn_undo_move = binding.btnUndoMove;
        btn_resign = binding.btnResign;
        PGN_textView = binding.pgnTextview;
        gameStateView = binding.gameStateView;
        whiteName = binding.whiteNameTV;
        blackName = binding.blackNameTV;
        whiteCaptured = binding.whiteCaptured;
        blackCaptured = binding.blackCaptured;
        whiteValue = binding.whiteValue;
        blackValue = binding.blackValue;

        horizontalScrollView = binding.scrollView;

        whiteTimeTV = binding.whiteTimeTV;
        blackTimeTV = binding.blackTimeTV;
        whiteTimeLayout = binding.whiteTimeLayout;
        blackTimeLayout = binding.blackTimeLayout;

        dataManager = new DataManager(requireContext());

        clipboard = (ClipboardManager) requireActivity().getSystemService(CLIPBOARD_SERVICE);

        newGame = args.getBoolean(NEW_GAME_KEY);
        initializeData();

        if (args.containsKey(LOAD_PGN_KEY)) {
            loadingPGN = true;
            loadGameFragment = (LoadGameFragment) args.getSerializable(LOAD_GAME_FRAGMENT_KEY);
            HashMap<String, String> tags = loadGameFragment.tagsMap;
            tags.get(PGN.APP_TAG);
            white = tags.get(PGN.WHITE_TAG);
            black = tags.get(PGN.BLACK_TAG);
            date = tags.get(PGN.DATE_TAG);
        }
        if (args.containsKey(FEN_KEY)) {
            FEN = args.getString(FEN_KEY);
            Log.d(TAG, "onViewCreated: Loading FEN: " + FEN);
            reset();
            newGame = false;
        }

        binding.btnSaveExit.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCopyPgn.setOnClickListener(v -> copyPGN());
        binding.btnExportPgn.setOnClickListener(v -> exportPGN());
        binding.btnReset.setOnClickListener(v -> reset());
        binding.btnCopyFen.setOnClickListener(v -> copyFEN());

        btn_resign.setOnClickListener(v -> resign());
        btn_undo_move.setOnClickListener(v -> undoLastMove());

        if (newGame) reset();
        else updateAll();

        if (!timerEnabled) {
            whiteTimeLayout.setVisibility(View.GONE);
            blackTimeLayout.setVisibility(View.GONE);
        } else chessTimer.startTimer();

        if (loadingPGN) {
            long start = System.nanoTime();
            boolean result = loadPGN();
            long end = System.nanoTime();

            Log.d(TAG, "onViewCreated: Dialog dismissed");
            HomeFragment.printTime(TAG, "loading PGN moves", end - start, loadGameFragment.moves.size());
            if (result) Toast.makeText(requireContext(), "Game Valid", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeData() {
        gameTerminated = false;
        loadingPGN = false;
        FEN = "";

        timerEnabled = dataManager.isTimerEnabled();
        vibrationEnabled = dataManager.getVibration();

        //    private String[] permissions;
        SimpleDateFormat pgnDate = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);

        white = dataManager.getWhite();
        Player.WHITE.setName(white);
        black = dataManager.getBlack();
        Player.BLACK.setName(black);
        app = PGN.APP_NAME;
        date = pgnDate.format(new Date());

        if (timerEnabled)
            chessTimer = new ChessTimer(this, dataManager.getWhiteTimeLeft(), dataManager.getBlackTimeLeft());

        if (!newGame) {
            boardModel = (BoardModel) dataManager.readObject(DataManager.boardFile);
            pgn = (PGN) dataManager.readObject(DataManager.PGNFile);
            boardModelStack = (Stack<BoardModel>) dataManager.readObject(DataManager.stackFile);
            FENs = (LinkedList<String[]>) dataManager.readObject(DataManager.FENsListFile);
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel(requireContext(), true);
            boardModelStack = new Stack<>();
            FENs = new LinkedList<>();
            boardModelStack.push(boardModel);
            FENs.push(boardModel.toFENStrings());
            pgn = new PGN(PGN.APP_NAME, white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
            if (timerEnabled) chessTimer = new ChessTimer(this);
        }

        gameState = pgn.getGameState();         //Get previous state from PGN
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names
        PGN_textView.setText(pgn.getPGN());     //Update PGN in TextView

        whiteName.setText(white);
        blackName.setText(black);

        chessBoard.boardInterface = this;
        chessBoard.setTheme(dataManager.getBoardTheme());
        chessBoard.invalidate = true;

        vibrator = (Vibrator) requireContext().getSystemService(VIBRATOR_SERVICE);
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
//            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        else permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
    }

    public void reset() {
        gameTerminated = false;
        gameState = ChessState.WHITE_TO_PLAY;

        if (timerEnabled) {
            if (chessTimer != null) chessTimer.stopTimer();
            chessTimer = new ChessTimer(this);
            chessTimer.startTimer();
        }

        if (FEN.isEmpty()) {
            boardModel = new BoardModel(requireContext(), true);
            pgn = new PGN(app, white, black, date, ChessState.WHITE_TO_PLAY);
        } else {
            long start = System.nanoTime();
            boardModel = BoardModel.parseFEN(FEN, requireContext());

            Matcher player = activePlayerPattern.matcher(FEN);
            if (player.find())
                gameState = player.group().trim().equals("w") ? ChessState.WHITE_TO_PLAY : ChessState.BLACK_TO_PLAY;
            long end = System.nanoTime();
            HomeFragment.printTime(TAG, "parsing FEN", end - start, FEN.length());
            pgn = new PGN(app, white, black, date, gameState, FEN);
        }
        boardModelStack = new Stack<>();
        FENs = new LinkedList<>();
        pushToStack();
        Log.v(TAG, "reset: New PGN created " + date);
        Log.v(TAG, "reset: initial BoardModel in stack: " + boardModel);
        PGN_textView.setText(pgn.getPGN());
        updateAll();
    }

    private boolean loadPGN() {
        LinkedList<String> moves = loadGameFragment.moves;
        char ch;
        int i, startRow, startCol, destRow, destCol, c = 0, size = moves.size();
        boolean promotion;
        Rank rank = null, promotionRank;
        Piece piece;
        Player player;

        for (String move : moves) {
            startRow = -1;
            startCol = -1;
            destRow = -1;
            destCol = -1;
            promotion = false;
            promotionRank = null;
            player = playerToPlay();

            try {
                if (move.equals(PGN.SHORT_CASTLE)) {
                    King king = gameState == ChessState.WHITE_TO_PLAY ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canShortCastle(this))
                        movePiece(king.getRow(), king.getCol(), king.getRow(), king.getCol() + 2);
                    continue;
                } else if (move.equals(PGN.LONG_CASTLE)) {
                    King king = gameState == ChessState.WHITE_TO_PLAY ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canShortCastle(this))
                        movePiece(king.getRow(), king.getCol(), king.getRow(), king.getCol() - 3);
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
                            break;

                        case 'K':
                        case 'Q':
                        case 'R':
                        case 'N':
                        case 'B':
                        case 'P':
                        case 'x':
                        case '+':
                        case '#':
                            break;
                    }
                    if (promotion) {
                        ch = move.charAt(i + 1);
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
                        if (promotionRank == null) return false;
                    }
                }

                if (startRow != -1 && startCol != -1) {
                    piece = boardModel.pieceAt(startRow, startCol);
                    Log.d(TAG, String.format("loadPGN: piece found at %s", toNotation(startRow, startCol)));
                } else if (startCol != -1) {
                    piece = boardModel.searchCol(player, rank, startCol);
                    Log.d(TAG, "loadPGN: searched col: " + startCol);
                } else if (startRow != -1) {
                    piece = boardModel.searchRow(player, rank, startRow);
                    Log.d(TAG, "loadPGN: searched row:" + startRow);
                } else {
                    piece = boardModel.searchPiece(this, player, rank, destRow, destCol);
                    Log.d(TAG, "loadPGN: piece searched");
                }

                if (promotion) {
                    Pawn pawn = (Pawn) pieceAt(startRow, startCol);
                    if (promote(pawn, destRow, destCol, startRow, startCol, promotionRank))
                        Log.d(TAG, String.format("loadPGN: Promoted to %s at %s", promotionRank, toNotation(destRow, destCol)));
                }

//                Log.d(TAG, String.format("loadPGN: Player: %s Rank: %s Start square: (%d,%d), End square: (%d,%d) move: %s legal: %b", player, rank, piece.getRow(), piece.getCol(), destRow, destCol, move, piece.canMoveTo(this, destRow, destCol)));
//                if (movePiece(piece.getRow(), piece.getCol(), destRow, destCol))
//                    Log.d(TAG, "loadPGN: Move success");
//                else {
//                    Log.d(TAG, String.format("loadPGN: Move invalid %s->%s", piece.getPosition(), toNotation(destRow, destCol)));
//                    return false;
//                }
                if (piece != null) movePiece(piece.getRow(), piece.getCol(), destRow, destCol);
                else {
                    Log.d(TAG, String.format("loadPGN: Move invalid! Piece not found! %s move: %s", toNotation(destRow, destCol), move));
                    return false;
                }
                c++;
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error occurred after move " + move, Toast.LENGTH_LONG).show();
                Log.e(TAG, "loadPGN: Error occurred after move " + move, e);
                return false;
            }
        }

        if (loadGameFragment.tagsMap.containsKey(PGN.RESULT_TAG))
            pgn.setAppendResult(loadGameFragment.tagsMap.get(PGN.RESULT_TAG));
        loadGame();
        return true;
    }

    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    @Override
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
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
                chessBoard.invalidate();
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
            boolean result = chessBoard.movePiece(fromRow, fromCol, toRow, toCol);
            if (result) {
                if (movingPiece.getRank() == Rank.PAWN) if (Math.abs(fromRow - toRow) == 2) {
                    Pawn enPassantPawn = (Pawn) movingPiece;
                    boardModel.enPassantPawn = enPassantPawn;
                    boardModel.enPassantSquare = toNotation(enPassantPawn.getRow() - enPassantPawn.direction, enPassantPawn.getCol());
                    Log.d(TAG, "movePiece: EnPassantPawn: " + boardModel.enPassantPawn.getPosition() + " EnPassantSquare: " + boardModel.enPassantSquare);
                } else {
                    boardModel.enPassantPawn = null;
                    boardModel.enPassantSquare = "";
                }
                pushToStack();
                toggleGameState();
                if (Player.WHITE.isInCheck() || Player.BLACK.isInCheck()) printLegalMoves();
            }
            return result;
        }
    }

    public void saveGame() {
        if (gameTerminated) return;
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
            moveStringBuilder.append('=').append(piece.getPosition().charAt(0));
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
        PromoteDialog promoteDialog = new PromoteDialog(requireContext());
        promoteDialog.show();

//        Set image buttons as respective color pieces
        promoteDialog.findViewById(R.id.promote_to_queen).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString()));
        promoteDialog.findViewById(R.id.promote_to_rook).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.ROOK.toString()));
        promoteDialog.findViewById(R.id.promote_to_bishop).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString()));
        promoteDialog.findViewById(R.id.promote_to_knight).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString()));

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
            Log.v(TAG, "promote: Promoted to " + rank);
            pushToStack();
            toggleGameState();
        });
    }

    public boolean promote(Pawn pawn, int row, int col, int fromRow, int fromCol, Rank rank) {
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
            pushToStack();
            toggleGameState();
        }
        return promoted;
    }

    public static boolean isGameTerminated() {
        return gameTerminated;
    }

    private void toggleGameState() {
        if (gameState == ChessState.WHITE_TO_PLAY) gameState = ChessState.BLACK_TO_PLAY;
        else if (gameState == ChessState.BLACK_TO_PLAY) gameState = ChessState.WHITE_TO_PLAY;
        pgn.setGameState(gameState);
        if (timerEnabled) chessTimer.toggleTimer();
        updateAll();
    }

    private void undoLastMove() {
        if (gameTerminated) return;
        pgn.removeLast();
        if (boardModelStack.size() > 1) {
            boardModelStack.pop();
            FENs.pop();
            boardModel = boardModelStack.peek().clone();
            if (boardModel.enPassantPawn != null)
                Log.d(TAG, "undoLastMove: EnPassantPawn: " + boardModel.enPassantPawn.getPosition() + " EnPassantSquare: " + boardModel.enPassantSquare);
            toggleGameState();
        }
    }

    /**
     * Updates all necessary fields and views
     */
    private void updateAll() {
        saveGame();
        long start, end;

        start = System.nanoTime();
        updatePossibleMoves();
        end = System.nanoTime();

        count = 0;
        start = System.nanoTime();
        updateLegalMoves();
        end = System.nanoTime();
        isChecked();
        HomeFragment.printTime(TAG, "updating LegalMoves", end - start, count);
        checkGameTermination();
        updateViews();

        btn_undo_move.setEnabled(boardModelStack.size() > 1);
        if (gameTerminated) btn_undo_move.setEnabled(false);

        if (btn_undo_move.isEnabled()) btn_undo_move.setAlpha(1f);
        else btn_undo_move.setAlpha(0.5f);

        if (btn_resign.isEnabled()) btn_resign.setAlpha(1f);
        else btn_resign.setAlpha(0.5f);

        chessBoard.invalidate();
        Log.i(TAG, "updateAll: Updated and saved game");
    }

    /**
     * Checks for termination of the game after each move
     */
    private void checkGameTermination() {
        ChessState terminationState;

//      Check for draw by insufficient material
        if (drawByInsufficientMaterial()) {
            termination = "Draw by insufficient material";
            terminationState = ChessState.DRAW;
            pgn.setTermination(termination);
            Toast.makeText(requireContext(), termination, Toast.LENGTH_LONG).show();
            terminateGame(terminationState);
            return;
        }

//      Check for draw by repetition
        if (drawByRepetition()) {
            termination = "Draw by repetition";
            terminationState = ChessState.DRAW;
            pgn.setTermination(termination);
            Toast.makeText(requireContext(), termination, Toast.LENGTH_LONG).show();
            terminateGame(terminationState);
            return;
        }

        if (noLegalMoves()) {
            Log.v(TAG, "checkGameTermination: No Legal Moves for: " + playerToPlay());
            isChecked();

            if (!playerToPlay().isInCheck()) {
                termination = "Draw by Stalemate";
                terminationState = ChessState.STALEMATE;
            } else {
                termination = opponentPlayer(playerToPlay()).getName() + " won by Checkmate";
                terminationState = ChessState.CHECKMATE;
            }

            pgn.setTermination(termination);
            Toast.makeText(requireContext(), termination, Toast.LENGTH_LONG).show();
            terminateGame(terminationState);
        }
    }

    private boolean noLegalMoves() {
        Set<Map.Entry<Piece, HashSet<Integer>>> pieces = legalMoves.entrySet();
        for (Map.Entry<Piece, HashSet<Integer>> entry : pieces)
            if (!entry.getValue().isEmpty()) return false;
        return true;
    }

    /**
     * Terminates the game and displays Game Over dialog
     *
     * @param gameState State of the termination
     */
    private void terminateGame(ChessState gameState) {
        saveGame();
        gameTerminated = true;

        if (dataManager.saveGame() && !loadingPGN)
            Log.d(TAG, "terminateGame: Game saved successfully!");

        if (loadingPGN) loadGame();

        if (timerEnabled && chessTimer != null) chessTimer.stopTimer();
        chessBoard.invalidate();
        setGameState(gameState);
        btn_resign.setEnabled(false);
        Log.i(TAG, "terminateGame: Game terminated by: " + gameState);
        chessBoard.setOnClickListener(this);
        showGameOverDialog();
    }

    private void showGameOverDialog() {
        GameOverDialog gameOverDialog = new GameOverDialog(requireContext(), pgn);
        gameOverDialog.show();
        gameOverDialog.findViewById(R.id.btn_view_game).setOnClickListener(v -> {
            gameOverDialog.dismiss();
            loadGame();
        });

//        gameOverDialog.setOnDismissListener(dialogInterface -> finish());
    }

    private void loadGame() {
        Bundle args = new Bundle();
        args.putSerializable(LoadGameFragment.LOAD_GAME_KEY, boardModelStack);
        args.putSerializable(LoadGameFragment.LOAD_PGN_KEY, pgn);
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    public void terminateByTimeOut() {
        gameTerminated = true;
        termination = opponentPlayer(playerToPlay()).getName() + " won on time";
        pgn.setTermination(termination);
        terminateGame(ChessState.TIMEOUT);
    }

    private boolean drawByInsufficientMaterial() {
        boolean draw = false;
        int whiteValue = 0, blackValue = 0, totalValue;
        for (Piece piece : boardModel.pieces) {
            if (piece.isKing()) continue;
            if (piece.isWhite()) {
                if (piece.getRank() == Rank.PAWN) whiteValue += 9;
                else whiteValue += piece.getRank().getValue();
            } else {
                if (piece.getRank() == Rank.PAWN) blackValue += 9;
                blackValue += piece.getRank().getValue();
            }
        }
        totalValue = whiteValue + blackValue;
        if (whiteValue <= 3 && blackValue <= 3 && totalValue <= 7) {
            draw = true;
            Log.i(TAG, "Draw by insufficient material");
        }
        return draw;
    }

    private boolean drawByRepetition() {
        int i = 0, j, l = FENs.size();
        String[] positions = new String[l];
        for (String[] FEN : FENs) {
            positions[i++] = FEN[0];
        }
        for (i = 0; i < l - 2; i++)
            for (j = i + 1; j < l - 1; j++) {
                if (positions[j].isEmpty()) continue;
                if (positions[i].equals(positions[j])) {
                    for (int k = j + 1; k < l; k++)
                        if (positions[j].equals(positions[k])) {
                            Log.i(TAG, "Draw by repetition");
                            Log.i(TAG, "Position : " + i + ", " + j + " & " + k);
                            Log.i(TAG, "Repeated moves FEN:\n" + i + " - " + positions[i] + "\n" + j + " - " + positions[j] + "\n" + k + " - " + positions[k]);
                            return true;
                        }
                    positions[i] = "";
                    positions[j] = "";
                }
            }
        return false;
    }

    /**
     * Resigns and terminates the game
     */
    private void resign() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage("Are you sure you want to resign?");
        builder.setTitle("Resign");
        builder.setPositiveButton("Yes", (d, i) -> {
            pgn.setTermination(opponentPlayer(playerToPlay()).getName() + " won by Resignation");
            terminateGame(ChessState.RESIGN);
        });
        builder.setNegativeButton("No", (dialog, i) -> dialog.cancel());

        AlertDialog resignDialog = builder.create();
        resignDialog.setOnShowListener(di -> {
            resignDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor("#EFEFEF"));
            resignDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#EFEFEF"));
        });
        resignDialog.show();
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
            Log.i(TAG, "isChecked: White King checked");
        }
        if (blackKing.isChecked(this)) {
            isChecked = true;
            Player.BLACK.setInCheck(true);
            Log.i(TAG, "isChecked: Black King checked");
        }

        if (vibrationEnabled && isChecked) {
            long vibrationDuration = 150;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(vibrationDuration);
        }
    }

    /**
     * Updates possible moves of all pieces on the board
     */
    private void updatePossibleMoves() {
        HashSet<Piece> pieces = boardModel.pieces;
        for (Piece piece : pieces) piece.updatePossibleMoves(this);
    }

    /**
     * Updates all legal moves after each move
     */
    private void updateLegalMoves() {
        legalMoves = computeLegalMoves();
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
                Log.v(TAG, "movePiece: Legal Moves for " + piece.getPosition() + ": " + allMoves);
            }
//            else Log.v(TAG, "movePiece: No legal moves for " + piece.getPosition());
        }
    }

    /**
     * Computes all legal moves for the player to play
     *
     * @return {@code  HashMap<Piece, HashSet<Integer>>} <br> Set of legal moves for each <code>Piece</code>
     */
    private HashMap<Piece, HashSet<Integer>> computeLegalMoves() {
        legalMoves = new HashMap<>();
        HashSet<Piece> pieces = boardModel.pieces;
        for (Piece piece : pieces) {
            if (!isPieceToPlay(piece) || piece.isCaptured()) continue;
            HashSet<Integer> possibleMoves = piece.getPossibleMoves(), illegalMoves = new HashSet<>();
            for (int move : possibleMoves) {
                if (isIllegalMove(piece, move)) illegalMoves.add(move);
                count++;
            }

            possibleMoves.removeAll(illegalMoves);
            legalMoves.put(piece, possibleMoves);
        }
        return legalMoves;
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
        tempBoardInterface.movePiece(row, col, toRow, toCol);
        if (piece.isWhite())
            isChecked = tempBoardInterface.tempBoardModel.getWhiteKing().isChecked(tempBoardInterface);
        else
            isChecked = tempBoardInterface.tempBoardModel.getBlackKing().isChecked(tempBoardInterface);
        return isChecked;
    }

    /**
     * Export PGN to a file with <code>.pgn</code> extension
     */
    private void exportPGN() {
        Toast.makeText(getContext(), "Coming soon", Toast.LENGTH_SHORT).show();
//        if (checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
//            try {
//                String dir = pgn.exportPGN();
//                Toast.makeText(this, "PGN saved in " + dir, Toast.LENGTH_LONG).show();
//            } catch (IOException e) {
//                Toast.makeText(this, "File not saved!", Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "exportPGN: \n", e);
//            }
//        } else {
//            Toast.makeText(this, "Write permission is required to export PGN", Toast.LENGTH_SHORT).show();
//            ActivityCompat.requestPermissions(this, permissions, 0);
//        }
    }

    private void copyFEN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("FEN", boardModel.toFEN()));
        Toast.makeText(requireContext(), "FEN copied", Toast.LENGTH_SHORT).show();
    }

    private void copyPGN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("PGN", pgn.toString()));
        Toast.makeText(requireContext(), "PGN copied", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void updateViews() {
        StringBuilder whiteText = new StringBuilder(), blackText = new StringBuilder();
        int blackCapturedValue = 0, whiteCapturedValue = 0, difference;
        PGN_textView.setText(pgn.getPGN());
        horizontalScrollView.post(() -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));

        if (gameTerminated) gameStateView.setText(termination);
        else gameStateView.setText(playerToPlay().getName() + "'s turn");

        whiteCaptured.setText("");
        blackCaptured.setText("");

        whiteValue.setText("");
        blackValue.setText("");

        ArrayList<Piece> capturedPieces = boardModel.getCapturedPieces();
        for (Piece piece : capturedPieces) {
            if (piece.isWhite()) {
                blackText.append(piece.getUnicode());
                blackCapturedValue += piece.getRank().getValue();
            } else {
                whiteText.append(piece.getUnicode());
                whiteCapturedValue += piece.getRank().getValue();
            }
        }
        difference = Math.abs(blackCapturedValue - whiteCapturedValue);

        if (difference != 0) {
            if (whiteCapturedValue > blackCapturedValue) whiteValue.setText(" +" + difference);
            else blackValue.setText(" +" + difference);
        }

        whiteCaptured.setText(whiteText);
        blackCaptured.setText(blackText);
    }

    private void pushToStack() {
        boardModelStack.push(boardModel.clone());
        FENs.push(boardModel.toFENStrings());
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
     * Returns whether the piece belongs to the current player to play
     *
     * @param piece <code>Piece</code> to check
     * @return <code>True|False</code>
     */
    public static boolean isPieceToPlay(@NonNull Piece piece) {
        return piece.getPlayer() == playerToPlay();
    }

    /**
     * Returns the current player to play
     *
     * @return <code>White|Black</code>
     */
    public static Player playerToPlay() {
        return gameState == ChessState.WHITE_TO_PLAY ? Player.WHITE : Player.BLACK;
    }

    @Override
    public BoardModel getBoardModel() {
        return boardModel;
    }

    private void setGameState(ChessState gameState) {
        GameFragment.gameState = gameState;
    }

    public static ChessState getGameState() {
        return gameState;
    }

    @Override
    public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
        return legalMoves;
    }

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
    static String toNotation(int position) {
        return "" + (char) ('a' + position % 8) + (position / 8 + 1);
    }

    /**
     * Converts row and column numbers to Standard Notation
     */
    public static String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    @Override
    public void onClick(View view) {
        if (gameTerminated) terminateGame(gameState);
    }

    /**
     * Temporary BoardInterface for computing Legal Moves
     */
    static class TempBoardInterface implements BoardInterface {
        private BoardModel tempBoardModel;

        @Override
        public Piece pieceAt(int row, int col) {
            return tempBoardModel.pieceAt(row, col);
        }

        @Override
        public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
            Piece opponentPiece = pieceAt(toRow, toCol), tempPiece = pieceAt(fromRow, fromCol);
            if (tempPiece != null) tempPiece.moveTo(toRow, toCol);
            else Log.e("TempBoardInterface", "movePiece: Error! tempPiece is null");
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
        public BoardModel getBoardModel() {
            return tempBoardModel;
        }

        @Override
        public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
            return null;
        }
    }

    public static String getTAG() {
        return TAG;
    }
}
