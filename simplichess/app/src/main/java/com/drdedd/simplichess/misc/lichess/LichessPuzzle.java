package com.drdedd.simplichess.misc.lichess;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class LichessPuzzle {
    private static final String TAG = "LichessPuzzle";
    private static final String keyGame = "game", keyPuzzle = "puzzle", keySolution = "solution", keyInitialPly = "initialPly", keyRating = "rating";
    private final LichessGame game;
    private final ArrayList<String> solution;
    private int initialPly, rating;

    private LichessPuzzle(JSONObject gameJSON) {
        game = LichessGame.parse(gameJSON);
        solution = new ArrayList<>();
    }

    private static LichessPuzzle parse(JSONObject json) {
        try {
            LichessPuzzle puzzle = new LichessPuzzle(json.getJSONObject(keyGame));

            JSONObject puzzleJSON = json.getJSONObject(keyPuzzle);
            puzzle.solution.addAll(Arrays.asList(puzzleJSON.getString(keySolution).split(" ")));
            puzzle.initialPly = puzzleJSON.getInt(keyInitialPly);
            puzzle.rating = puzzleJSON.getInt(keyRating);
            return puzzle;
        } catch (JSONException e) {
            Log.e(TAG, "parse: Parse failed!", e);
            return null;
        }
    }

    public ArrayList<String> getSolution() {
        return solution;
    }

    public int getInitialPly() {
        return initialPly;
    }

    public static String getKeyRating() {
        return keyRating;
    }

    public LichessGame getGame() {
        return game;
    }
}