package com.drdedd.simplichess.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.R;

import java.util.Random;

public class SelectGameModeDialog extends Dialog implements View.OnClickListener {
    private final Random r = new Random();
    private LinearLayout playAsLayout;
    private boolean playAsWhite = true, start = false, singlePlayer = true;

    public SelectGameModeDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_select_game_mode);

        playAsLayout = findViewById(R.id.playAsLayout);

        RadioGroup mode = findViewById(R.id.mode);
        mode.setOnCheckedChangeListener((g, id) -> {
            switch (id) {
                case R.id.single_player:
                    singlePlayer = true;
                    playAsLayout.setVisibility(View.VISIBLE);
                    break;
                case R.id.two_players:
                    singlePlayer = false;
                    playAsLayout.setVisibility(View.GONE);
                    break;
            }
        });

        RadioGroup playAs = findViewById(R.id.playAs);
        playAs.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.white:
                    playAsWhite = true;
                    break;
                case R.id.black:
                    playAsWhite = false;
                    break;
                case R.id.random:
                    playAsWhite = r.nextBoolean();
                    break;
            }
        });

        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);

        Window window = getWindow();
        if (window != null) {
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            window.setLayout((int) (displayMetrics.widthPixels * 0.95), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.start:
                start = true;
                dismiss();
                break;
            case R.id.cancel:
                dismiss();
                break;
        }
    }

    public boolean isStart() {
        return start;
    }

    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    public boolean isPlayAsWhite() {
        return playAsWhite;
    }
}