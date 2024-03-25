package com.drdedd.simplechess_temp.GameData;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.PGN;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

/**
 * A class to store and retrieve the game data using files or <code>SharedPreferences</code>
 */
public class DataManager {
    public static final String TAG = "DataManager", boardFile = "boardFile", PGNFile = "PGNFile", stackFile = "stackFile", FENsListFile = "FENsListFile";
    private final String boardThemeLabel = "BoardTheme", whiteLabel = "white", blackLabel = "black", fullScreenLabel = "fullScreen", cheatModeLabel = "cheatMode", invertBlackSVGLabel = "invertBlackSVG";
    private final String timerLabel = "timer", minutesLabel = "minutes", secondsLabel = "seconds", whiteTimeLeftLabel = "whiteTimeLeft", blackTimeLeftLabel = "blackTimeLeft", gameTerminatedLabel = "gameTerminated";
    private final String gameTerminationMessageLabel = "gameTerminationMessage", UNICODE_LABEL = "unicode";
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final HashMap<String, BoardTheme> themesMap = new HashMap<>();

    public DataManager(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        BoardTheme[] themes = BoardTheme.getValues();
        for (BoardTheme theme : themes)
            themesMap.put(theme.toString(), theme);
    }

    /**
     * Read serialized object from the file
     *
     * @param fileName Name of the file
     * @return <code>Object|null</code>
     */
    public Object readObject(String fileName) {
        try {
            FileInputStream fileInputStream = context.openFileInput(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object obj = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return obj;
        } catch (FileNotFoundException e) {
//            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, fileName + " not found" + "\n" + e);
        } catch (IOException e) {
            Toast.makeText(context, "Couldn't load file", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Couldn't load " + fileName + "\n" + e);
        } catch (ClassNotFoundException e) {
            Toast.makeText(context, "File corrupted", Toast.LENGTH_SHORT).show();
            Log.d(TAG, fileName + " corrupted " + "\n" + e);
        }
        return null;
    }

    /**
     * Save serialized object into a file in storage
     *
     * @param fileName Name of the file
     * @param obj      Object to be saved
     */
    public void saveObject(String fileName, Object obj) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(obj);
//            Log.d(TAG, fileName + " saved successfully");
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, fileName + " not found" + "\n" + e);
        } catch (IOException e) {
            Toast.makeText(context, "Error while saving " + fileName, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Error while saving " + fileName + "\n" + e);
        }
    }

/*
    private boolean deleteFile(String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) return file.delete();
        else Log.d(TAG, "deleteFile: File does not exist");
        return false;
    }

    public void deleteGameFiles() {
        if (deleteFile(boardFile) && deleteFile(stackFile) && deleteFile(PGNFile))
            Log.d(TAG, "deleteGameFiles: Game deleted successfully");
    }
*/

    /**
     * Retrieve saved board theme
     *
     * @return <code>BoardTheme</code> - Light and dark color scheme for board
     */
    public BoardTheme getBoardTheme() {
        BoardTheme boardTheme;
        try {
            boardTheme = themesMap.get(sharedPreferences.getString(boardThemeLabel, "DEFAULT_BROWN"));
        } catch (Exception e) {
            Log.d(TAG, "getBoardTheme: Error in finding BoardTheme");
            return BoardTheme.DEFAULT_BROWN;
        }
        return boardTheme;
    }

    /**
     * Saves the board theme
     *
     * @param boardTheme <code>BoardTheme</code> to be saved
     */
    public void setBoardTheme(@NonNull BoardTheme boardTheme) {
        editor.putString(boardThemeLabel, boardTheme.toString());
        editor.commit();
    }

    public void saveData(BoardModel boardModel, PGN pgn, Stack<BoardModel> boardModelStack, LinkedList<String[]> FENs) {
        saveObject(DataManager.boardFile, boardModel);
        saveObject(DataManager.PGNFile, pgn);
        saveObject(DataManager.stackFile, boardModelStack);
        saveObject(DataManager.FENsListFile, FENs);
//        editor.putString(boardThemeLabel, boardTheme.toString());
        editor.commit();
    }

    /**
     * Saves white and black players' names
     *
     * @param white Name of white player
     * @param black Name of black player
     */
    public void saveWhiteBlack(String white, String black) {
        editor.putString(whiteLabel, white);
        editor.putString(blackLabel, black);
        editor.commit();
    }

    /**
     * Get White player's name
     */
    public String getWhite() {
        return sharedPreferences.getString(whiteLabel, "White");
    }

    /**
     * Get Black player's name
     */
    public String getBlack() {
        return sharedPreferences.getString(blackLabel, "Black");
    }

