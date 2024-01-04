package com.drdedd.simplechess_temp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * {@inheritDoc}
 */
@SuppressLint({"SimpleDateFormat", "NewApi"})
public class GameActivity extends AppCompatActivity implements BoardInterface {
    private final String TAG = "GameActivity";
    protected String white = "White", black = "Black";
    public PGN pgn;
    protected BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private Button btn_undo_move;
    private TextView PGN_textView, gameStateView, whiteName, blackName;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private BoardTheme theme;
    private String[] permissions;
    private SimpleDateFormat pgnDate;
    private static ChessState gameState;
    protected boolean newGame, gameTerminated;
    protected Stack<BoardModel> boardModelStack;
    protected ClipboardManager clipboard;
    protected HashMap<Piece, HashSet<Integer>> legalMoves;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveGame();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        dataManager = new DataManager(this);
        if (dataManager.isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            Objects.requireNonNull(getSupportActionBar()).hide();   //Hide the action bar
            View decorView = getWindow().getDecorView();            // Hide the status bar.
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        chessBoard = findViewById(R.id.chessBoard);
        PGN_textView = findViewById(R.id.pgn_textview);
        horizontalScrollView = findViewById(R.id.scrollView);
        gameStateView = findViewById(R.id.gameStateView);
        whiteName = findViewById(R.id.whiteNameTV);
        blackName = findViewById(R.id.blackNameTV);
        btn_undo_move = findViewById(R.id.btn_undo_move);

        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        Bundle extras = getIntent().getExtras();
        if (extras != null) newGame = extras.getBoolean("newGame");
        initializeData();
//        gameLogic = new GameLogic(getApplicationContext(), chessBoard);

        findViewById(R.id.btn_save_exit).setOnClickListener(view -> finish());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(view -> copyPGN());
        findViewById(R.id.btn_export_pgn).setOnClickListener(view -> exportPGN());
        findViewById(R.id.btn_reset).setOnClickListener(view -> reset());
        findViewById(R.id.btn_copy_fen).setOnClickListener(view -> copyFEN());
        btn_undo_move.setOnClickListener(view -> undoLastMove());

        btn_undo_move.setEnabled(pgn.lastMove() != null);

        if (newGame) reset();
//        Log.d(TAG, "onCreate: PGN: " + pgn.hashCode());
        updateAll();
    }

