package com.drdedd.simplichess.misc.chess_api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.misc.MiscMethods;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class ChessAPI {
    private static final String TAG = "ChessAPI", post = "https://chess-api.com/v1", websocket = "wss://chess-api.com/v1";
    private static final String fieldFEN = "fen", fieldVariants = "variants", fieldDepth = "depth", fieldTime = "maxThinkingTime", fieldMoves = "searchmoves";
    public static final int defaultDepth = 18, defaultVariants = 1, defaultTime = 100;

    public static void main(String[] args) {
        ChessAPI api = new ChessAPI();
//        Scanner sc = new Scanner(System.in);
//        String FEN = sc.nextLine();
        String FEN = "8/1P1R4/n1r2B2/3Pp3/1k4P1/6K1/Bppr1P2/2q5 w - - 0 1";
        Result result = api.getAnalysis(FEN, 2, defaultDepth, defaultTime, "");
        System.out.println(result);
//        sc.close();
    }

    public Result getAnalysis(String FEN, int variants, int depth, int time, String moves) {
        if (variants < 1 || variants > 5) variants = defaultVariants;
        if (depth < 1 || depth > 18) depth = defaultDepth;
        if (time < 1 || time > 100) time = defaultTime;

        String body = buildRequestBody(FEN, variants, depth, time, moves);

        try {
            URL url = new URL(post);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setRequestProperty("body", body);
//            builder.POST(HttpRequest.BodyPublishers.ofString(body));
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                return new Result(MiscMethods.getString(connection.getInputStream()));
            else {
                String s = MiscMethods.getString(connection.getErrorStream());
                Log.e(TAG, "getAnalysis: Error while fetching analysis!\n" + s);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return null;
    }

    private String buildRequestBody(String FEN, int variants, int depth, int time, String move) {
        StringBuilder builder = new StringBuilder("{");
        if (FEN != null && !FEN.isEmpty())
            builder.append(String.format("\"%s\":\"%s\"", fieldFEN, FEN));
        if (variants > 0 && variants < 6)
            builder.append(",\"").append(fieldVariants).append("\":").append(variants);
        if (depth > 0 && depth < 19)
            builder.append(",\"").append(fieldDepth).append("\":").append(depth);
        if (time > 0 && time < 101)
            builder.append(",\"").append(fieldTime).append("\":").append(time);
        if (move != null && !move.isEmpty())
            builder.append(String.format("\"%s\":\"%s\"", fieldMoves, move));
        return builder.append("}").toString();
    }

    public static class Result {
        private static final String keyText = "text", keyType = "type", keyFrom = "from", keyTo = "to", keyTurn = "turn";
        private static final String keyEval = "eval", keyMove = "move", keyFEN = "fen", keySAN = "san", keyUCI = "lan", keyDepth = "depth", keyCP = "centipawns", keyMate = "mate";
        private static final String keyWin = "winChance", keyIsCastling = "isCastling", keyIsCapture = "isCapture", keyIsPromotion = "isPromotion";
        private static final String keyMoves = "continuationArr";
        private static final String n = "\n";

        private String text, type, eval, move, fen, san, uci, fromSquare, toSquare;
        private int depth, cp, mate;
        private float win;
        private Player turn;
        private boolean isCapture, isCastling, isPromotion;
        private final ArrayList<String> moves = new ArrayList<>();

        private Result(String result) {
            try {
                JSONObject json = new JSONObject(result);
                text = json.optString(keyText, "");
                type = json.optString(keyType, "");
                eval = json.optString(keyEval, "");
                move = json.optString(keyMove, "");
                fen = json.optString(keyFEN, "");
                san = json.optString(keySAN, "");
                uci = json.optString(keyUCI, "");
                fromSquare = json.optString(keyFrom, "");
                toSquare = json.optString(keyTo, "");

                depth = json.optInt(keyDepth, 12);
                cp = Integer.parseInt(json.optString(keyCP, "0"));
                mate = json.optInt(keyMate, 0);

                win = json.optInt(keyWin, 0);

                turn = json.getString(keyTurn).equals("w") ? Player.WHITE : Player.BLACK;
                isCapture = json.optBoolean(keyIsCapture, false);
                isCastling = json.optBoolean(keyIsCastling, false);
                isPromotion = json.optBoolean(keyIsPromotion, false);

                JSONArray array = json.getJSONArray(keyMoves);
                for (int i = 0; i < array.length(); i++) moves.add(array.getString(i));
            } catch (Exception e) {
                Log.e(TAG, "Result: Exception! ", e);
            }
        }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }

        public String getEval() {
            return eval;
        }

        public String getMove() {
            return move;
        }

        public String getFEN() {
            return fen;
        }

        public String getSAN() {
            return san;
        }

        public String getUCI() {
            return uci;
        }

        public String getFromSquare() {
            return fromSquare;
        }

        public String getToSquare() {
            return toSquare;
        }

        public int getDepth() {
            return depth;
        }

        public int getCP() {
            return cp;
        }

        public int getMate() {
            return mate;
        }

        public float getWinChance() {
            return win;
        }

        public Player getTurn() {
            return turn;
        }

        public boolean isCapture() {
            return isCapture;
        }

        public boolean isCastling() {
            return isCastling;
        }

        public boolean isPromotion() {
            return isPromotion;
        }

        public ArrayList<String> getMoves() {
            return moves;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(n);
            String[] ranks = fen.substring(0, fen.indexOf(' ')).split("/");
            int i, c;
            for (String rank : ranks) {
                for (i = 0, c = 0; i < rank.length() && c < 8; i++) {
                    char ch = rank.charAt(i);
                    if (Character.isDigit(ch)) {
                        int spaces = ch - '0';
                        builder.append(MiscMethods.repeat("- ", spaces));
                        c += spaces;
                    } else builder.append(ch).append(' ');
                }
                builder.append(n);
            }

            builder.append(fen).append(n);
            builder.append("Eval: ").append(eval).append(n);
            builder.append("Best move: ").append(san).append(' ').append(uci).append(n);
            builder.append("Line: ").append(moves).append(n);

            return builder.append(n).toString();
        }
    }
}