package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.data.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Timer for a chess game
 */
public class ChessTimer {
    private final static long T1 = 60000;
    private final Context context;
    private final TextView whiteTimeTV, blackTimeTV;
    private final DataManager dataManager;
    private final BoardInterface boardInterface;
    private final long millis;
    private final int defaultColor, activeColor, criticalColor;
    private long whiteTimeLeft, blackTimeLeft, increment;
    private boolean whiteTurn, timerRunning, hasIncrement;
    private CountDownTimer timer;

    public ChessTimer(BoardInterface boardInterface, Context context, TextView whiteTimeTV, TextView blackTimeTV, long millis) {
        this.context = context;
        this.whiteTimeTV = whiteTimeTV;
        this.blackTimeTV = blackTimeTV;
        this.boardInterface = boardInterface;
        this.millis = millis;
//        this.whiteLayout = whiteLayout;
//        this.blackLayout = blackLayout;
        dataManager = null;
        defaultColor = R.color.timer_default;
        activeColor = R.color.timer_active;
        criticalColor = R.color.timer_critical;

        resetTimer();
        updateTimeText();
    }

    public ChessTimer(BoardInterface boardInterface, Context context, TextView whiteTimeTV, TextView blackTimeTV, TimeControl timeControl) {
        this.context = context;
        this.whiteTimeTV = whiteTimeTV;
        this.blackTimeTV = blackTimeTV;
        this.boardInterface = boardInterface;
        this.millis = timeControl.getMillis();
        increment = minutesSecondsToMillis(0, timeControl.getIncrement());
        hasIncrement = timeControl.hasIncrement();
        dataManager = null;
        defaultColor = R.color.timer_default;
        activeColor = R.color.timer_active;
        criticalColor = R.color.timer_critical;

        resetTimer();
        updateTimeText();
    }

    /**
     * Starts a new timer
     */
    @SuppressLint("SetTextI18n")
    public void startTimer() {
        if (!timerRunning) {
            toggleTimer();
            Toast.makeText(context, "Timer started", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Toggles white or black timer
     */
    public void toggleTimer() {
        if (boardInterface.isGameTerminated()) return;
        if (hasIncrement && timerRunning) if (whiteTurn) whiteTimeLeft += increment;
        else blackTimeLeft += increment;
        whiteTurn = boardInterface.isWhiteToPlay();
        if (!timerRunning) timerRunning = true;
        newTimer(whiteTurn ? whiteTimeLeft : blackTimeLeft);
    }

    /**
     * Creates a new timer with the given time and updates the player's time
     *
     * @param timeLeft Time left in milliseconds
     */
    public void newTimer(long timeLeft) {
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(timeLeft, 100) {
            @Override
            public void onTick(long l) {
                if (whiteTurn) whiteTimeLeft = l;
                else blackTimeLeft = l;
                if (l < T1) {
                    cancel();
                    if (whiteTurn) newTimer(whiteTimeLeft);
                    else newTimer(blackTimeLeft);
                }
                updateTimeText();
            }

            @Override
            public void onFinish() {
                stopTimer();
                boardInterface.terminateByTimeOut(boardInterface.isWhiteToPlay() ? Player.WHITE : Player.BLACK);
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
                Toast.makeText(context, "White wins", Toast.LENGTH_SHORT).show();
            else Toast.makeText(context, "Black wins", Toast.LENGTH_SHORT).show();
        }
        timerRunning = false;
        updateTimeText();
    }

    /**
     * Resets timer to initial states
     */
    public void resetTimer() {
        whiteTimeLeft = millis;
        blackTimeLeft = millis;
        updateTimeText();
        if (timer != null) timer.cancel();
        whiteTimeTV.setBackgroundResource(defaultColor);
        blackTimeTV.setBackgroundResource(defaultColor);
        if (dataManager != null) dataManager.setWhiteBlackTimeLeft(whiteTimeLeft, blackTimeLeft);
    }

    /**
     * Updates time in timer TextViews
     */
    private void updateTimeText() {
        whiteTimeTV.setText(formatTime(whiteTimeLeft));
        blackTimeTV.setText(formatTime(blackTimeLeft));
        if (timerRunning) {
            updateLayoutColors();
            if (dataManager != null)
                dataManager.setWhiteBlackTimeLeft(whiteTimeLeft, blackTimeLeft);
        }
    }

    /**
     * Sets background colors for timer TextViews
     */
    private void updateLayoutColors() {
        whiteTimeTV.setBackgroundResource(whiteTurn ? whiteTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
        blackTimeTV.setBackgroundResource(!whiteTurn ? blackTimeLeft < 60000 ? criticalColor : activeColor : defaultColor);
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

    public static long minutesSecondsToMillis(int minutes, int seconds) {
        return minutes * 60 * 1000L + seconds * 1000L;
    }

    /**
     * Time control for chess game
     * Standard time controls include Classical, Rapid, Blitz and Bullet
     *
     * @see <a href="">Time control in chess</a>
     */
    public static class TimeControl {
        public static TimeControl T1 = new TimeControl(1, 0, 0);
        public static TimeControl T2 = new TimeControl(2, 0, 0), T2_1 = new TimeControl(2, 0, 1);
        public static TimeControl T3 = new TimeControl(3, 0, 0), T3_2 = new TimeControl(3, 0, 2);
        public static TimeControl T5 = new TimeControl(5, 0, 0), T5_3 = new TimeControl(5, 0, 3);
        public static TimeControl T10 = new TimeControl(10, 0, 0), T10_5 = new TimeControl(10, 0, 5);
        public static TimeControl T15_10 = new TimeControl(15, 0, 10), T20 = new TimeControl(20, 0, 0);
        public static TimeControl T30 = new TimeControl(30, 0, 0), T30_20 = new TimeControl(30, 0, 20);

        public static ArrayList<TimeControl> bullet = new ArrayList<>(List.of(T1, T2, T2_1));
        public static ArrayList<TimeControl> blitz = new ArrayList<>(List.of(T3, T3_2, T5, T5_3));
        public static ArrayList<TimeControl> rapid = new ArrayList<>(List.of(T10, T10_5, T15_10, T20));
        public static ArrayList<TimeControl> classical = new ArrayList<>(List.of(T30, T30_20));
        private final int minutes, seconds;
        private final long millis;
        private final int increment;

        /**
         * @param minutes   Time in minutes
         * @param seconds   Time in seconds
         * @param increment Increment time in seconds
         */
        public TimeControl(int minutes, int seconds, int increment) {
            this.minutes = minutes;
            this.seconds = seconds;
            this.increment = increment;
            this.millis = minutesSecondsToMillis(minutes, seconds);
        }

        public long getMillis() {
            return millis;
        }

        public int getIncrement() {
            return increment;
        }

        public boolean hasIncrement() {
            return increment != 0;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%d%s+%s", minutes, seconds != 0 ? ":" + seconds : "", increment);
        }
    }
}