package com.drdedd.simplichess.misc.lichess;

import android.util.Log;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.game.pgn.PGN;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class LichessGame {
    private static final String TAG = "LichessGame";
    private static final String keyId = "id", keyName = "name", keySpeed = "speed", keyStatus = "status", keyPlayers = "players", keyError = "error";
    private static final String keyWhite = "white", keyBlack = "black", keyUser = "user", keyRating = "rating", keyRatingDiff = "ratingDiff", keyWinner = "winner", keyTitle = "title";
    private static final String keyInaccuracy = "inaccuracy", keyMistake = "mistake", keyBlunder = "blunder", keyACPL = "acpl", keyAccuracy = "accuracy";
    private static final String keyOpening = "opening", keyECO = "eco", keyPly = "ply", keyClocks = "clocks", keyMoves = "moves", keyAnalysis = "analysis";
    private static final String keyEval = "eval", keyMate = "mate", keyBest = "best", keyJudgement = "judgment", keyVariation = "variation", keyComment = "comment";
    private static final String keyCreatedAt = "createdAt", keyPGN = "pgn";

    private static final Set<String> statuses = Set.of("started", "mate", "draw", "timeout", "outoftime", "resign");

    private static final String whiteWonOnTime = "CqIu8ufF", whiteTimeOut = "4BQv0hgp", whiteResigned = "N3DTAR0g", insufficientMaterial = "Mbk5NKGJ", repetition = "VY8HRGX8";
    private static final String timeOutDraw = "MH02zyFH", whiteLeftDraw = "N5gISgwF", checkmate = "eVSz616c";
    public static final Set<String> testGamesSet = Set.of(whiteWonOnTime, whiteTimeOut, whiteResigned, insufficientMaterial, repetition, timeOutDraw, whiteLeftDraw, checkmate);

    private String id, status, speed, opening, eco, openingPly, date;
    private LichessPlayer white, black, winner;
    private PlayerAccuracy whiteAccuracy, blackAccuracy;
    private final ArrayList<String> moves;
    private final ArrayList<Integer> clocks;
    private final ArrayList<MoveAnalysis> moveAnalyses;
    private boolean gameAnalysed, valid = true;

    private LichessGame() {
        moves = new ArrayList<>();
        clocks = new ArrayList<>();
        moveAnalyses = new ArrayList<>();
    }

    public static LichessGame parse(JSONObject json) {
        LichessGame game = new LichessGame();
        try {
            if (json.has(keyError)) {
                Log.e(TAG, "parse: Error while fetching JSON: " + json.getString(keyError));
                game.valid = false;
                return game;
            }
            game.id = json.getString(keyId);
            game.speed = json.optString(keySpeed, "");
            game.status = json.optString(keyStatus, PGN.RESULT_ONGOING);

            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH);
            Calendar calendar = Calendar.getInstance();
            long ms = json.optLong(keyCreatedAt, 0);
            if (ms != 0) {
                calendar.setTimeInMillis(ms);
                game.date = format.format(calendar.getTime());
            }

            JSONObject openingJSON = json.getJSONObject(keyOpening);
            game.opening = openingJSON.optString(keyName, "");
            game.eco = openingJSON.optString(keyECO, "");
            game.openingPly = openingJSON.optString(keyPly, "");

            JSONObject players = json.getJSONObject(keyPlayers), whitePlayer = players.getJSONObject(keyWhite), blackPlayer = players.getJSONObject(keyBlack);
            JSONObject whiteUser = whitePlayer.getJSONObject(keyUser), blackUser = blackPlayer.getJSONObject(keyUser);

            game.gameAnalysed = whitePlayer.has(keyAnalysis) && blackPlayer.has(keyAnalysis);

            game.white = new LichessPlayer(whiteUser.optString(keyName, "White"), whiteUser.getString(keyId), whiteUser.optString(keyTitle, ""), whitePlayer.optString(keyRating, ""), whitePlayer.optInt(keyRatingDiff, 0));
            game.black = new LichessPlayer(blackUser.optString(keyName, "Black"), blackUser.getString(keyId), blackUser.optString(keyTitle, ""), blackPlayer.optString(keyRating, ""), blackPlayer.optInt(keyRatingDiff, 0));

            if (json.has(keyWinner)) {
                if (json.getString(keyWinner).equals(keyWhite)) game.winner = game.white;
                else game.winner = game.black;
            }

            if (game.gameAnalysed) {
                game.whiteAccuracy = new PlayerAccuracy(game.white.name, true, whitePlayer.getJSONObject(keyAnalysis));
                game.blackAccuracy = new PlayerAccuracy(game.black.name, false, blackPlayer.getJSONObject(keyAnalysis));
            }

            if (json.has(keyMoves))
                game.moves.addAll(Arrays.asList(json.getString(keyMoves).split(" ")));
            if (game.moves.isEmpty() && json.has(keyPGN))
                game.moves.addAll(Arrays.asList(json.getString(keyPGN).split(" ")));
            if (json.has(keyClocks)) {
                JSONArray clocks = json.getJSONArray(keyClocks);
                for (int i = 0; i < clocks.length(); i++) game.clocks.add(clocks.getInt(i));
            }
            if (json.has(keyAnalysis)) {
                JSONArray analysis = json.getJSONArray(keyAnalysis);
                for (int i = 0; i < analysis.length(); i++)
                    game.moveAnalyses.add(new MoveAnalysis(analysis.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "parse: Error while parsing!", e);
        }
        return game;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getSpeed() {
        return speed;
    }

    public String getOpening() {
        return opening;
    }

    public String getECO() {
        return eco;
    }

    public String getOpeningPly() {
        return openingPly;
    }

    public LichessPlayer getWhite() {
        return white;
    }

    public LichessPlayer getBlack() {
        return black;
    }

    public LichessPlayer getWinner() {
        return winner;
    }

    public PlayerAccuracy getWhiteAccuracy() {
        return whiteAccuracy;
    }

    public PlayerAccuracy getBlackAccuracy() {
        return blackAccuracy;
    }

    public ArrayList<String> getMoves() {
        return moves;
    }

    public ArrayList<Integer> getClocks() {
        return clocks;
    }

    public ArrayList<MoveAnalysis> getMoveAnalyses() {
        return moveAnalyses;
    }

    public boolean isGameAnalysed() {
        return gameAnalysed;
    }

    public boolean isValid() {
        return valid;
    }

    @NonNull
    @Override
    public String toString() {
        if (!valid) return "Invalid game!";
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("\nLichess game:\nid: %s", id)).append(String.format(", result: %s", status)).append(String.format(", speed: %s", speed));

        if (winner != null) builder.append(String.format(", winner: %s", winner));

        builder.append(String.format("\nOpening: %s:%s,  openingPly: %s", eco, opening, openingPly));
        builder.append(String.format("\nWhite player: %s %s", white, white.eloDiff));
        builder.append(String.format("\nBlack player: %s %s", black, black.eloDiff));

        if (gameAnalysed) {
            builder.append(String.format("\nWhite accuracy:%s", whiteAccuracy));
            builder.append(String.format("\nBlack accuracy:%s", blackAccuracy));
        } else builder.append("\nGame not analysed");

        builder.append(String.format("\nMoves: %s", moves));

        if (!clocks.isEmpty()) builder.append(String.format("\nClocks: %s", clocks));
        if (gameAnalysed) builder.append(String.format("\nMove Analyses: %s", moveAnalyses));
        return builder.toString();
    }

    public static class LichessPlayer {
        private final String name, id, elo, title;
        private final String eloDiff;

        private LichessPlayer(String name, String id, String title, String elo, int eloDiff) {
            this.name = name;
            this.id = id;
            this.title = title;
            this.elo = elo;
            if (eloDiff == 0) this.eloDiff = "";
            else if (eloDiff > 0) this.eloDiff = "+" + eloDiff;
            else this.eloDiff = String.valueOf(eloDiff);
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public String getElo() {
            return elo;
        }

        public String getEloDiff() {
            return eloDiff;
        }

        public String getTitle() {
            return title;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s%s(%s)", title.isEmpty() ? title : title + " ", name, elo);
        }
    }

    public static class PlayerAccuracy {
        private final String name;
        private final int inaccuracy, mistake, blunder, acpl, accuracy;
        private final boolean white;

        private PlayerAccuracy(String name, boolean white, JSONObject analysis) {
            this.name = name;
            this.white = white;
            inaccuracy = analysis.optInt(keyInaccuracy, 0);
            mistake = analysis.optInt(keyMistake, 0);
            blunder = analysis.optInt(keyBlunder, 0);
            acpl = analysis.optInt(keyACPL, 0);
            accuracy = analysis.optInt(keyAccuracy, 0);
        }

        public String getName() {
            return name;
        }

        public int getInaccuracy() {
            return inaccuracy;
        }

        public int getMistake() {
            return mistake;
        }

        public int getBlunder() {
            return blunder;
        }

        public int getACPL() {
            return acpl;
        }

        public int getAccuracy() {
            return accuracy;
        }

        public boolean isWhite() {
            return white;
        }

        @NonNull
        @Override
        public String toString() {
            String i = "\n\t";
            return String.format(Locale.ENGLISH, "\tInaccuracies: %d%sMistakes: %d%sBlunders: %d%sAverage CPL: %d%sAccuracy: %d%%", inaccuracy, i, mistake, i, blunder, i, acpl, i, accuracy);
        }
    }

    public static class MoveAnalysis {
        private final String eval, bestMove, variation, annotation, comment;

        private MoveAnalysis(JSONObject moveAnalysis) {
            if (moveAnalysis.has(keyEval))
                eval = parseEval(keyEval, moveAnalysis.optString(keyEval, ""));
            else if (moveAnalysis.has(keyMate))
                eval = parseEval(keyMate, moveAnalysis.optString(keyMate, ""));
            else eval = "";
            bestMove = moveAnalysis.optString(keyBest, "");
            variation = moveAnalysis.optString(keyVariation, "");

            JSONObject judgement = new JSONObject();
            if (moveAnalysis.has(keyJudgement)) {
                try {
                    judgement = moveAnalysis.getJSONObject(keyJudgement);
                } catch (JSONException e) {
                    Log.e(TAG, "MoveAnalysis: Exception during parsing analysis!", e);
                }
            }
            comment = judgement.optString(keyComment, "");
            annotation = judgement.optString(keyName, "").toUpperCase();
            if (!annotation.isEmpty()) Log.d(TAG, "MoveAnalysis: Annotation: " + annotation);
        }

        private String parseEval(String name, String value) {
            if (name.equals(keyEval))
                return String.format(Locale.ENGLISH, "%.2f", Float.parseFloat(value) / 100);
            if (name.equals(keyMate)) {
                int mate = Integer.parseInt(value);
                return String.format(Locale.ENGLISH, "%sM%d", mate < 0 ? "-" : "", Math.abs(mate));
            }
            return "";
        }

        public String getEval() {
            return eval;
        }

        public String getBestMove() {
            return bestMove;
        }

        public String getVariation() {
            return variation;
        }

        public String getAnnotation() {
            return annotation;
        }

        public String getComment() {
            return comment;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "Eval: %s%s", eval, comment.isEmpty() ? comment : " " + comment);
        }
    }

    private final static String testOngoingBlitz = "{\"id\":\"nD062ydS\",\"rated\":true,\"variant\":\"standard\",\"speed\":\"bullet\",\"perf\":\"bullet\",\"createdAt\":1727598997341,\"lastMoveAt\":1727599018204,\"status\":\"started\",\"source\":\"pool\",\"players\":{\"white\":{\"user\":{\"name\":\"Roaccutane\",\"title\":\"NM\",\"flair\":\"nature.high-voltage\",\"id\":\"roaccutane\"},\"rating\":2812},\"black\":{\"user\":{\"name\":\"Peng_Li_Min\",\"title\":\"GM\",\"id\":\"peng_li_min\"},\"rating\":2945}},\"opening\":{\"eco\":\"D01\",\"name\":\"Richter-Veresov Attack\",\"ply\":5},\"moves\":\"d4 Nf6 Nc3 d5 Bg5 Nbd7 f3 h6 Bh4 c5 e4 dxe4 d5 exf3 Nxf3 g5 Bf2\",\"clocks\":[6000,6000,5947,5967,5859,5856,5804,5777,5712,5524,5663,5258,5573,5103,5573,5060,5478],\"clock\":{\"initial\":60,\"increment\":0,\"totalTime\":60},\"division\":{}}";
}