package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ChessTimer {
    private final GameActivity gameActivity;
    private final int INITIAL_TIME;
    private long whiteTimeLeft, blackTimeLeft;
    private final TextView whiteTimeTV, blackTimeTV;
    private final LinearLayout whiteLayout, blackLayout;
    private CountDownTimer timer;
    private Button btn_startStopTimer;
    private boolean whiteTurn, timerRunning;
    private final DataManager dataManager;
    private final int defaultColor, activeColor, criticalColor;

    ChessTimer(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
        dataManager = new DataManager(gameActivity);
        INITIAL_TIME = dataManager.getTimerMinutes() * 60 * 1000 + dataManager.getTimerSeconds() * 1000;
        Resources resources = gameActivity.getResources();
        defaultColor = R.drawable.timer_bg_default;
        activeColor = R.drawable.timer_bg_active;
        criticalColor = R.drawable.timer_bg_critical;
        timerRunning = false;
        whiteTimeTV = gameActivity.findViewById(R.id.whiteTimeTV);
        blackTimeTV = gameActivity.findViewById(R.id.blackTimeTV);
        whiteLayout = gameActivity.findViewById(R.id.whiteTimeLayout);
        blackLayout = gameActivity.findViewById(R.id.blackTimeLayout);

        whiteLayout.setClipToOutline(true);
        blackLayout.setClipToOutline(true);

//        btn_startStopTimer = gameActivity.findViewById(R.id.btn_startStopTimer);
//        btn_startStopTimer.setOnClickListener(v -> startStopTimer());

//        whiteLayout.setOnClickListener(v -> {
//            if (!timerRunning) return;
//            whiteTurn = false;
//            toggleTimer(blackTimeLeft);
//        });
//
//        blackLayout.setOnClickListener(v -> {
//            if (!timerRunning) return;
//            whiteTurn = true;
//            toggleTimer(whiteTimeLeft);
//        });
        resetTimer();
        updateTimeText();
    }

    @SuppressLint("SetTextI18n")
    public void startTimer() {
        if (!timerRunning) {
            timerRunning = true;
            toggleTimer();
        } else stopTimer();
    }

    public void toggleTimer() {
        if (!GameActivity.isGameTerminated())
            whiteTurn = GameActivity.playerToPlay() == Player.WHITE;
        newTimer(whiteTurn ? whiteTimeLeft : blackTimeLeft);
    }

    public void newTimer(long timeLeft) {
        if (timer != null) timer.cancel();
        String TAG = "TimerTest";
        Log.d(TAG, (whiteTurn ? "White's turn" : "Black's turn"));
        Log.d(TAG, "toggleTimer: Timer toggled\nWhite time left: " + formatTime(whiteTimeLeft) + "\nBlack time left: " + formatTime(blackTimeLeft) + "\nTime left param: " + formatTime(timeLeft));

        timer = new CountDownTimer(timeLeft, 100) {
            @Override
            public void onTick(long l) {
                if (whiteTurn) whiteTimeLeft = l;
                else blackTimeLeft = l;
                if (l < 60000) {
                    cancel();
                    if (whiteTurn) newTimer(whiteTimeLeft);
                    else newTimer(blackTimeLeft);
                }
                updateTimeText();
            }

            @Override
            public void onFinish() {
                stopTimer();
                gameActivity.terminateByTimeOut();
            }
        }.start();
    }

    public void stopTimer() {
        timer.cancel();
        if (whiteTimeLeft / 100 == 0 || blackTimeLeft / 100 == 0) {
            if (whiteTimeLeft > blackTimeLeft)
                Toast.makeText(gameActivity, "White wins", Toast.LENGTH_SHORT).show();
            else Toast.makeText(gameActivity, "Black wins", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(gameActivity, "Timer stopped", Toast.LENGTH_SHORT).show();
        timerRunning = false;
        updateTimeText();
    }

    public void resetTimer() {
        whiteTimeLeft = INITIAL_TIME;
        blackTimeLeft = INITIAL_TIME;
        updateLayoutColors();
    }

    private void updateTimeText() {
        dataManager.setWhiteBlackTimeLeft(whiteTimeLeft, blackTimeLeft);
        whiteTimeTV.setText(formatTime(whiteTimeLeft));
        blackTimeTV.setText(formatTime(blackTimeLeft));
        if (timerRunning) updateLayoutColors();
    }

    private void updateLayoutColors() {
        whiteLayout.setBackgroundResource(whiteTurn ? whiteTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
        blackLayout.setBackgroundResource(!whiteTurn ? blackTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
    }

    /**
     * Formats time to minutes and seconds and milliseconds
     *
     * @param time Time in milliseconds
     * @return <code>String</code> - Time in <code>mm:ss.ms</code>
     */
    private String formatTime(long time) {
        int millis = (int) time % 1000 / 100;
        time /= 1000;
        int minute = (int) time / 60, seconds = (int) time % 60;
        if (time < 60)
            return String.format(Locale.getDefault(), "%01d:%02d.%1d", minute, seconds, millis);
        return String.format(Locale.getDefault(), "%02d:%02d", minute, seconds);
    }

    public static int millisToMinutes(long millis) {
        return (int) (millis / 1000 / 60);
    }

    public static int millisToSeconds(long millis) {
        return (int) (millis / 1000 % 60);
    }
}