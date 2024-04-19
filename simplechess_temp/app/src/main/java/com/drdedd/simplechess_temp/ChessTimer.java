package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.fragments.GameFragment;

import java.util.Locale;

/**
 * Timer for a chess game
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class ChessTimer {
    private final GameFragment gameFragment;
    private final int INITIAL_TIME;
    private long whiteTimeLeft, blackTimeLeft;
    private final TextView whiteTimeTV, blackTimeTV;
    private final LinearLayout whiteLayout, blackLayout;
    private CountDownTimer timer;
    private boolean whiteTurn, timerRunning;
    private final DataManager dataManager;
    private final int defaultColor, activeColor, criticalColor;

    public ChessTimer(GameFragment gameFragment) {
        this.gameFragment = gameFragment;
        dataManager = new DataManager(gameFragment.requireContext());
        INITIAL_TIME = minutesSecondsToMillis(dataManager.getTimerMinutes(), dataManager.getTimerSeconds());
        defaultColor = R.drawable.timer_bg_default;
        activeColor = R.drawable.timer_bg_active;
        criticalColor = R.drawable.timer_bg_critical;
        timerRunning = false;
        whiteTimeTV = gameFragment.whiteTimeTV;
        blackTimeTV = gameFragment.blackTimeTV;
        whiteLayout = gameFragment.whiteTimeLayout;
        blackLayout = gameFragment.blackTimeLayout;

        whiteLayout.setClipToOutline(true);
        blackLayout.setClipToOutline(true);

        resetTimer();
        updateTimeText();
    }


    /**
     * @param gameFragment  GameFragment instance
     * @param whiteTimeLeft White time left in milliseconds
     * @param blackTimeLeft Black time left in milliseconds
     */
    public ChessTimer(GameFragment gameFragment, long whiteTimeLeft, long blackTimeLeft) {
        this.gameFragment = gameFragment;
        dataManager = new DataManager(gameFragment.requireContext());
        INITIAL_TIME = minutesSecondsToMillis(dataManager.getTimerMinutes(), dataManager.getTimerSeconds());
        defaultColor = R.drawable.timer_bg_default;
        activeColor = R.drawable.timer_bg_active;
        criticalColor = R.drawable.timer_bg_critical;
        timerRunning = false;
        whiteTimeTV = gameFragment.whiteTimeTV;
        blackTimeTV = gameFragment.blackTimeTV;
        whiteLayout = gameFragment.whiteTimeLayout;
        blackLayout = gameFragment.blackTimeLayout;

        whiteLayout.setClipToOutline(true);
        blackLayout.setClipToOutline(true);

        this.whiteTimeLeft = whiteTimeLeft;
        this.blackTimeLeft = blackTimeLeft;
        updateTimeText();
    }

    /**
     * Starts a new timer
     */
    @SuppressLint("SetTextI18n")
    public void startTimer() {
        if (!timerRunning) {
            timerRunning = true;
            toggleTimer();
            Toast.makeText(gameFragment.requireContext(), "Timer started", Toast.LENGTH_SHORT).show();
        }
//        else stopTimer();
    }

    /**
     * Toggles white or black timer
     */
    public void toggleTimer() {
        if (!GameFragment.isGameTerminated())
            whiteTurn = GameFragment.playerToPlay() == Player.WHITE;
        newTimer(whiteTurn ? whiteTimeLeft : blackTimeLeft);
    }

    /**
     * Creates a new timer with the given time and updates the player's time
     *
     * @param timeLeft Time left in milliseconds
     */
    public void newTimer(long timeLeft) {
        if (timer != null) timer.cancel();
//        String TAG = "ChessTimer";
//        Log.d(TAG, (whiteTurn ? "White" : "Black") + "'s turn");
//        Log.d(TAG, "toggleTimer: Timer toggled\nWhite time left: " + formatTime(whiteTimeLeft) + "\nBlack time left: " + formatTime(blackTimeLeft) + "\nTime left param: " + formatTime(timeLeft));

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
                gameFragment.terminateByTimeOut();
            }
        }.start();
    }

    /**
     * Stops the timer
     */
    public void stopTimer() {
        if (timer != null) timer.cancel();
        if (whiteTimeLeft / 100 == 0 || blackTimeLeft / 100 == 0) {
            if (whiteTimeLeft > blackTimeLeft)
                Toast.makeText(gameFragment.requireContext(), "White wins", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(gameFragment.requireContext(), "Black wins", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(gameFragment.requireContext(), "Timer stopped", Toast.LENGTH_SHORT).show();
        timerRunning = false;
        updateTimeText();
    }

    /**
     * Resets timer to initial states
     */
    public void resetTimer() {
        whiteTimeLeft = INITIAL_TIME;
        blackTimeLeft = INITIAL_TIME;
        if (timer != null) timer.cancel();
        dataManager.setWhiteBlackTimeLeft(whiteTimeLeft, blackTimeLeft);
        whiteLayout.setBackgroundResource(defaultColor);
        blackLayout.setBackgroundResource(defaultColor);
    }

    /**
     * Updates time in timer TextViews
     */
    private void updateTimeText() {
        dataManager.setWhiteBlackTimeLeft(whiteTimeLeft, blackTimeLeft);
        whiteTimeTV.setText(formatTime(whiteTimeLeft));
        blackTimeTV.setText(formatTime(blackTimeLeft));
        if (timerRunning) updateLayoutColors();
    }

    /**
     * Sets background colors for timer TextViews
     */
    private void updateLayoutColors() {
        whiteLayout.setBackgroundResource(whiteTurn ? whiteTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
        blackLayout.setBackgroundResource(!whiteTurn ? blackTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
    }

    /**
     * Formats time to minutes, seconds and milliseconds
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

    public static int minutesSecondsToMillis(int minutes, int seconds) {
        return minutes * 60 * 1000 + seconds * 1000;
    }
}