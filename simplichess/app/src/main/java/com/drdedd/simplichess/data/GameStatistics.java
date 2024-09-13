package com.drdedd.simplichess.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class GameStatistics extends SQLiteOpenHelper {
    private static final int version = 1;
    private static final String DBName = "SimpleChessDB", TABLE_NAME = "Statistics", COLUMN_NAME = "record_name", COLUMN_TIME_CONSUMED = "time_consumed", COLUMN_SIZE = "size", COLUMN_DATE = "recorded_on";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
    private static final String TAG = "GameStatistics";

    public GameStatistics(@Nullable Context context) {
        super(context, DBName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMN_NAME + " TEXT, " + COLUMN_TIME_CONSUMED + " LONG, " + COLUMN_SIZE + " INTEGER ," + COLUMN_DATE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addRecord(String name, long time, long size) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        String date = DATE_FORMAT.format(new Date());
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TIME_CONSUMED, time);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_SIZE, size);
        db.insert(TABLE_NAME, null, values);
    }

    public ArrayList<String> getNames() {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> names = new ArrayList<>();
        Cursor namesCursor = db.rawQuery("SELECT " + COLUMN_NAME + " FROM " + TABLE_NAME + " GROUP BY " + COLUMN_NAME, null);
        while (namesCursor.moveToNext()) names.add(namesCursor.getString(0));
        namesCursor.close();
        return names;
    }

    public ArrayList<RecordsData> getRecords(String name) {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<RecordsData> records = new ArrayList<>();
        Cursor recordsCursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " where " + COLUMN_NAME + "=?", new String[]{name});
        Log.i(TAG, "getRecords: Total no of records: " + recordsCursor.getCount());
        while (recordsCursor.moveToNext())
            records.add(new RecordsData(recordsCursor.getString(0), recordsCursor.getLong(1), recordsCursor.getInt(2), recordsCursor.getString(3)));
        recordsCursor.close();
        return records;
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }

    public static class RecordsData {
        private final String name, date;
        private final long time;
        private final int size;

        public RecordsData(String name, long time, int size, String date) {
            this.name = name;
            this.time = time;
            this.size = size;
            this.date = date;
        }

        public String getName() {
            return name;
        }

        public long getTime() {
            return time;
        }

        public String getDate() {
            return date;
        }

        public long getSize() {
            return size;
        }
    }
}