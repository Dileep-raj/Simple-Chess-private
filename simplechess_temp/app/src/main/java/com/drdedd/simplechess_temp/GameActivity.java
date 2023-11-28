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
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Stack;


@SuppressLint({"SimpleDateFormat", "NewApi"})
public class GameActivity extends AppCompatActivity implements BoardInterface {
    private final String TAG = "GameActivity";
    public final String boardFile = "boardFile", PGNFile = "PGNFile", stackFile = "stackFile";
    private String white = "White", black = "Black";
    private PGN pgn;
    private BoardModel boardModel = null;
    private ChessBoard chessBoard;
    private Button btn_previous_move;
    private TextView PGN_textView, gameStateView, whiteName, blackName;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    private BoardTheme theme;
    private String[] permissions;
    private SimpleDateFormat pgnDate;
    private static ChessState gameState;
    private boolean newGame;
    private Stack<BoardModel> boardModelStack;

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
        btn_previous_move = findViewById(R.id.btn_previous_move);

        Bundle extras = getIntent().getExtras();
        if (extras != null) newGame = extras.getBoolean("newGame");
        initializeData();

        findViewById(R.id.btn_save_exit).setOnClickListener(view -> finish());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(view -> copyPGN());
        findViewById(R.id.btn_export_pgn).setOnClickListener(view -> exportPGN());
        findViewById(R.id.btn_reset).setOnClickListener(view -> reset());
        btn_previous_move.setOnClickListener(view -> previousMove());

        btn_previous_move.setEnabled(pgn.lastMove() != null);

        if (newGame) reset();
    }

    private void initializeData() {
        white = dataManager.getWhite();
        black = dataManager.getBlack();

        pgnDate = new SimpleDateFormat("yyyy.MM.dd");

        if (!newGame) {
            boardModel = (BoardModel) dataManager.readObject(boardFile);
            pgn = (PGN) dataManager.readObject(PGNFile);
            boardModelStack = (Stack<BoardModel>) dataManager.readObject(stackFile);
        }

        if (boardModel == null || pgn == null) {
            boardModel = new BoardModel();
            boardModelStack = new Stack<>();
            boardModelStack.push(boardModel);
            pgn = new PGN(new StringBuilder(), "Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITETOPLAY);
//            reset();
        }

        gameState = pgn.getGameState();         //Get previous state from PGN
        pgn.setWhiteBlack(white, black);        //Set the white and the black players' names
        PGN_textView.setText(pgn.getFinalPGN());     //Update PGN in TextView
        theme = dataManager.getBoardTheme();    //Get theme from DataManager

        whiteName.setText(white);
        blackName.setText(black);

        chessBoard.boardInterface = this;
        chessBoard.boardModel = boardModel;
        chessBoard.setTheme(theme);

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
                if (movingPiece != null)
                    if (movingPiece.getRank() == Rank.PAWN) if (Math.abs(fromRow - toRow) == 2) {
                        BoardModel.enPassantPawn = (Pawn) movingPiece;
//                        Log.d(TAG, "movePiece: En-passant Pawn: " + movingPiece.getPosition());
                    } else BoardModel.enPassantPawn = null;

                saveGame();
                pushToStack();
//                Log.d(TAG, "movePiece: Current FEN: " + toFEN());
//                Log.d(TAG, "movePiece: Stack count: " + boardModelStack.size());
            }
            btn_previous_move.setEnabled(pgn.lastMove() != null);
            updatePGNView();
            return result;
        }
    }

    public void saveGame() {
        dataManager.saveObject(boardFile, boardModel);
        dataManager.saveObject(PGNFile, pgn);
        dataManager.saveObject(stackFile, boardModelStack);
        dataManager.saveData(theme);
    }

    public void addToPGN(Piece piece, String move) {
        pgn.addToPGN(piece, move);
        updatePGNView();
    }

    public void reset() {
        boardModel = new BoardModel();
        pgn = new PGN(new StringBuilder(), "Simple chess", white, black, pgnDate.format(new Date()), ChessState.WHITETOPLAY);
        boardModelStack = new Stack<>();
        pushToStack();
        Log.d(TAG, "reset: New PGN created " + pgnDate.format(new Date()));
        Log.d(TAG, "reset: initial BoardModel in stack: " + boardModel);
//        pgn.resetPGN();
        gameState = pgn.getGameState();
        PGN_textView.setText(pgn.getFinalPGN());
        btn_previous_move.setEnabled(pgn.lastMove() != null);
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
            chessBoard.invalidate();
        });
    }

    //    public void setGameState(ChessState gameState) { GameActivity.gameState = gameState; }

    public static ChessState getGameState() {
        return gameState;
    }

    public void toggleGameState() {
        if (gameState == ChessState.WHITETOPLAY) gameState = ChessState.BLACKTOPLAY;
        else if (gameState == ChessState.BLACKTOPLAY) gameState = ChessState.WHITETOPLAY;
        Log.d(TAG, "toggleGameState: GameState toggled");
        pgn.setGameState(gameState);
        showGameStateView();
    }

    @SuppressLint("SetTextI18n")
    private void showGameStateView() {
        if (gameState == ChessState.WHITETOPLAY) gameStateView.setText(white);
        else if (gameState == ChessState.BLACKTOPLAY) gameStateView.setText(black);
    }

    private void previousMove() {
        String move = pgn.removeLast();
        Log.d(TAG, "previousMove: BoardModel current : " + boardModel);
        if (boardModelStack.size() >= 2) {
            boardModelStack.pop();
            Log.d(TAG, "previousMove: popped previous model");
            toggleGameState();
            updatePGNView();
        }
        boardModel = boardModelStack.peek();
        Log.d(TAG, "previousMove: BoardModel previous : " + boardModel + " \n count: " + boardModelStack.size());
        btn_previous_move.setEnabled(pgn.lastMove() != null);
        chessBoard.invalidate();
        saveGame();
        Log.d(TAG, "lastMove: " + move);
        Log.d(TAG, "Current last move: " + pgn.lastMove());
    }

    private void pushToStack() {
        boardModelStack.push(boardModel.clone());
        Log.d(TAG, "pushToStack: pushed current BoardModel to stack");
    }

    private String toFEN() {
        StringBuilder FEN = new StringBuilder(boardModel.toFEN());
        if (getGameState() == ChessState.WHITETOPLAY) FEN.append(" w");
        else if (getGameState() == ChessState.BLACKTOPLAY) FEN.append(" b");

        FEN.append(" ");

        King whiteKing = boardModel.getWhiteKing();
        King blackKing = boardModel.getBlackKing();

        if (whiteKing != null) {
            if (whiteKing.canShortCastle(this)) FEN.append('K');
            if (whiteKing.canLongCastle(this)) FEN.append('Q');
        }
        if (blackKing != null) {
            if (blackKing.canShortCastle(this)) FEN.append('k');
            if (blackKing.canLongCastle(this)) FEN.append('q');
        }

        FEN.append(" ");

        if (BoardModel.enPassantPawn != null) {
            Pawn enPassantPawn = BoardModel.enPassantPawn;
            FEN.append((char) (enPassantPawn.getPosition().charAt(1))).append(enPassantPawn.getRow() + 1 - enPassantPawn.direction);
        }

//        FEN.append(" - - ");
        return String.valueOf(FEN);
    }

    private void updatePGNView() {
        PGN_textView.setText(pgn.getFinalPGN());
        horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
    }
}