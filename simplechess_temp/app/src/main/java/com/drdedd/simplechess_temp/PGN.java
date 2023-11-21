package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class PGN implements Serializable {
    private StringBuilder pgn;
    private int moveCount;
    private final String event, date;
    private String white, black;
    private String result;
    private ChessState gameState;

    PGN(StringBuilder pgn, String event, String white, String black, String date, ChessState gameState) {
        this.pgn = pgn;
        this.moveCount = 0;
        this.event = event;
        this.white = white;
        this.black = black;
        this.date = date;
        this.gameState = gameState;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("SimpleDateFormat")
    public String exportPGN() throws IOException {
        final String TAG = "PGN";

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Simple chess/";
        Log.d(TAG, "exportPGN: Directory: " + dir);

        if (new File(dir).mkdir()) Log.d(TAG, "exportPGN:" + dir + " Folder created");

        File file = new File(dir, "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn");

        if (file.createNewFile()) Log.d(TAG, "exportPGN: File created successfully");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
//      Convert String to UTF-8 CharacterSet
        fileOutputStream.write(toString().getBytes(StandardCharsets.UTF_8));
        fileOutputStream.close();
        return dir;
    }

    public void addToPGN(Piece piece, String move) {
        moveCount++;
        if (moveCount % 2 == 1) pgn.append(moveCount / 2 + 1).append(". ");

        if (move.equals("")) pgn.append(piece.getPosition()).append(" ");
        else pgn.append(move).append(" ");

        switch (GameActivity.getGameState()) {
            case WHITETOPLAY:
            case BLACKTOPLAY:
                result = "*";
                break;
            case RESIGN:
            case CHECKMATE:
                result = "";
                break;
            case STALEMATE:
            case DRAW:
                result = "1/2-1/2";
                break;
        }
    }

    public void resetPGN() {
        moveCount = 0;
        pgn = new StringBuilder();
        result = "*";
    }

    public String getPGN() {
        return String.valueOf(pgn);
    }

    public ChessState getGameState() {
        return gameState;
    }

    public void setGameState(ChessState gameState) {
        this.gameState = gameState;
    }

    public void setWhiteBlack(String white, String black) {
        this.white = white;
        this.black = black;
    }

    public String getWhite() {
        return white;
    }

    public String getBlack() {
        return black;
    }

    @NonNull
    @Override
    public String toString() {
        return "[Event \"" + event + "\"] [Date \"" + date + "\"] [White \"" + white + "\"] [Black \"" + black + "\"] [Result  \"" + result + "\"]" + pgn;
    }

    public String toFEN(String boardFEN) {
        StringBuilder FEN = new StringBuilder(boardFEN);
        if (GameActivity.getGameState() == ChessState.WHITETOPLAY) FEN.append(" w");
        else if (GameActivity.getGameState() == ChessState.BLACKTOPLAY) FEN.append(" b");
        FEN.append(" - - ");
        return String.valueOf(FEN);
    }
}