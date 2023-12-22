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
import java.util.LinkedList;

public class PGN implements Serializable {
    //    private StringBuilder pgn;
    //    private int moveCount;
    private final String app, date, TAG = "PGN";
    private String white, black, result;
    private ChessState gameState;
    private final LinkedList<String> moves = new LinkedList<>();

    PGN(String app, String white, String black, String date, ChessState gameState) {
//        this.pgn = pgn;
//        this.moveCount = 0;
        this.app = app;
        this.white = white;
        this.black = black;
        this.date = date;
        this.gameState = gameState;
        moves.clear();
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
//        moveCount++;
//        if (moveCount % 2 == 1) {
//            pgn.append(moveCount / 2 + 1).append(". ");
//        }

        if (move.equals("")) {
//            pgn.append(piece.getPosition()).append(" ");
            moves.addLast(piece.getPosition() + " ");
        } else {
//            pgn.append(move).append(" ");
            moves.addLast(move + " ");
        }

        switch (GameActivity.getGameState()) {
            case WHITE_TO_PLAY:
            case BLACK_TO_PLAY:
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

    public String lastMove() {
        return moves.peekLast();
    }

    public String removeLast() {
        if (!moves.isEmpty()) return moves.removeLast();
        return "";
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
        return "[App \"" + app + "\"] [Date \"" + date + "\"] [White \"" + white + "\"] [Black \"" + black + "\"] [Result  \"" + result + "\"]" + getPGN();
    }

    public String getPGN() {
        StringBuilder pgn = new StringBuilder();
        int length = moves.size();
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) pgn.append(i / 2 + 1).append(". ");
            pgn.append(moves.get(i)).append(" ");
        }
        return pgn.toString();
    }
}