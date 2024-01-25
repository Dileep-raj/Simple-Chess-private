package com.drdedd.simplechess_temp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.drdedd.simplechess_temp.GameData.DataManager;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataManager dataManager = new DataManager(this);
        if (dataManager.isFullScreen())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Button btn_continue = findViewById(R.id.btn_continue_game);

        btn_continue.setOnClickListener(view -> startGame(false));
        findViewById(R.id.btn_new_game).setOnClickListener(view -> startGame(true));
        findViewById(R.id.btn_exit_app).setOnClickListener(view -> exit_app());
        findViewById(R.id.btn_settings).setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));

        if (dataManager.readObject(DataManager.boardFile) == null || dataManager.readObject(DataManager.PGNFile) == null || dataManager.readObject(DataManager.stackFile) == null)
            btn_continue.setVisibility(View.GONE);
    }

    public void startGame(boolean newGame) {
        Log.d(TAG, "startGame: Game started");
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("newGame", newGame);
        startActivity(i);
    }

    public void exit_app() {
        finishAffinity();
    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        Log.d(TAG, "onRestart: Restarted");
//        finish();
//        startActivity(getIntent());
//    }
}