    /**
     * Sets fullscreen mode
     *
     * @param fullScreen Fullscreen flag
     */
    public void setFullScreen(boolean fullScreen) {
        editor.putBoolean(fullScreenLabel, fullScreen);
        editor.commit();
    }

    /**
     * Returns fullscreen mode setting
     *
     * @return <code>true|false</code>
     */
    public boolean isFullScreen() {
        return sharedPreferences.getBoolean(fullScreenLabel, false);
    }

    /**
     * Sets cheat mode setting
     *
     * @param cheatMode CheatMode flag
     */
    public void setCheatMode(boolean cheatMode) {
        editor.putBoolean(cheatModeLabel, cheatMode);
        editor.commit();
    }

    /**
     * Returns if cheat mode is enabled
     *
     * @return <code>true|false</code>
     */
    public boolean cheatModeEnabled() {
        return sharedPreferences.getBoolean(cheatModeLabel, false);
    }

    /**
     * Sets Invert black SVGs setting
     *
     * @param invertBlackSVGs Invert black SVGs flag
     */
    public void setInvertBlackSVG(boolean invertBlackSVGs) {
        editor.putBoolean(invertBlackSVGLabel, invertBlackSVGs);
        editor.commit();
    }

    /**
     * Returns if invert black SVGs is enabled
     *
     * @return <code>true|false</code>
     */
    public Boolean invertBlackSVGEnabled() {
        return sharedPreferences.getBoolean(invertBlackSVGLabel, false);
    }

    /**
     * Sets timer enable setting
     *
     * @param timerEnabled Timer setting flag
     */
    public void setTimerEnabled(boolean timerEnabled) {
        editor.putBoolean(timerLabel, timerEnabled);
        editor.commit();
    }

    /**
     * Returns if timer setting is enabled
     *
     * @return <code>true|false</code>
     */
    public boolean isTimerEnabled() {
        return sharedPreferences.getBoolean(timerLabel, false);
    }

    /**
     * Sets time limit for the game
     *
     * @param minutes No of minutes
     * @param seconds No of seconds
     */
    public void setTimerMinutesSeconds(int minutes, int seconds) {
        editor.putInt(minutesLabel, minutes);
        editor.putInt(secondsLabel, seconds);
        editor.commit();
    }

    /**
     * @return <code>int</code> - No of minutes
     */
    public int getTimerMinutes() {
        return sharedPreferences.getInt(minutesLabel, 10);
    }

    /**
     * @return <code>int</code> - No of seconds
     */
    public int getTimerSeconds() {
        return sharedPreferences.getInt(secondsLabel, 0);
    }

    /**
     * Sets time left for each player in the game
     *
     * @param whiteTimeLeft Time left for white
     * @param blackTimeLeft Time left for black
     */
    public void setWhiteBlackTimeLeft(long whiteTimeLeft, long blackTimeLeft) {
        editor.putLong(whiteTimeLeftLabel, whiteTimeLeft);
        editor.putLong(blackTimeLeftLabel, blackTimeLeft);
        editor.commit();
    }

    /**
     * @return <code>long</code> - Time left for white player
     */
    public long getWhiteTimeLeft() {
        return sharedPreferences.getLong(whiteTimeLeftLabel, 0);
    }

    /**
     * @return <code>long</code> - Time left for black player
     */
    public long getBlackTimeLeft() {
        return sharedPreferences.getLong(blackTimeLeftLabel, 0);
    }

    /**
     * Sets if a game is terminated
     *
     * @param gameTerminated Game termination flag
     */
    public void setGameTerminated(boolean gameTerminated) {
        editor.putBoolean(gameTerminatedLabel, gameTerminated);
        editor.commit();
    }

    /**
     * Returns whether a game is terminated
     *
     * @return <code>true|false</code>
     */
    public boolean isGameTerminated() {
        return sharedPreferences.getBoolean(gameTerminatedLabel, false);
    }

    /**
     * Sets game termination message
     *
     * @param gameTerminationMessage Message for game termination
     */
    public void setGameTerminationMessage(String gameTerminationMessage) {
        editor.putString(gameTerminationMessageLabel, gameTerminationMessage);
        editor.commit();
    }

    /**
     * @return <code>String</code> - Message for game termination
     */
    public String getGameTerminationMessage() {
        return sharedPreferences.getString(gameTerminationMessageLabel, "");
    }

    public void setUnicode(boolean useUnicode) {
        editor.putBoolean(UNICODE_LABEL, useUnicode);
        editor.commit();
    }

    public boolean getUnicode() {
        return sharedPreferences.getBoolean(UNICODE_LABEL, false);
    }
}