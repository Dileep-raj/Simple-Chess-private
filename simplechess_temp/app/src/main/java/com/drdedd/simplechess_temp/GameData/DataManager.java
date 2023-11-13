package com.drdedd.simplechess_temp.GameData;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
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
    private final String TAG = "DataManager";
    private File file;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final HashMap<String, BoardTheme> themesMap = new HashMap<>();
    public final BoardTheme[] themes = BoardTheme.get();

    public DataManager(Context context) {
        this.context = context;
//        sharedPreferences = context.getSharedPreferences("GameData", Context.MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        for (BoardTheme theme : themes)
            themesMap.put(theme.toString(), theme);
    }

    public Object readObject(String fileName) {
        file = new File(context.getFilesDir(), fileName);
        try {
            FileInputStream fileInputStream = context.openFileInput(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object obj = objectInputStream.readObject();
            if (file.createNewFile()) Log.d(TAG, "File created successfully");
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
            if (file.createNewFile()) Log.d(TAG, "File created successfully");
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(obj);
            Log.d(TAG, fileName + " saved successfully");
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
        return themesMap.get(sharedPreferences.getString("BoardTheme", "GREY"));
    }

    public void setBoardTheme(@NonNull BoardTheme boardTheme) {
        editor.putString("BoardTheme", boardTheme.toString());
        editor.commit();
    }

    public void saveData(@NonNull BoardTheme boardTheme) {
        editor.putString("BoardTheme", boardTheme.toString());
        editor.commit();
    }

    public void saveWhiteBlack(String white, String black) {
        editor.putString("white", white);
        editor.putString("black", black);
        editor.commit();
    }

    public String getWhite() {
        return sharedPreferences.getString("white", "White");
    }

    public String getBlack() {
        return sharedPreferences.getString("black", "Black");
    }

    public void setFullScreen(boolean fullScreen) {
        editor.putBoolean("fullScreen", fullScreen);
        editor.commit();
    }

    public boolean isFullScreen() {
        return sharedPreferences.getBoolean("fullScreen", false);
    }
}