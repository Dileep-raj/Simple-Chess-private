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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
 * {@inheritDoc}
 * Fragment to view, play, load and save chess game
 */
@SuppressLint("NewApi")
public class GameFragment extends Fragment implements BoardInterface {
    private final static String TAG = "GameFragment";
    protected static final String FEN_KEY = "ARG_FEN", NEW_GAME_KEY = "NewGame", LOAD_GAME_KEY = "LoadGame", LOAD_PGN_KEY = "LoadPGN";
    protected static final String TAGS_MAP_KEY = "tagsMap", MOVES_LIST_KEY = "moves", ANNOTATION_MAP_KEY = "annotationsMap", ALTERNATE_MOVE_SEQUENCE_KEY = "alternateMoveSequence", COMMENTS_KEY = "comments";
    private FragmentGameBinding binding;
    private String FEN, white = "White", black = "Black", app, date, termination, fromSquare, toSquare;
    private PGN pgn;
    protected BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private ImageButton btn_undo_move, btn_resign, btn_draw;
    public TextView PGN_textView, gameStateView, whiteName, blackName, whiteCaptured, blackCaptured, whiteValue, blackValue, whiteTimeTV, blackTimeTV;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private static ChessState gameState;
    private static boolean gameTerminated;
    protected Stack<BoardModel> boardModelStack;
    protected ClipboardManager clipboard;
    protected HashMap<Piece, HashSet<Integer>> legalMoves;
    private LinkedList<String> FENs;
    private boolean timerEnabled, vibrationEnabled, newGame, loadingPGN;
    public LinearLayout whiteTimeLayout, blackTimeLayout;
    private ChessTimer chessTimer;
    private Vibrator vibrator;
    private int count;
    private NavController navController;
    public HashMap<String, String> tagsMap;
    public LinkedList<String> moves;
    public LinkedHashMap<Integer, String> commentsMap, moveAnnotationMap, alternateMoveSequence;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
        Bundle args = requireArguments();
        navController = Navigation.findNavController(view);

        chessBoard = binding.chessBoard;
        btn_undo_move = binding.btnUndoMove;
        btn_resign = binding.btnResign;
        btn_draw = binding.btnDraw;
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
            tagsMap = (HashMap<String, String>) args.getSerializable(TAGS_MAP_KEY);
            moves = (LinkedList<String>) args.getSerializable(MOVES_LIST_KEY);

            commentsMap = (LinkedHashMap<Integer, String>) args.getSerializable(COMMENTS_KEY);
            moveAnnotationMap = (LinkedHashMap<Integer, String>) args.getSerializable(ANNOTATION_MAP_KEY);
            alternateMoveSequence = (LinkedHashMap<Integer, String>) args.getSerializable(ALTERNATE_MOVE_SEQUENCE_KEY);

