package com.drdedd.simplichess.misc;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.data.Regexes;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pieces.Piece;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;

public class MiscMethods {
    private static final String TAG = "MiscMethods";

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

    /**
     * Converts column number to the corresponding file character
     */
    public static char toColChar(int col) {
        return (char) ('a' + col);
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
     * Converts piece to letter for FEN representation
     *
     * @param piece Piece to be converted
     * @return <code>K|Q|R|N|B|P</code> - Uppercase or Lowercase
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">More about FEN</a>
     */
    public static char getPieceChar(@NonNull Piece piece) {
        char ch = piece.getRank().getLetter();
        if (!piece.isWhite()) ch = Character.toLowerCase(ch);
        return ch;
    }

    public static float dpToPixel(DisplayMetrics displayMetrics, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    public static float spToPixel(DisplayMetrics displayMetrics, int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics);
    }

    public static void shareContent(Context context, String label, String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        context.startActivity(Intent.createChooser(shareIntent, "Share " + label));
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (nc == null) return false;
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    public static boolean isLichessLink(String link) {
        Matcher matcher = Regexes.lichessGamePattern.matcher(link);
        return matcher.find();
    }

    public static Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> uciToRowCol(String uciMove) {
        int fromRow, fromCol, toRow, toCol;
        fromCol = uciMove.charAt(0) - 'a';
        fromRow = uciMove.charAt(1) - '1';
        toCol = uciMove.charAt(2) - 'a';
        toRow = uciMove.charAt(3) - '1';
        return new Pair<>(new Pair<>(fromRow, fromCol), new Pair<>(toRow, toCol));
    }

    public static void setImageButtonEnabled(ImageButton imageButton, boolean enabled) {
        imageButton.setEnabled(enabled);
        imageButton.setAlpha(enabled ? 1f : 0.75f);
    }

    public static String repeat(String repeat, int n) {
        StringBuilder builder = new StringBuilder(repeat);
        for (int i = 1; i < n; i++) builder.append(repeat);
        return builder.toString();
    }

    public static String getString(InputStream stream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = br.readLine()) != null) response.append(line).append('\n');
            br.close();
            Log.d(TAG, "getString: Response string: " + response);
            return response.toString();
        } catch (IOException e) {
            Log.e(TAG, "getString: Exception occurred!", e);
        }
        return "";
    }

    public static boolean notWithinBounds(int rowCol) {
        return rowCol > 7 || rowCol < 0;
    }
}