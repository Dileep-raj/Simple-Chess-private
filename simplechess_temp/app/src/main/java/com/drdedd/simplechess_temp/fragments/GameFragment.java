package com.drdedd.simplechess_temp.fragments;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drdedd.simplechess_temp.BoardInterface;
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

@SuppressLint("NewApi")
public class GameFragment extends Fragment implements BoardInterface, View.OnClickListener {
    private final static String TAG = "GameFragment";
    private FragmentGameBinding binding;
    private String white = "White", black = "Black", termination;
    public PGN pgn;
    protected BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private ImageButton btn_undo_move, btn_resign;
    private TextView PGN_textView, gameStateView, whiteName, blackName;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    //    private String[] permissions;
    private SimpleDateFormat pgnDate;
    private static ChessState gameState;
    private static boolean newGame, gameTerminated;
    protected Stack<BoardModel> boardModelStack;
    protected ClipboardManager clipboard;
    protected HashMap<Piece, HashSet<Integer>> legalMoves;
    private LinkedList<String[]> FENs;
    private boolean timerEnabled;
    public TextView whiteTimeTV, blackTimeTV;
    public LinearLayout whiteTimeLayout, blackTimeLayout;
    private LinearLayout whiteCaptured, blackCaptured;
    private final ViewGroup.LayoutParams pieceImageViewParams = new ViewGroup.LayoutParams(40, 40);
    private ChessTimer chessTimer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chessBoard = binding.chessBoard;
        btn_undo_move = binding.btnUndoMove;
        btn_resign = binding.btnResign;
        PGN_textView = binding.pgnTextview;
        gameStateView = binding.gameStateView;
        whiteName = binding.whiteNameTV;
        blackName = binding.blackNameTV;
        horizontalScrollView = binding.scrollView;

        whiteTimeTV = binding.whiteTimeTV;
        blackTimeTV = binding.blackTimeTV;
        whiteTimeLayout = binding.whiteTimeLayout;
        blackTimeLayout = binding.blackTimeLayout;
        whiteCaptured = binding.whiteCapturedPieces;
        blackCaptured = binding.blackCapturedPieces;

        dataManager = new DataManager(requireContext());
        initializeData();

        timerEnabled = dataManager.isTimerEnabled();

        clipboard = (ClipboardManager) requireActivity().getSystemService(CLIPBOARD_SERVICE);

        newGame = requireArguments().getBoolean(HomeFragment.NEW_GAME_KEY);
        if (!newGame && isGameTerminated()) terminateGame(null);
        initializeData();

        binding.btnSaveExit.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCopyPgn.setOnClickListener(v -> copyPGN());
        binding.btnExportPgn.setOnClickListener(v -> exportPGN());
        binding.btnReset.setOnClickListener(v -> reset());
        binding.btnCopyFen.setOnClickListener(v -> copyFEN());

        btn_resign.setOnClickListener(v -> resign());
        btn_undo_move.setOnClickListener(v -> undoLastMove());

        if (newGame) reset();
        updateAll();
        if (!timerEnabled) {
            whiteTimeLayout.setVisibility(View.GONE);
            blackTimeLayout.setVisibility(View.GONE);
        } else chessTimer.startTimer();
    }

    @SuppressWarnings("unchecked")
    private void initializeData() {
        gameTerminated = false;

        white = dataManager.getWhite();
        Player.WHITE.setName(white);
        black = dataManager.getBlack();
        Player.BLACK.setName(black);

        pgnDate = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);
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