            white = tagsMap.get(PGN.WHITE_TAG);
            black = tagsMap.get(PGN.BLACK_TAG);
            date = tagsMap.get(PGN.DATE_TAG);
        }
        if (args.containsKey(FEN_KEY)) {
            FEN = args.getString(FEN_KEY);
            Log.d(TAG, "onViewCreated: Loading FEN: " + FEN);
            reset();
            newGame = false;
        }

        binding.btnSaveExit.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCopyPgn.setOnClickListener(v -> copyPGN());
        binding.btnReset.setOnClickListener(v -> reset());
        binding.btnCopyFen.setOnClickListener(v -> copyFEN());

        btn_draw.setOnClickListener(v -> createDialog("Draw", "Are you sure you want to draw?", (d, i) -> {
            pgn.setTermination("Draw by agreement");
            terminateGame(ChessState.DRAW);
        }));
        btn_resign.setOnClickListener(v -> createDialog("Resign", "Are you sure you want to resign?", (d, i) -> {
            pgn.setTermination(opponentPlayer(playerToPlay()).getName() + " won by Resignation");
            terminateGame(ChessState.RESIGN);
        }));
        btn_undo_move.setOnClickListener(v -> undoLastMove());

        if (newGame) reset();
        else updateAll();

        if (!timerEnabled) {
            whiteTimeLayout.setVisibility(View.GONE);
            blackTimeLayout.setVisibility(View.GONE);
        } else chessTimer.startTimer();

        if (loadingPGN) {
            pgn.setCommentsMap(commentsMap);
            pgn.setMoveAnnotationMap(moveAnnotationMap);
            pgn.setAlternateMoveSequence(alternateMoveSequence);
            long start = System.nanoTime();
            boolean result = parsePGN();
            long end = System.nanoTime();

            HomeFragment.printTime(TAG, "loading PGN moves", end - start, moves.size());
            if (result) Log.i(TAG, "onViewCreated: Game valid");
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeData() {
        gameTerminated = false;
        loadingPGN = false;
        FEN = "";

        timerEnabled = dataManager.isTimerEnabled();
        vibrationEnabled = dataManager.getVibration();

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
            boardModel = (BoardModel) dataManager.readObject(DataManager.BOARD_FILE);
            pgn = (PGN) dataManager.readObject(DataManager.PGN_FILE);
            boardModelStack = (Stack<BoardModel>) dataManager.readObject(DataManager.STACK_FILE);
            FENs = (LinkedList<String>) dataManager.readObject(DataManager.FENS_LIST_FILE);
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel(requireContext(), true);
            boardModelStack = new Stack<>();
            FENs = new LinkedList<>();
            boardModelStack.push(boardModel);
            FENs.push(boardModel.toFEN());
            pgn = new PGN(PGN.APP_NAME, white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
            if (timerEnabled) chessTimer = new ChessTimer(this);
        }

        gameState = pgn.getGameState();         //Get previous state from PGN
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names

        whiteName.setText(white);
        blackName.setText(black);

        chessBoard.boardInterface = this;
        chessBoard.setTheme(dataManager.getBoardTheme());
        chessBoard.invalidate = true;

        vibrator = (Vibrator) requireContext().getSystemService(VIBRATOR_SERVICE);
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
        fromSquare = "";
        toSquare = "";
        pushToStack();
        Log.v(TAG, "reset: New PGN created " + date);
        updateAll();
    }

    private boolean parsePGN() {
        char ch;
        int i, startRow, startCol, destRow, destCol;
        boolean promotion;
        Rank rank = null, promotionRank;
        Piece piece;
        Player player;

        for (String move : moves) {
            move = move.trim();
            Log.d(TAG, "parsePGN: Move: " + move);
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
                    King king = gameState == ChessState.WHITE_TO_PLAY ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canShortCastle(this))
                        movePiece(king.getRow(), king.getCol(), king.getRow(), king.getCol() + 2);
                    continue;
                } else if (move.equals(PGN.LONG_CASTLE)) {
                    King king = gameState == ChessState.WHITE_TO_PLAY ? boardModel.getWhiteKing() : boardModel.getBlackKing();
                    if (king.canLongCastle(this))
                        movePiece(king.getRow(), king.getCol(), king.getRow(), king.getCol() - 2);
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
                            Log.d(TAG, "parsePGN: Promotion");
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
                            Log.d(TAG, "parsePGN: Promotion rank: " + promotionRank);
                            if (promotionRank == null) return false;
                            break label;

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
                    if (promote(pawn, destRow, destCol, startRow, startCol, promotionRank))
                        Log.d(TAG, String.format("parsePGN: Promoted to %s at %s", promotionRank, toNotation(destRow, destCol)));
                }

                if (piece != null) {
                    if (movePiece(piece.getRow(), piece.getCol(), destRow, destCol))
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

                        if (piece != null && movePiece(piece.getRow(), piece.getCol(), destRow, destCol))
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
                    Toast.makeText(requireContext(), "Invalid move " + move, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error occurred after move " + move, Toast.LENGTH_LONG).show();
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
                boardModel.fromSquare = toNotation(fromRow, fromCol);
                boardModel.toSquare = toNotation(toRow, toCol);
                if (movingPiece.getRank() == Rank.PAWN) if (Math.abs(fromRow - toRow) == 2) {
                    Pawn enPassantPawn = (Pawn) movingPiece;
                    boardModel.enPassantPawn = enPassantPawn;
                    boardModel.enPassantSquare = toNotation(enPassantPawn.getRow() - enPassantPawn.direction, enPassantPawn.getCol());
                    Log.d(TAG, "movePiece: EnPassantPawn: " + boardModel.enPassantPawn.getPosition() + " EnPassantSquare: " + boardModel.enPassantSquare);
                } else {
                    boardModel.enPassantPawn = null;
                    boardModel.enPassantSquare = "";
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

    public void saveGame() {
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
        PromoteDialog promoteDialog = new PromoteDialog(requireContext());
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
            Log.v(TAG, "promote: Promoted to " + rank);
            fromSquare = toNotation(fromRow, fromCol);
            toSquare = toNotation(row, col);
            toggleGameState();
            pushToStack();
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
            fromSquare = toNotation(fromRow, fromCol);
            toSquare = toNotation(row, col);
            toggleGameState();
            pushToStack();
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
            updateAll();
        }
    }

    /**
     * Updates all necessary fields and views
     */
    private void updateAll() {
        saveGame();
        long start, end;

        count = 0;
        start = System.nanoTime();
        computeLegalMoves();
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

        if (btn_draw.isEnabled()) btn_draw.setAlpha(1f);
        else btn_draw.setAlpha(0.5f);

        chessBoard.invalidate();
        Log.i(TAG, "updateAll: Updated and saved game");
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
            Toast.makeText(requireContext(), termination, Toast.LENGTH_SHORT).show();
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
        setGameState(gameState);
        if (!loadingPGN && dataManager.deleteGameFiles())
            Log.d(TAG, "deleteGameFiles: Game files deleted successfully!");
        gameTerminated = true;

        if (!loadingPGN) {
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
            String name = "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn";
            if (dataManager.savePGN(pgn, name))
                Log.d(TAG, "terminateGame: Game PGN saved successfully!");
        }

        if (timerEnabled && chessTimer != null) chessTimer.stopTimer();
        chessBoard.invalidate();
        btn_resign.setEnabled(false);
        btn_draw.setEnabled(false);
        Log.i(TAG, "terminateGame: Game terminated by: " + gameState);
        showGameOverDialog();
    }

    private void showGameOverDialog() {
        if (loadingPGN) return;
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

    private boolean drawByRepetition() {
        int i = 0, j, l = FENs.size();
        String[] positions = new String[l];
        for (String FEN : FENs) positions[l - i++ - 1] = FEN;

        String lastPosition = positions[l - 1];
        for (i = 0; i < l - 2; i++)
            if (lastPosition.equals(positions[i])) {
                Log.d(TAG, String.format("drawByRepetition: One repetition found:\n%d: %s\n%d: %s", i / 2 + 1, positions[i], (l - 1) / 2 + 1, lastPosition));
                for (j = i + 1; j < l - 1; j++)
                    if (positions[i].equals(positions[j])) {
                        Log.i(TAG, "Draw by repetition");
                        Log.i(TAG, String.format("Position : %d, %d & %d", i / 2 + 1, j / 2 + 1, (l - 1) / 2 + 1));
                        Log.i(TAG, String.format("Repeated moves FEN:\n%d - %s\n%d - %s\n%d - %s", i / 2 + 1, positions[i], j / 2 + 1, positions[j], (l - 1) / 2 + 1, lastPosition));
                        return true;
                    }
                positions[i] = "";
            }
        return false;
    }

    private void createDialog(String title, String message, DialogInterface.OnClickListener positiveOnClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title).setMessage(message).setPositiveButton("Yes", positiveOnClickListener).setNegativeButton("No", (d, i) -> d.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(d -> {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor("#EFEFEF"));
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#EFEFEF"));
        });
        alertDialog.show();
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
        tempBoardInterface.movePiece(row, col, toRow, toCol);
        if (piece.isWhite())
            isChecked = tempBoardInterface.tempBoardModel.getWhiteKing().isChecked(tempBoardInterface);
        else
            isChecked = tempBoardInterface.tempBoardModel.getBlackKing().isChecked(tempBoardInterface);
        return isChecked;
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
        FENs.push(boardModel.toFEN());
        boardModel.fromSquare = fromSquare;
        boardModel.toSquare = toSquare;
        fromSquare = "";
        toSquare = "";
        updateAll();
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
    public static String toNotation(int position) {
        return "" + (char) ('a' + position % 8) + (position / 8 + 1);
    }

    /**
     * Converts row and column numbers to Standard Notation
     */
    public static String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.show();
        }
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
            Piece opponentPiece = pieceAt(toRow, toCol), movingPiece = pieceAt(fromRow, fromCol);
            if (movingPiece != null) movingPiece.moveTo(toRow, toCol);
            else Log.e("TempBoardInterface", "movePiece: Error! movingPiece is null");
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