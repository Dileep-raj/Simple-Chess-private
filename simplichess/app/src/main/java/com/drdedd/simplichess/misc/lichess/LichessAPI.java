package com.drdedd.simplichess.misc.lichess;

import android.util.Log;

import com.drdedd.simplichess.data.Regexes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;

import javax.net.ssl.HttpsURLConnection;

public class LichessAPI {
    private static final String TAG = "LichessAPI";
    private static final String urlEndPoint = "https://lichess.org/", urlDailyPuzzle = "api/puzzle/daily", urlPuzzle = "api/puzzle", urlExportGame = "game/export", urlCloudEval = "api/cloud-eval";
    private static final String LITERATE = "literate", ACCURACY = "accuracy", TRUE = "1", FEN = "fen", MULTI_PV = "multiPv", keyError = "error";
    private static final String typeJSON = "application/json", typePGN = "application/x-chess-pgn";
    private static final String whiteCircle = " ○ ", blackCircle = " ● ", blackPawn = " ♟ ", whitePawn = " ♙ ";

    public static String getGamePGNById(String id) {
        return getResponse(buildURL(urlEndPoint + urlExportGame, id, LITERATE, TRUE, ACCURACY, TRUE), typePGN);
    }

    public static JSONObject getGameJSONById(String id) {
        String url = buildURL(urlEndPoint + urlExportGame, id, LITERATE, TRUE, ACCURACY, TRUE);
        String response = getResponse(url, typeJSON);
        try {
            if (!response.isEmpty()) {
                JSONObject json = new JSONObject(response);
                if (isValidJSON(json)) return json;
                else return new JSONObject("{\"error\":\"Game not found\"}");
            }
        } catch (JSONException e) {
            Log.e(TAG, "getGameJSONById: Exception occurred!\nURL: " + url + "\nResponse:\n" + response, e);
        }
        return null;
    }

    private static boolean isValidJSON(JSONObject json) {
        return !json.has("error");
    }

    private static String getResponse(String url, String type) {
        try {
            URL urlRequest = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) urlRequest.openConnection();
            connection.addRequestProperty("ACCEPT", type);
            connection.setRequestMethod("GET");
            connection.connect();
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = br.readLine()) != null) response.append(line).append('\n');
                br.close();
                Log.d(TAG, "getResponse: Response string: " + response);
                return response.toString();
            } else if (statusCode == HttpsURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "getResponse: Not found!\nRequest: " + urlRequest);
                return "{\"error\":\"Not found\"}";
            }
        } catch (Exception e) {
            Log.e(TAG, "getResponse: Exception occurred!", e);
        }
        return "";
    }

    private static String buildURL(String baseURL, String additionalPath, String... params) {
        String url = String.format("%s%s?", baseURL, additionalPath != null && !additionalPath.isEmpty() ? "/" + additionalPath : "");

        StringBuilder paramBuilder = new StringBuilder();
        int l = params.length;
        for (int i = 0; i < l; i += 2) {
            try {
                paramBuilder.append(params[i]).append('=').append(URLEncoder.encode(params[i + 1], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "buildURL: Encoding exception!", e);
            }
            if (i < l - 2) paramBuilder.append('&');
        }

        String s = url + paramBuilder;
        System.out.println("URL: " + s);
        return s;
    }

    public static String extractCode(String link) {
        Matcher lichessMatcher = Regexes.lichessGamePattern.matcher(link);
        if (lichessMatcher.find()) {
            String group = lichessMatcher.group();
            int index = group.indexOf("org/") + 4;
            Log.d(TAG, String.format("extractCode: Group: %s index: %d", group, index));
            return group.substring(index, index + 8);
        }
        return "";
    }
}