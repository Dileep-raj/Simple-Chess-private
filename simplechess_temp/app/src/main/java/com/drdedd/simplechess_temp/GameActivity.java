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
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;


@SuppressLint({"SimpleDateFormat", "NewApi"})
public class GameActivity extends AppCompatActivity implements BoardInterface {
    private final String TAG = "GameActivity";
    private final String boardFile = "boardFile", PGNFile = "PGNFile";
    private String white = "White", black = "Black";
    private PGN pgn;
    private BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private TextView PGN_textView, gameStateView, whiteName, blackName;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private BoardTheme theme;
    private String[] permissions;
    private SimpleDateFormat pgnDate;
    private static ChessState gameState;

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

        findViewById(R.id.btn_save_exit).setOnClickListener(view -> save_Exit());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(view -> copyPGN());
        findViewById(R.id.btn_export_pgn).setOnClickListener(view -> exportPGN());
        findViewById(R.id.btn_reset).setOnClickListener(view -> reset());

        initializeData();
        Bundle extras = getIntent().getExtras();
        if (extras != null) if (extras.getBoolean("newGame")) reset();

    }

    private void initializeData() {
        white = dataManager.getWhite();
        black = dataManager.getBlack();

        pgnDate = new SimpleDateFormat("dd/MM/yyyy");

        boardModel = (BoardModel) dataManager.readObject(boardFile);
        pgn = (PGN) dataManager.readObject(PGNFile);

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel();
            pgn = new PGN(new StringBuilder(), 0, "Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITETOPLAY);
        }

        gameState = pgn.getGameState();         //Get previous state from PGN
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names
        PGN_textView.setText(pgn.getPGN());     //Update PGN in TextView
        theme = dataManager.getBoardTheme();    //Get theme from DataManager

        whiteName.setText(white);
        blackName.setText(black);

        chessBoard.setTheme(theme);
        chessBoard.boardInterface = this;

        showGameStateView();

        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        save_Exit();
    }

    @Override
    public Piece pieceAt(int row, int col) {
        return boardModel.pieceAt(row, col);
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        boolean result = chessBoard.movePiece(fromRow, fromCol, toRow, toCol);
        if (result) chessBoard.invalidate();
        return result;
    }

    public void save_Exit() {
        dataManager.saveObject(boardFile, boardModel);
        dataManager.saveObject(PGNFile, pgn);
        dataManager.saveData(theme);
        finish();
    }

    public void addToPGN(Piece piece) {
        pgn.addToPGN(piece);
        toggleGameState();
        pgn.setGameState(gameState);
        PGN_textView.setText(pgn.getPGN());
        horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
    }


    public void reset() {
        boardModel.resetBoard();        //Reset the board to initial state
        pgn = new PGN(new StringBuilder(), 0, "Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITETOPLAY);
        Log.d(TAG, "reset: New PGN created " + pgnDate.format(new Date()));
        pgn.resetPGN();
        gameState = pgn.getGameState();
        PGN_textView.setText(pgn.getPGN());
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

    public void copyPGN() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("PGN", pgn.toString());
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public boolean isPieceToPlay(@NonNull Piece piece) {
        return piece.getPlayerType() == Player.WHITE && getGameState() == ChessState.WHITETOPLAY || piece.getPlayerType() == Player.BLACK && getGameState() == ChessState.BLACKTOPLAY;
    }

    @Override
    public void removePiece(Piece piece) {
        boardModel.removePiece(piece);
    }

//    public void setGameState(ChessState gameState) { GameActivity.gameState = gameState; }

    public static ChessState getGameState() {
        return gameState;
    }

    public void toggleGameState() {
        if (gameState == ChessState.WHITETOPLAY) gameState = ChessState.BLACKTOPLAY;
        else if (gameState == ChessState.BLACKTOPLAY) gameState = ChessState.WHITETOPLAY;
        showGameStateView();
    }

    @SuppressLint("SetTextI18n")
    private void showGameStateView() {
        if (gameState == ChessState.WHITETOPLAY) gameStateView.setText(white);
        else if (gameState == ChessState.BLACKTOPLAY) gameStateView.setText(black);
    }
}