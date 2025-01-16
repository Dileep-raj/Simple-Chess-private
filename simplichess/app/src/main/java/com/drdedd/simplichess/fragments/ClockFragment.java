package com.drdedd.simplichess.fragments;

import static com.drdedd.simplichess.misc.MiscMethods.opponentPlayer;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.drdedd.simplichess.game.BoardModel;
import com.drdedd.simplichess.misc.ChessTimer;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.FragmentClockBinding;
import com.drdedd.simplichess.interfaces.GameLogicInterface;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class ClockFragment extends Fragment implements GameLogicInterface {
    private static final String TAG = "ClockFragment";
    private final static long T1 = 60000, T2 = 120000, T3 = 180000, T10 = 600000, T20 = 1200000, T30 = 1800000, Test = 1000L;
    private final static long millis = T1;
    private ChessTimer.TimeControl timeControl = ChessTimer.TimeControl.T10;
    private FragmentClockBinding binding;
    private TextView whiteTime, blackTime, moveNumber;
    private ImageButton pauseResumeTimer;
    private ChessTimer chessTimer;
    private boolean gameTerminated, whiteToPlay, timerRunning, timerPaused;
    private Vibrator vibrator;
    private boolean sound;
    private MoreMenuDialog menuDialog;
    private int moveCount;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        blackTime = binding.blackTime;
        whiteTime = binding.whiteTime;
        moveNumber = binding.moveNumber;
        pauseResumeTimer = binding.pauseResumeTimer;
        reset();

        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        binding.resetTimer.setOnClickListener(v -> stopTimer());
        pauseResumeTimer.setOnClickListener(v -> pauseResumeTimer());
        binding.menu.setOnClickListener(v -> showMenuDialog());
        blackTime.setOnClickListener(v -> {
            if (!whiteToPlay) toggleTimer();
        });
        whiteTime.setOnClickListener(v -> {
            if (whiteToPlay) toggleTimer();
        });
    }

    private void reset() {
        chessTimer = new ChessTimer(this, requireContext(), whiteTime, blackTime, timeControl);
        moveCount = 0;
        gameTerminated = false;
        whiteToPlay = true;
        timerRunning = false;
        timerPaused = false;
        whiteTime.setBackgroundResource(R.color.timer_default);
        blackTime.setBackgroundResource(R.color.timer_default);

        moveNumber.setText(String.valueOf(moveCount / 2 + 1));
        pauseResumeTimer.setImageResource(R.drawable.ic_pause);

        toggleButtons();
    }

    private void toggleTimer() {
        if (timerPaused) return;
        moveCount++;
        moveNumber.setText(String.valueOf(moveCount / 2 + 1));
        whiteToPlay = !whiteToPlay;
        if (timerRunning) chessTimer.toggleTimer();
        else {
            chessTimer.startTimer();
            timerRunning = true;
            timerPaused = false;
        }
        toggleButtons();
    }

    private void stopTimer() {
        chessTimer.stopTimer();
        chessTimer.resetTimer();
        if (timerRunning) Log.d(TAG, "stopTimer: Timer stopped and reset");
        reset();
    }

    private void pauseResumeTimer() {
        if (timerRunning) {
            chessTimer.stopTimer();
            timerRunning = false;
            timerPaused = true;
            pauseResumeTimer.setImageResource(R.drawable.ic_play);
        } else {
            chessTimer.startTimer();
            timerRunning = true;
            timerPaused = false;
            pauseResumeTimer.setImageResource(R.drawable.ic_pause);
        }
        toggleButtons();
    }

    @Override
    public void terminateByTimeOut(Player player) {
        String termination = opponentPlayer(player) + " won on time";
        Log.d(TAG, "terminateByTimeOut: " + termination);
        Toast.makeText(requireContext(), termination, Toast.LENGTH_SHORT).show();
        vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE));
        gameTerminated = true;
        timerRunning = false;
        timerPaused = false;
        toggleButtons();

        if (sound) {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
        }
    }

    private void toggleButtons() {
//        Log.d(TAG, "toggleButtons() called");
        setButtonEnabled(pauseResumeTimer, timerRunning || timerPaused);
        setButtonEnabled(binding.resetTimer, timerRunning || timerPaused);
        setButtonEnabled(binding.menu, !timerRunning || timerPaused);
    }

    private void setButtonEnabled(ImageButton imageButton, boolean enabled) {
        imageButton.setEnabled(enabled);
        imageButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private void showMenuDialog() {
        menuDialog = new MoreMenuDialog(requireContext(), timeControl, sound);
        menuDialog.show();
        menuDialog.setOnDismissListener(d -> {
            timeControl = menuDialog.timeControl;
            sound = menuDialog.sound;
            stopTimer();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timerRunning) pauseResumeTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (timerPaused) pauseResumeTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    @Override
    public boolean isWhiteToPlay() {
        return whiteToPlay;
    }

    @Override
    public boolean isGameTerminated() {
        return gameTerminated;
    }

    @Override
    public Piece pieceAt(int row, int col) {
        return null;
    }

    @Override
    public boolean move(int fromRow, int fromCol, int toRow, int toCol) {
        return false;
    }

    @Override
    public boolean capturePiece(Piece piece) {
        return false;
    }

    @Override
    public void promote(Pawn pawn, int row, int col, int fromRow, int fromCol) {
    }

    @Override
    public BoardModel getBoardModel() {
        return null;
    }

    public HashMap<String, HashSet<Integer>> getAllLegalMoves() {
        return null;
    }

    @Override
    public boolean isPieceToPlay(Piece piece) {
        return false;
    }

    static class MoreMenuDialog extends Dialog {

        private boolean sound;
        private ChessTimer.TimeControl timeControl;

        public MoreMenuDialog(@NonNull Context context, ChessTimer.TimeControl timeControl, boolean sound) {
            super(context);
            this.timeControl = timeControl;
            this.sound = sound;
            show();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.dialog_clock_more_menu);

            SwitchCompat soundToggle = findViewById(R.id.clockSoundToggle);
            soundToggle.setChecked(sound);

            ArrayList<ChessTimer.TimeControl> timeControls = new ArrayList<>(ChessTimer.TimeControl.bullet);
            timeControls.addAll(ChessTimer.TimeControl.blitz);
            timeControls.addAll(ChessTimer.TimeControl.rapid);
            timeControls.addAll(ChessTimer.TimeControl.classical);
            ArrayAdapter<ChessTimer.TimeControl> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, timeControls);

            Spinner timeControlSpinner = findViewById(R.id.timeControlSpinner);
            timeControlSpinner.setAdapter(adapter);
            timeControlSpinner.setSelection(timeControls.indexOf(timeControl));

            timeControlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    timeControl = timeControls.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            soundToggle.setOnCheckedChangeListener((button, selected) -> sound = selected);

            Objects.requireNonNull(getWindow()).setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}