    private void initializeData() {
        gameTerminated = false;

        white = dataManager.getWhite();
        Player.WHITE.setName(white);
        black = dataManager.getBlack();
        Player.BLACK.setName(black);

        pgnDate = new SimpleDateFormat("yyyy.MM.dd");

        if (!newGame) {
            boardModel = (BoardModel) dataManager.readObject(DataManager.boardFile);
            pgn = (PGN) dataManager.readObject(DataManager.PGNFile);
            boardModelStack = (Stack<BoardModel>) dataManager.readObject(DataManager.stackFile);
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel(getApplicationContext());
            boardModelStack = new Stack<>();
            boardModelStack.push(boardModel);
            pgn = new PGN("Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
//            reset();
        }

        gameState = pgn.getGameState();         //Get previous state from PGN
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names
        PGN_textView.setText(pgn.getPGN());     //Update PGN in TextView
        theme = dataManager.getBoardTheme();    //Get theme from DataManager

        whiteName.setText(white);
        blackName.setText(black);

//        GameLogic.boardInterface = this;
        chessBoard.boardInterface = this;
        chessBoard.setTheme(theme);
        isChecked();

        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        Pawn pawn = unPromotedPawn();
        if (pawn != null) promote(pawn, pawn.getRow(), pawn.getCol(), -1, -1);

    }

    private Pawn unPromotedPawn() {
        int rank = (gameState.equals(ChessState.WHITE_TO_PLAY)) ? 0 : 7;
        Piece piece;
        for (int i = 0; i < 8; i++)
            if ((piece = boardModel.pieceAt(rank, i)) != null)
                if (piece.getRank() == Rank.PAWN) return (Pawn) piece;
        return null;
    }

    //    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
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

            if (dataManager.computeLegalMovesEnabled()) if (isPieceToPlay(movingPiece))
                if (!legalMoves.get(movingPiece).contains(toCol + toRow * 8)) return false;
            boolean result = chessBoard.movePiece(fromRow, fromCol, toRow, toCol);
            if (result) {
                chessBoard.invalidate();
                if (movingPiece.getRank() == Rank.PAWN) if (Math.abs(fromRow - toRow) == 2)
                    boardModel.enPassantPawn = (Pawn) movingPiece;
                else boardModel.enPassantPawn = null;
                HashSet<Integer> moves = legalMoves.get(movingPiece);
                Log.d(TAG, "movePiece: Legal Moves HashSet: " + legalMoves.size() + " Piece: " + legalMoves.containsKey(movingPiece));
                if (moves != null)
                    Log.d(TAG, "movePiece: " + (moves.contains(toRow * 8 + toCol) ? "Legal Move " : "Illegal Move " + movingPiece.getPosition()));
                toggleGameState();
                saveGame();
                pushToStack();
                isChecked();
                if (Player.WHITE.isInCheck() || Player.BLACK.isInCheck()) printLegalMoves();
            }
            btn_undo_move.setEnabled(pgn.lastMove() != null);
            return result;
        }
    }

    public void saveGame() {
        dataManager.saveObject(DataManager.boardFile, boardModel);
        dataManager.saveObject(DataManager.PGNFile, pgn);
        dataManager.saveObject(DataManager.stackFile, boardModelStack);
        dataManager.saveData(theme);
    }

    public void addToPGN(Piece piece, String move, int fromRow, int fromCol) {
        String position, capture = "";
        StringBuilder moveStringBuilder = new StringBuilder(piece.getPosition());
        position = toNotation(fromRow, fromCol);
        if (move.contains(PGN.capture)) capture = "x";
        moveStringBuilder.insert(1, position + capture);

        if (move.equals(PGN.longCastle) || move.equals(PGN.shortCastle))
            moveStringBuilder = new StringBuilder(move);
        pgn.addToPGN(piece, moveStringBuilder.toString());
        updatePGNView();
    }

    public void reset() {
        boardModel = new BoardModel(getApplicationContext());
        pgn = new PGN("Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
        boardModelStack = new Stack<>();
        pushToStack();
        Log.d(TAG, "reset: New PGN created " + pgnDate.format(new Date()));
        Log.d(TAG, "reset: initial BoardModel in stack: " + boardModel);
        gameState = pgn.getGameState();
        PGN_textView.setText(pgn.getPGN());
        btn_undo_move.setEnabled(pgn.lastMove() != null);
        updateAll();
        chessBoard.invalidate();
    }

    public void exportPGN() {
        if (checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            try {
                String dir = pgn.exportPGN();
                Toast.makeText(this, "PGN saved in " + dir, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "File not saved!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "exportPGN: \n" + e);
            }
        } else {
            Toast.makeText(this, "Write permission is required to export PGN file", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }

    private void copyFEN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("FEN", boardModel.toFEN()));
    }

    private void copyPGN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("PGN", pgn.toString()));
    }

    @Override
    public void removePiece(Piece piece) {
        boardModel.removePiece(piece);
    }

    @Override
    public void promote(Pawn pawn, int row, int col, int fromRow, int fromCol) {
        PromoteDialog promoteDialog = new PromoteDialog(this);
        promoteDialog.show();

//        Set image buttons as respective color pieces
        promoteDialog.findViewById(R.id.promote_to_queen).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.QUEEN.toString()));
        promoteDialog.findViewById(R.id.promote_to_rook).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.ROOK.toString()));
        promoteDialog.findViewById(R.id.promote_to_bishop).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.BISHOP.toString()));
        promoteDialog.findViewById(R.id.promote_to_knight).setBackgroundResource(boardModel.resIDs.get(pawn.getPlayer() + Rank.KNIGHT.toString()));

