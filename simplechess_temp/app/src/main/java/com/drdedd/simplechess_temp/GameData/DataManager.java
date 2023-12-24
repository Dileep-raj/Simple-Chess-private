package com.drdedd.simplechess_temp.GameData;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * A class to store and retrieve the game data using files or SharedPreferences
 */
public class DataManager {
    public static final String TAG = "DataManager", boardFile = "boardFile", PGNFile = "PGNFile", stackFile = "stackFile";
    private final String boardThemeLabel = "BoardTheme", whiteLabel = "white", blackLabel = "black", fullScreenLabel = "fullScreen", cheatModeLabel = "cheatMode", invertBlackSVGLabel = "invertBlackSVG";
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

    public Object readObject(String fileName) {
        try {
            FileInputStream fileInputStream = context.openFileInput(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object obj = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return obj;
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
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

    public BoardTheme getBoardTheme() {
        BoardTheme boardTheme = themesMap.get(sharedPreferences.getString(boardThemeLabel, "DEFAULT_BROWN"));
        if (boardTheme == null) boardTheme = BoardTheme.DEFAULT_BROWN;
        return boardTheme;
    }

    public void setBoardTheme(@NonNull BoardTheme boardTheme) {
        editor.putString(boardThemeLabel, boardTheme.toString());
        editor.commit();
    }

    public void saveData(@NonNull BoardTheme boardTheme) {
        editor.putString(boardThemeLabel, boardTheme.toString());
        editor.commit();
    }

    public void saveWhiteBlack(String white, String black) {
        editor.putString(whiteLabel, white);
        editor.putString(blackLabel, black);
        editor.commit();
    }

    public String getWhite() {
        return sharedPreferences.getString(whiteLabel, "White");
    }

    public String getBlack() {
        return sharedPreferences.getString(blackLabel, "Black");
    }

    public void setFullScreen(boolean fullScreen) {
        editor.putBoolean(fullScreenLabel, fullScreen);
        editor.commit();
    }

    public boolean isFullScreen() {
        return sharedPreferences.getBoolean(fullScreenLabel, false);
    }

    public void setCheatMode(boolean cheatMode) {
        editor.putBoolean(cheatModeLabel, cheatMode);
        editor.commit();
    }

    public boolean cheatModeEnabled() {
        return sharedPreferences.getBoolean(cheatModeLabel, false);
    }

    public Boolean invertBlackSVGEnabled() {
        return sharedPreferences.getBoolean(invertBlackSVGLabel, false);
    }

    public void setInvertBlackSVG(boolean invertBlackSVGs) {
        editor.putBoolean(invertBlackSVGLabel, invertBlackSVGs);
        editor.commit();
    }
}