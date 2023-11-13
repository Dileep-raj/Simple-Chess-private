package com.drdedd.simplechess_temp;


import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.DataManager;

import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //    private final HashMap<String, BoardTheme> themesMap = new HashMap<>();
    //    private EditText whiteName, blackName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataManager dataManager = new DataManager(this);
        if (dataManager.isFullScreen())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.btn_new_game).setOnClickListener(view -> startGame());
        findViewById(R.id.btn_exit_app).setOnClickListener(view -> exit_app());
        findViewById(R.id.btn_settings).setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));

//
//        BoardTheme[] themes = BoardTheme.values();
//        String[] items = new String[themes.length];
//        int i = 0;
//        for (BoardTheme theme : themes) {
//            items[i] = theme.toString();
//            themesMap.put(items[i++], theme);
//        }
//
//        Spinner themeSpinnerMenu = findViewById(R.id.themeSpinnerMenu);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
//        themeSpinnerMenu.setAdapter(adapter);
//        themeSpinnerMenu.setSelection(dataManager.getBoardTheme().ordinal());
//
//        themeSpinnerMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
//                dataManager.setBoardTheme(Objects.requireNonNull(themesMap.get(items[position])));
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView) {
//            }
//        });
//        whiteName = findViewById(R.id.whiteName);
//        blackName = findViewById(R.id.blackName);
//
//
//        PGN pgn = (PGN) dataManager.readObject("PGNFile");
//        if (pgn != null) {
//            whiteName.setText(pgn.getWhite());
//            blackName.setText(pgn.getBlack());
//        }
//
//        dataManager.saveWhiteBlack(whiteName.getText().toString(), blackName.getText().toString());
    }

    public void startGame() {
        Intent i = new Intent(this, GameActivity.class);
        startActivity(i);
    }

    public void exit_app() {
        finishAffinity();
    }

}