//        Invalidate chess board to show new promoted piece
        promoteDialog.setOnDismissListener(dialogInterface -> {
            Piece piece = boardModel.promote(pawn, promoteDialog.getRank(), row, col);
//            addToPGN(pawn, pawn.getPosition().charAt(1) + "=" + piece.getPosition().substring(1) + piece.getPosition().charAt(0));
            addToPGN(pawn, piece.getPosition().substring(1) + piece.getPosition().charAt(0), -1, -1);
            updateAll();
            isChecked();
            chessBoard.invalidate();
//            this.return true;
        });
    }

    @Override
    public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
        return legalMoves;
    }

    public void setGameState(ChessState gameState) {
        GameActivity.gameState = gameState;
    }

    public static ChessState getGameState() {
        return gameState;
    }

    private void toggleGameState() {
        if (gameState == ChessState.WHITE_TO_PLAY) gameState = ChessState.BLACK_TO_PLAY;
        else if (gameState == ChessState.BLACK_TO_PLAY) gameState = ChessState.WHITE_TO_PLAY;
//        Log.d(TAG, "toggleGameState: GameState toggled");
        pgn.setGameState(gameState);
        updateAll();
    }

    @SuppressLint("SetTextI18n")
    void updateGameStateView() {
        gameStateView.setText(playerToPlay().getName());
    }

    private void undoLastMove() {
        pgn.removeLast();
        if (boardModelStack.size() >= 2) {
            boardModelStack.pop();
            toggleGameState();
        }

        boardModel = boardModelStack.peek().clone();
        updateAll();
        btn_undo_move.setEnabled(pgn.lastMove() != null);
        chessBoard.invalidate();
        isChecked();
        saveGame();
    }

    private void updateAll() {
        updateLegalMoves();
        updatePGNView();
        updateGameStateView();
        checkGameTermination();
    }

    private void checkGameTermination() {
        Log.d(TAG, "checkGameTermination: Checking for Termination");
        Set<Map.Entry<Piece, HashSet<Integer>>> pieces = legalMoves.entrySet();
        Iterator<Map.Entry<Piece, HashSet<Integer>>> piecesIterator = pieces.iterator();
        while (piecesIterator.hasNext()) {
            Map.Entry<Piece, HashSet<Integer>> entry = piecesIterator.next();
            if (!entry.getValue().isEmpty()) break;
        }
        if (!piecesIterator.hasNext()) {
            gameTerminated = true;
            String termination;
            ChessState terminationState;
            Log.d(TAG, "checkGameTermination: No Legal Moves for: " + playerToPlay());
            isChecked();

            if (!playerToPlay().isInCheck()) {
                termination = "Draw by Stalemate";
                terminationState = ChessState.STALEMATE;
            } else {
                termination = opponentPlayer(playerToPlay()).getName() + " wins by Checkmate ";
                terminationState = ChessState.CHECKMATE;
            }

            pgn.setTermination(termination);
            Log.d(TAG, "checkGameTermination: " + termination);
            Toast.makeText(this, termination, Toast.LENGTH_LONG).show();
            terminateGame(terminationState);
        }
    }

    private void terminateGame(ChessState gameState) {
        setGameState(gameState);
        GameOverDialog gameOverDialog = new GameOverDialog(this, pgn);
        gameOverDialog.show();

        gameOverDialog.setOnDismissListener(dialogInterface -> finish());
    }

    public void isChecked() {
        King whiteKing = boardModel.getWhiteKing();
        King blackKing = boardModel.getBlackKing();
        Player.WHITE.setInCheck(false);
        Player.BLACK.setInCheck(false);
        if (whiteKing.isChecked(this)) {
            Player.WHITE.setInCheck(true);
            Log.d(TAG, "isChecked: White King checked");
        }
        if (blackKing.isChecked(this)) {
            Player.BLACK.setInCheck(true);
            Log.d(TAG, "isChecked: Black King checked");
        }
    }

    private void pushToStack() {
        boardModelStack.push(boardModel.clone());
    }

    void updatePGNView() {
        PGN_textView.setText(pgn.getPGN());
        horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
    }

    public BoardModel getBoardModel() {
        return boardModel;
    }

    private void updateLegalMoves() {
        legalMoves = computeLegalMoves();
        Log.d(TAG, "updateLegalMoves: Updated Legal Moves");
    }

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
                Log.d(TAG, "movePiece: Legal Moves for " + piece.getPosition() + ": " + allMoves);
            } else Log.d(TAG, "movePiece: No legal moves for " + piece.getPosition());
        }
    }

    public static Player opponentPlayer(Player player) {
        return player == Player.WHITE ? Player.BLACK : Player.WHITE;
    }

    public static Player playerToPlay() {
        return gameState == ChessState.WHITE_TO_PLAY ? Player.WHITE : Player.BLACK;
    }

    public static boolean isPieceToPlay(@NonNull Piece piece) {
        return piece.getPlayer() == playerToPlay();
    }


    private HashMap<Piece, HashSet<Integer>> computeLegalMoves() {
        legalMoves = new HashMap<>();
        HashSet<Piece> pieces = boardModel.pieces;
        for (Piece piece : pieces) {
            if (!isPieceToPlay(piece)) continue;
            HashSet<Integer> possibleMoves = piece.getPossibleMoves(this), illegalMoves = new HashSet<>();
            for (int move : possibleMoves)
                if (isIllegalMove(piece, move)) {
//                    Log.d(TAG, "getLegalMoves:" + piece.getPlayer() + " Illegal Move " + piece.getPosition() + "->" + toNotation(move));
                    illegalMoves.add(move);
                }
//                else
//                    Log.d(TAG, "getLegalMoves:" + piece.getPlayer() + " Legal Move " + piece.getPosition() + "->" + toNotation(move));
            possibleMoves.removeAll(illegalMoves);
            legalMoves.put(piece, possibleMoves);
        }
        return legalMoves;
    }

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

    private int toCol(int position) {
        return position % 8;
    }

    private int toRow(int position) {
        return position / 8;
    }

    static String toNotation(int position) {
        return "" + (char) ('a' + position % 8) + (position / 8 + 1);
    }

    static String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }

    public static char colToChar(int col) {
        return (char) ('a' + col);
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
            tempPiece.moveTo(toRow, toCol);
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
}