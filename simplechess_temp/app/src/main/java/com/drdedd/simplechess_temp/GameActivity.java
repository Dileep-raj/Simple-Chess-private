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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Stack;

@SuppressLint({"SimpleDateFormat", "NewApi"})
public class GameActivity extends AppCompatActivity implements BoardInterface {
    private final String TAG = "GameActivity";
    private String white = "White", black = "Black";
    private PGN pgn;
    private BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private Button btn_undo_move;
    private TextView PGN_textView, gameStateView, whiteName, blackName;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private BoardTheme theme;
    private String[] permissions;
    private SimpleDateFormat pgnDate;
    private static ChessState gameState;
    private boolean newGame;
    private Stack<BoardModel> boardModelStack;
    private ClipboardManager clipboard;

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

        findViewById(R.id.btn_save_exit).setOnClickListener(view -> finish());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(view -> copyPGN());
        findViewById(R.id.btn_export_pgn).setOnClickListener(view -> exportPGN());
        findViewById(R.id.btn_reset).setOnClickListener(view -> reset());
        findViewById(R.id.btn_copy_fen).setOnClickListener(view -> copyFEN());
        btn_undo_move.setOnClickListener(view -> undoLastMove());

        btn_undo_move.setEnabled(pgn.lastMove() != null);

        if (newGame) reset();
    }

    private void initializeData() {
        white = dataManager.getWhite();
        black = dataManager.getBlack();

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

        chessBoard.boardInterface = this;
        chessBoard.setTheme(theme);
        chessBoard.isChecked();

        showGameStateView();

        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveGame();
    }

    @Override
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
            boolean result = chessBoard.movePiece(fromRow, fromCol, toRow, toCol);
            if (result) {
                toggleGameState();
                chessBoard.invalidate();
                Piece movingPiece = pieceAt(toRow, toCol);
                if (movingPiece != null) if (movingPiece.getRank() == Rank.PAWN)
                    if (Math.abs(fromRow - toRow) == 2)
                        boardModel.enPassantPawn = (Pawn) movingPiece;
                    else boardModel.enPassantPawn = null;

                saveGame();
                pushToStack();
                chessBoard.isChecked();
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

    public void addToPGN(Piece piece, String move) {
        pgn.addToPGN(piece, move);
        updatePGNView();
    }

    public void reset() {
        boardModel = new BoardModel(getApplicationContext());
        pgn = new PGN("Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITE_TO_PLAY);
        boardModelStack = new Stack<>();
        pushToStack();
        Log.d(TAG, "reset: New PGN created " + pgnDate.format(new Date()));
//        Log.d(TAG, "reset: initial BoardModel in stack: " + boardModel);
        gameState = pgn.getGameState();
        PGN_textView.setText(pgn.getPGN());
        btn_undo_move.setEnabled(pgn.lastMove() != null);
        showGameStateView();
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
    public void promote(Pawn pawn, int row, int col) {
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
            addToPGN(pawn, piece.getPosition().substring(1) + piece.getPosition().charAt(0));
            chessBoard.isChecked();
            chessBoard.invalidate();
        });
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
        showGameStateView();
    }

    @SuppressLint("SetTextI18n")
    private void showGameStateView() {
        if (gameState == ChessState.WHITE_TO_PLAY) gameStateView.setText(white);
        else if (gameState == ChessState.BLACK_TO_PLAY) gameStateView.setText(black);
    }

    private void undoLastMove() {
        pgn.removeLast();
        if (boardModelStack.size() >= 2) {
            boardModelStack.pop();
            toggleGameState();
            updatePGNView();
        }

        boardModel = boardModelStack.peek().clone();
        btn_undo_move.setEnabled(pgn.lastMove() != null);
        chessBoard.invalidate();
        chessBoard.isChecked();
        saveGame();
    }

    private void pushToStack() {
        boardModelStack.push(boardModel.clone());
    }

    private void updatePGNView() {
        PGN_textView.setText(pgn.getPGN());
        horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
    }

    public BoardModel getBoardModel() {
        return boardModel;
    }
}