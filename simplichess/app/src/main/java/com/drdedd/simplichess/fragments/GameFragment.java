package com.drdedd.simplichess.fragments;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.drdedd.simplichess.misc.MiscMethods.opponentPlayer;
import static com.drdedd.simplichess.misc.MiscMethods.shareContent;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.databinding.FragmentGameBinding;
import com.drdedd.simplichess.dialogs.GameOverDialog;
import com.drdedd.simplichess.game.GameLogic;
import com.drdedd.simplichess.game.Openings;
import com.drdedd.simplichess.game.ParsedGame;
import com.drdedd.simplichess.game.gameData.Annotation;
import com.drdedd.simplichess.game.gameData.ChessState;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.interfaces.GameUI;
import com.drdedd.simplichess.misc.ChessTimer;
import com.drdedd.simplichess.misc.Constants;
import com.drdedd.simplichess.views.ChessBoard;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Fragment to view and play chess game
 */
@SuppressLint("NewApi")
public class GameFragment extends Fragment implements GameUI {
    private final static String TAG = "GameFragment";
    private FragmentGameBinding binding;
    private String termination;
    private PGN pgn;
    private ChessBoard chessBoard;
    private ImageButton btn_undo_move, btn_resign, btn_draw;
    public TextView PGN_textView, gameStateView, whiteName, blackName, whiteCaptured, blackCaptured, whiteValue, blackValue, whiteTimeTV, blackTimeTV;
    private HorizontalScrollView horizontalScrollView;
    private DataManager dataManager;
    protected ClipboardManager clipboard;
    private boolean timerEnabled;
    private ChessTimer chessTimer;
    private NavController navController;
    private GameLogic gameLogic;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
        Bundle args = requireArguments();
        navController = Navigation.findNavController(view);

        chessBoard = binding.chessBoard;
        btn_undo_move = binding.btnUndoMove;
        btn_resign = binding.btnResign;
        btn_draw = binding.btnDraw;
        PGN_textView = binding.pgnTextview;
        gameStateView = binding.gameStateView;
        whiteName = binding.whiteNameTV;
        blackName = binding.blackNameTV;
        whiteCaptured = binding.whiteCaptured;
        blackCaptured = binding.blackCaptured;
        whiteValue = binding.whiteValue;
        blackValue = binding.blackValue;

        horizontalScrollView = binding.scrollView;

        whiteTimeTV = binding.whiteTimeTV;
        blackTimeTV = binding.blackTimeTV;

        dataManager = new DataManager(requireContext());

        clipboard = (ClipboardManager) requireActivity().getSystemService(CLIPBOARD_SERVICE);

        boolean newGame = args.getBoolean(Constants.NEW_GAME_KEY);

        gameLogic = new GameLogic(this, requireContext(), chessBoard, newGame);
        initializeData();

        if (args.containsKey(Constants.FEN_KEY)) {
            String FEN = args.getString(Constants.FEN_KEY);
            gameLogic = new GameLogic(this, requireContext(), chessBoard, FEN);
            resetTimer();
            newGame = false;
        }

        binding.btnSaveExit.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnReset.setOnClickListener(v -> {
            resetTimer();
            gameLogic.reset();
        });
        binding.btnCopyPgn.setOnClickListener(v -> shareContent(requireContext(), "PGN", gameLogic.getPGN().toString()));
        binding.btnCopyFen.setOnClickListener(v -> shareContent(requireContext(), "FEN", gameLogic.getBoardModel().toFEN(gameLogic)));

        btn_draw.setOnClickListener(v -> createDialog("Draw", "Are you sure you want to draw?", (d, i) -> {
            gameLogic.getPGN().setTermination("Draw by agreement");
            gameLogic.terminateGame(ChessState.DRAW);
        }));
        btn_resign.setOnClickListener(v -> createDialog("Resign", "Are you sure you want to resign?", (d, i) -> {
            gameLogic.getPGN().setTermination(opponentPlayer(gameLogic.playerToPlay()) + " won by Resignation");
            gameLogic.terminateGame(ChessState.RESIGN);
        }));
        btn_undo_move.setOnClickListener(v -> gameLogic.undoLastMove());

        if (newGame) resetTimer();

        if (!timerEnabled) {
            whiteTimeTV.setVisibility(View.GONE);
            blackTimeTV.setVisibility(View.GONE);
        } else if (chessTimer != null) chessTimer.startTimer();