//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
//            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        else permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
    }

    public void reset() {
        dataManager.setGameTerminationMessage("");
        dataManager.setGameTerminated(false);
        if (timerEnabled) chessTimer.resetTimer();

        gameTerminated = false;
        boardModel = new BoardModel(requireContext(), true);
        pgn = new PGN(PGN.APP_NAME, white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
        boardModelStack = new Stack<>();
        FENs = new LinkedList<>();
        pushToStack();
        Log.v(TAG, "reset: New PGN created " + pgnDate.format(new Date()));
        Log.v(TAG, "reset: initial BoardModel in stack: " + boardModel);
        gameState = pgn.getGameState();
        PGN_textView.setText(pgn.getPGN());
        updateAll();
        chessBoard.invalidate();
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
                    else boardModel.removePiece(toPiece);
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
                chessBoard.invalidate();
                if (movingPiece.getRank() == Rank.PAWN) if (Math.abs(fromRow - toRow) == 2)
                    boardModel.enPassantPawn = (Pawn) movingPiece;
                else boardModel.enPassantPawn = null;
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
        updatePGNView();
    }

    /**
     * Export PGN to a file with <code>.pgn</code> extension
     */
    private void exportPGN() {
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

    @Override
    public void removePiece(Piece piece) {
        boardModel.removePiece(piece);
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
                    removePiece(tempPiece);
                    addToPGN(promotedPiece, PGN.PROMOTE + PGN.CAPTURE, fromRow, fromCol);
                }
            } else addToPGN(promotedPiece, PGN.PROMOTE, fromRow, fromCol);
            Log.v(TAG, "promote: Promoted to " + rank);
            pushToStack();
            toggleGameState();
            chessBoard.invalidate();
        });
    }

    @Override
    public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
        return legalMoves;
    }

    @Override
    public BoardModel getBoardModel() {
        return boardModel;
    }

    private void setGameState(ChessState gameState) {
        GameFragment.gameState = gameState;
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

    @SuppressLint("SetTextI18n")
    void updateGameStateView() {
        if (gameTerminated) gameStateView.setText(termination);
        else gameStateView.setText(playerToPlay().getName() + "'s turn");
    }

    private void undoLastMove() {
        if (gameTerminated) return;
        pgn.removeLast();
        if (boardModelStack.size() >= 2) {
            boardModelStack.pop();
            FENs.pop();
            boardModel = boardModelStack.peek().clone();
            toggleGameState();
        }
        chessBoard.invalidate();
    }

    /**
     * Updates all necessary fields and views
     */
    private void updateAll() {
        isChecked();
        long start = System.nanoTime();
        updateLegalMoves();
        long end = System.nanoTime();
        GameFragment.printTime(TAG, "updating LegalMoves", end - start);
        checkGameTermination();
        updatePGNView();
        updateGameStateView();
        updateCapturedPieces();

        btn_undo_move.setEnabled(boardModelStack.size() > 1);
        if (gameTerminated) btn_undo_move.setEnabled(false);

        if (btn_undo_move.isEnabled()) btn_undo_move.setAlpha(1f);
        else btn_undo_move.setAlpha(0.5f);

        if (btn_resign.isEnabled()) btn_resign.setAlpha(1f);
        else btn_resign.setAlpha(0.5f);

        saveGame();
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

//        Set<Map.Entry<Piece, HashSet<Integer>>> pieces = legalMoves.entrySet();
//        Iterator<Map.Entry<Piece, HashSet<Integer>>> piecesIterator = pieces.iterator();
//        while (piecesIterator.hasNext()) {
//            Map.Entry<Piece, HashSet<Integer>> entry = piecesIterator.next();
//            if (!entry.getValue().isEmpty()) break;
//        }
//        if (!piecesIterator.hasNext()) {
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
        dataManager.setGameTerminated(true);
//        if (timerEnabled) chessTimer.stopTimer();
        chessBoard.invalidate();
        gameTerminated = true;
        setGameState(gameState);
        btn_resign.setEnabled(false);
        Log.v(TAG, "terminateGame: Game terminated by: " + gameState);
//        dataManager.deleteGameFiles();
        GameOverDialog gameOverDialog = new GameOverDialog(requireContext(), pgn);
        gameOverDialog.show();

//        gameOverDialog.setOnDismissListener(dialogInterface -> finish());
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
        builder.setInverseBackgroundForced(true);
        builder.setMessage("Are you sure you want to resign?");
        builder.setTitle("Resign");
        builder.setPositiveButton("Yes", (dialog, i) -> {
            pgn.setTermination(opponentPlayer(playerToPlay()).getName() + " won by Resignation");
            terminateGame(ChessState.RESIGN);
        });
        builder.setNegativeButton("No", (dialog, i) -> dialog.cancel());
        builder.create().show();
    }

    /**
     * Checks if any of the player is checked
     */
    private void isChecked() {
        King whiteKing = boardModel.getWhiteKing();
        King blackKing = boardModel.getBlackKing();
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);
        if (whiteKing.isChecked(this)) {
            Player.WHITE.setInCheck(true);
            Log.v(TAG, "isChecked: White King checked");
        }
        if (blackKing.isChecked(this)) {
            Player.BLACK.setInCheck(true);
            Log.v(TAG, "isChecked: Black King checked");
        }
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
            HashSet<Integer> possibleMoves = piece.getPossibleMoves(this), illegalMoves = new HashSet<>();
            for (int move : possibleMoves)
                if (isIllegalMove(piece, move)) illegalMoves.add(move);

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

    private void updateCapturedPieces() {
        whiteCaptured.removeAllViews();
        blackCaptured.removeAllViews();
        ArrayList<Piece> capturedPieces = boardModel.getCapturedPieces();
        for (Piece piece : capturedPieces) {
            ImageView pieceImageView = createPieceView(piece);
            if (piece.isWhite()) whiteCaptured.addView(pieceImageView);
            else blackCaptured.addView(pieceImageView);
        }
    }

    private ImageView createPieceView(Piece piece) {
        ImageView imageView = new ImageView(requireContext());
        imageView.setImageResource(piece.getResID());
        imageView.setLayoutParams(pieceImageViewParams);
        return imageView;
    }

    private void updatePGNView() {
        PGN_textView.setText(pgn.getPGN());
        horizontalScrollView.post(() -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
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
     * Returns the current player to play
     *
     * @return <code>White|Black</code>
     */
    public static Player playerToPlay() {
        return gameState == ChessState.WHITE_TO_PLAY ? Player.WHITE : Player.BLACK;
    }

    public static ChessState getGameState() {
        return gameState;
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
    static String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    public static char colToChar(int col) {
        return (char) ('a' + col);
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
            if (opponentPiece != null) tempBoardModel.removePiece(opponentPiece);
            return true;
        }

        @Override
        public void addToPGN(Piece piece, String move, int fromRow, int fromCol) {
        }

        @Override
        public void removePiece(Piece piece) {
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

    public static void printTime(String TAG, String message, long time) {
        Log.i(TAG, String.format(Locale.ENGLISH, "printTime: Time calculated for %s: %,3d ns", message, time));
    }
}