        updateViews();
        if (args.getBoolean(Constants.SINGLE_PLAYER, false))
            gameLogic.setBotPlayer(args.getBoolean(Constants.PLAY_AS_WHITE, true) ? Player.BLACK : Player.WHITE);
    }

    private void initializeData() {
        timerEnabled = dataManager.getBoolean(DataManager.TIMER);
        chessBoard.setInvertBlackPieces(dataManager.getBoolean(DataManager.INVERT_BLACK_PIECES));

        String white = dataManager.getString(DataManager.WHITE), black = dataManager.getString(DataManager.BLACK);
        Player.WHITE.setName(white);
        Player.BLACK.setName(black);

//        if (timerEnabled)
//            chessTimer = new ChessTimer(this, gameLogic, dataManager.getWhiteTimeLeft(), dataManager.getBlackTimeLeft());

//        if ((boardModel == null || pgn == null) && timerEnabled)
//            chessTimer = new ChessTimer(gameLogic, requireContext(), whiteTimeTV, blackTimeTV, minutesSecondsToMillis(dataManager.getTimerMinutes(), dataManager.getTimerSeconds()));

        whiteName.setText(white);
        blackName.setText(black);
    }

    /**
     * Reset timer
     */
    public void resetTimer() {
        if (timerEnabled) {
            if (chessTimer != null) chessTimer.stopTimer();
//            chessTimer = new ChessTimer(gameLogic, requireContext(), whiteTimeTV, blackTimeTV, minutesSecondsToMillis(dataManager.getTimerMinutes(), dataManager.getTimerSeconds()));
//            chessTimer.startTimer();
        }
    }

    /**
     * Load the game in LoadGameFragment
     */
    private void loadGame() {
        Bundle args = new Bundle();
        if (pgn.isFENEmpty()) {
            String opening, eco;
            long start = System.nanoTime();
            Openings openings = Openings.getInstance(requireContext());
            String openingResult = openings.searchOpening(pgn.getUCIMoves());
            long end = System.nanoTime();

            String[] split = openingResult.split(Openings.separator);
            int lastBookMove = Integer.parseInt(split[0]);
            if (lastBookMove != -1 && split.length == 3) {
                HomeFragment.printTime(TAG, "searching opening", end - start, lastBookMove);
                eco = split[1];
                opening = split[2];
                pgn.setLastBookMoveNo(lastBookMove);
                pgn.addTag(PGN.TAG_ECO, eco);
                pgn.addTag(PGN.TAG_OPENING, opening);
                for (int i = 0; i <= lastBookMove; i++)
                    pgn.getPGNData().addAnnotation(i, Annotation.BOOK);
            } else {
                opening = eco = "";
                Log.d(TAG, String.format("readPGN: Opening not found!\n%s\nMoves: %s", Arrays.toString(split), pgn.getUCIMoves().subList(0, Math.min(pgn.getUCIMoves().size(), 10))));
            }
            args.putSerializable(Constants.PARSED_GAME_KEY, new ParsedGame(gameLogic.getBoardModelStack(), gameLogic.getFENs(), gameLogic.getPGN(), eco, opening));
        }
        args.putBoolean(Constants.FILE_EXISTS_KEY, true);
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    /**
     * Terminate game, disable buttons and timer
     */
    @Override
    public void terminateGame(String termination) {
        this.termination = termination;
        if (timerEnabled && chessTimer != null) chessTimer.stopTimer();
        chessBoard.invalidate();

        btn_resign.setEnabled(false);
        btn_draw.setEnabled(false);
        setImageButtonAlpha(btn_resign);
        setImageButtonAlpha(btn_draw);

        pgn = gameLogic.getPGN();
        showGameOverDialog();
    }

    @Override
    public boolean saveProgress() {
        return true;
    }

    /**
     * Update all the views in the fragment
     */
    @Override
    @SuppressLint("SetTextI18n")
    public void updateViews() {
        if (gameLogic == null) return;
        StringBuilder whiteText = new StringBuilder(), blackText = new StringBuilder();
        int blackCapturedValue = 0, whiteCapturedValue = 0, difference;
        PGN_textView.setText(gameLogic.getPGN().getPGNMoves());
        horizontalScrollView.post(() -> horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));

        if (gameLogic.isGameTerminated()) gameStateView.setText(termination);
        else gameStateView.setText(gameLogic.playerToPlay() + "'s turn");

        whiteCaptured.setText("");
        blackCaptured.setText("");

        whiteValue.setText("");
        blackValue.setText("");

        ArrayList<Piece> capturedPieces = gameLogic.getBoardModel().getCapturedPieces();
        for (Piece piece : capturedPieces) {
            if (piece.isWhite()) {
                blackText.append(piece.getUnicode());
                blackCapturedValue += piece.getRank().getValue();
            } else {
                whiteText.append(piece.getUnicode());
                whiteCapturedValue += piece.getRank().getValue();
            }
        }
        difference = Math.abs(blackCapturedValue - whiteCapturedValue);

        if (difference != 0) {
            if (whiteCapturedValue > blackCapturedValue) whiteValue.setText(" +" + difference);
            else blackValue.setText(" +" + difference);
        }

        whiteCaptured.setText(whiteText);
        blackCaptured.setText(blackText);

        btn_undo_move.setEnabled(!gameLogic.isGameTerminated() && gameLogic.getBoardModelStack().size() > 1);

        setImageButtonAlpha(btn_undo_move);
        setImageButtonAlpha(btn_resign);
        setImageButtonAlpha(btn_draw);
    }

    private void setImageButtonAlpha(ImageButton button) {
        button.setAlpha(button.isEnabled() ? 1f : 0.5f);
    }

    public void showGameOverDialog() {
        GameOverDialog gameOverDialog = new GameOverDialog(requireContext(), pgn);
        gameOverDialog.show();
        gameOverDialog.findViewById(R.id.btn_view_game).setOnClickListener(v -> {
            gameOverDialog.dismiss();
            loadGame();
        });

//        gameOverDialog.setOnDismissListener(dialogInterface -> finish());
    }

    private void createDialog(String title, String message, DialogInterface.OnClickListener positiveOnClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title).setMessage(message).setPositiveButton("Yes", positiveOnClickListener).setNegativeButton("No", (d, i) -> d.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(d -> {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor("#EFEFEF"));
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#EFEFEF"));
        });
        alertDialog.show();
    }
}