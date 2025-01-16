package com.drdedd.simplichess.fragments;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplichess.data.Regexes.FENRegex;
import static com.drdedd.simplichess.data.Regexes.resultPattern;
import static com.drdedd.simplichess.misc.MiscMethods.shareContent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.databinding.FragmentLoadGameBinding;
import com.drdedd.simplichess.game.BoardModel;
import com.drdedd.simplichess.game.ParsedGame;
import com.drdedd.simplichess.game.gameData.Annotation;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pgn.PGN;
import com.drdedd.simplichess.game.pgn.PGNData;
import com.drdedd.simplichess.game.pgn.PGNParser;
import com.drdedd.simplichess.game.pieces.King;
import com.drdedd.simplichess.game.pieces.Pawn;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.interfaces.GameLogicInterface;
import com.drdedd.simplichess.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplichess.misc.Constants;
import com.drdedd.simplichess.views.CompactBoard;
import com.drdedd.simplichess.views.EvalBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Fragment to load and view chess games
 */
public class LoadGameFragment extends Fragment {
    private static final String TAG = "LoadGameFragment";
    private static final int gone = View.GONE, visible = View.VISIBLE;
    private FragmentLoadGameBinding binding;
    private ClipboardManager clipboardManager;
    private NavController navController;
    private PGN pgn;
    private String pgnString;
    private Dialog dialog;
    private EditText pgnTxt;
    private boolean gameLoaded = false, fileExists;
    private final ActivityResultLauncher<Intent> fileOpenResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == AppCompatActivity.RESULT_OK) {
            Intent i = r.getData();
            if (i != null) openFile(i.getData());
        } else Log.i(TAG, "ActivityResult: Result not ok!");
    });

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadGameBinding.inflate(inflater, container, false);
        clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        navController = Navigation.findNavController(container);
        binding.notLoadedLayout.findViewById(R.id.btn_load_game).setOnClickListener(v -> inputPGNDialog());

        Bundle args = getArguments();
        if (args != null) {
            fileExists = args.getBoolean(Constants.FILE_EXISTS_KEY, false);
            if (args.containsKey(Constants.PARSED_GAME_KEY)) {
                ParsedGame parsedGame = (ParsedGame) args.getSerializable(Constants.PARSED_GAME_KEY);
                if (parsedGame != null) try {
                    pgn = parsedGame.getPGN();
                    pgnString = pgn.toString();
                    binding.analysisBoard.setOpening(parsedGame.getECO(), parsedGame.getOpening());
                    loadGameView(new ArrayList<>(parsedGame.getBoardModelStack()), new ArrayList<>(parsedGame.getFENs()), args);
                    gameLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "onCreateView: Error while loading game", e);
                }
            } else if (args.containsKey(Constants.PGN_CONTENT_KEY)) {
                String pgnContent = args.getString(Constants.PGN_CONTENT_KEY);
                readPGN(pgnContent);
            }
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
        refresh();
    }

    /**
     * Refreshes fragment layouts visibility
     */
    private void refresh() {
        if (gameLoaded) {
            binding.loadedLayout.setVisibility(visible);
            binding.notLoadedLayout.setVisibility(gone);
            if (pgn.hasNoEval()) binding.gameEvalBar.setVisibility(gone);
            Log.d(TAG, "onViewCreated: Game Loaded");
        } else {
            binding.loadedLayout.setVisibility(gone);
            binding.notLoadedLayout.setVisibility(visible);
        }
    }

    /**
     * Reads, validates and parses PGN content
     *
     * @param pgnContent Content of the PGN
     */
    private void readPGN(String pgnContent) {
        Handler pgnParserHandler = new Handler(requireActivity().getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(@NonNull Message message) {
                super.handleMessage(message);
                Bundle data = message.getData();
                if (data.getBoolean(Constants.READ_RESULT_KEY)) {
                    Log.v(TAG, "handleMessage: No errors in PGN");
                    if (dialog != null) dialog.dismiss();

//                    if (data.getBoolean(PARSE_RESULT_FLAG)) {
                    ParsedGame parsedGame = (ParsedGame) data.getSerializable(Constants.PARSED_GAME_KEY);
                    if (parsedGame != null) {
                        pgn = parsedGame.getPGN();
                        binding.analysisBoard.setOpening(parsedGame.getECO(), parsedGame.getOpening());
                        pgnString = pgn.toString();
                        gameLoaded = true;
                        loadGameView(new ArrayList<>(parsedGame.getBoardModelStack()), new ArrayList<>(parsedGame.getFENs()), data);
                    } else Log.d(TAG, "handleMessage: Parsed game is null!");
//                    } else {
//                        Log.d(TAG, "handleMessage: Game not parsed!");
//                        gameLoaded = false;
//                    }
                } else {
                    Log.d(TAG, "handleMessage: Invalid PGN!");
                    Toast.makeText(requireContext(), "Invalid PGN!", Toast.LENGTH_SHORT).show();
                    if (pgnTxt != null) pgnTxt.getText().clear();
                    gameLoaded = false;
                }

                if (data.containsKey(Constants.ERROR_KEY)) {
                    String error;
                    error = data.getString(Constants.ERROR_KEY, "Invalid PGN!");
                    Log.v(TAG, error);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }

                refresh();
            }
        };

        //        PGNParser pgnParser = new PGNParser(requireContext(), tagsMap, moves, commentsMap, moveAnnotationMap, alternateMoveSequence, evalMap, pgnContent, pgnParserHandler);
        PGNParser pgnParser = new PGNParser(requireContext(), pgnContent, pgnParserHandler);
        pgnParser.start();
    }

    /**
     * Opens dialog to input and load PGN or FEN
     */
    private void inputPGNDialog() {
        dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_load_game);
        dialog.setTitle("Load Game");
        pgnTxt = dialog.findViewById(R.id.load_pgn_txt);

        pgnTxt.setOnClickListener(v -> pgnTxt.selectAll());

        dialog.findViewById(R.id.openFile).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(Constants.PGN_MIME_TYPE);
            fileOpenResultLauncher.launch(intent);
        });

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> {
//            navController.popBackStack();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.load).setOnClickListener(v -> {
            if (TextUtils.isEmpty(pgnTxt.getText())) {
                pgnTxt.setError("Please enter a PGN");
                pgnTxt.requestFocus();
                return;
            }
            String pgnContent = pgnTxt.getText().toString();

            String FEN = isFEN(pgnContent);
            if (!FEN.isEmpty()) {
                Bundle args = new Bundle();
                args.putString(Constants.FEN_KEY, FEN);
                args.putBoolean(Constants.NEW_GAME_KEY, false);
                Log.v(TAG, "inputPGNDialog: Found FEN, navigating to GameFragment");
                dialog.dismiss();
                navController.navigate(R.id.nav_game, args);
                return;
            }

            fileExists = false;
            readPGN(pgnContent);
        });

        ImageButton paste = dialog.findViewById(R.id.paste_from_clipboard);
        boolean hasContent = false;
        if (clipboardManager.hasPrimaryClip())
            if (Objects.requireNonNull(clipboardManager.getPrimaryClipDescription()).hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                hasContent = true;
        if (!hasContent) {
            paste.setEnabled(false);
            paste.setAlpha(0.5f);
        }
        paste.setOnClickListener(v -> {
            ClipData.Item item = Objects.requireNonNull(clipboardManager.getPrimaryClip()).getItemAt(0);
            pgnTxt.setText(item.getText());
        });
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        Objects.requireNonNull(dialog.getWindow()).setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    /**
     * Loads the game objects to view the game
     *
     * @param boardModels List of BoardModels
     * @param FENs        List of FENs of BoardModels
     */
    private void loadGameView(ArrayList<BoardModel> boardModels, ArrayList<String> FENs, Bundle data) {
        GameViewer gameViewer = new GameViewer(requireActivity(), binding, boardModels, FENs, pgn);
        binding.movePrevious.setOnClickListener(v -> {
            gameViewer.movePrevious();
            if (gameViewer.autoplayRunning) gameViewer.stopAutoplay();
        });
        binding.moveNext.setOnClickListener(v -> {
            gameViewer.moveNext();
            if (gameViewer.autoplayRunning) gameViewer.stopAutoplay();
        });
        binding.movePrevious.setOnLongClickListener(v -> {
            if (gameViewer.autoplayRunning) gameViewer.stopAutoplay();
            return gameViewer.moveToFirst();
        });
        binding.moveNext.setOnLongClickListener(v -> {
            if (gameViewer.autoplayRunning) gameViewer.stopAutoplay();
            return gameViewer.moveToLast();
        });
        binding.btnCopyPgn.setOnClickListener(v -> shareContent(requireContext(), "PGN", pgnString));
        binding.btnCopyFen.setOnClickListener(v -> shareContent(requireContext(), "FEN", gameViewer.getFEN()));

        if (data.containsKey(Constants.WHITE_ACCURACY) && data.containsKey(Constants.BLACK_ACCURACY)) {
            binding.whiteName.setText(pgn.getWhite());
            binding.blackName.setText(pgn.getBlack());

            binding.whiteInaccuracy.setText(String.valueOf(data.getInt(Constants.WHITE_INACCURACY)));
            binding.blackInaccuracy.setText(String.valueOf(data.getInt(Constants.BLACK_INACCURACY)));

            binding.whiteMistake.setText(String.valueOf(data.getInt(Constants.WHITE_MISTAKE)));
            binding.blackMistake.setText(String.valueOf(data.getInt(Constants.BLACK_MISTAKE)));

            binding.whiteBlunder.setText(String.valueOf(data.getInt(Constants.WHITE_BLUNDER)));
            binding.blackBlunder.setText(String.valueOf(data.getInt(Constants.BLACK_BLUNDER)));

            binding.whiteACPL.setText(String.valueOf(data.getInt(Constants.WHITE_ACPL)));
            binding.blackACPL.setText(String.valueOf(data.getInt(Constants.BLACK_ACPL)));

            binding.whiteAccuracy.setText(String.format(Locale.ENGLISH, "%d%%", data.getInt(Constants.WHITE_ACCURACY)));
            binding.blackAccuracy.setText(String.format(Locale.ENGLISH, "%d%%", data.getInt(Constants.BLACK_ACCURACY)));
        } else binding.accuracyReport.setVisibility(View.GONE);

        if (fileExists) binding.btnSavePgn.setVisibility(gone);
        else binding.btnSavePgn.setOnClickListener(v -> savePGN());
    }

    /**
     * Save the loaded PGN to PGN library
     */
    private void savePGN() {
        DataManager dataManager = new DataManager(requireContext());
        String white = pgn.getWhite();
        String black = pgn.getBlack();
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
        String name = "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn";
        if (dataManager.savePGN(pgn, name))
            Toast.makeText(requireContext(), "Saved PGN", Toast.LENGTH_SHORT).show();
        binding.btnSavePgn.setVisibility(gone);
    }

    /**
     * Checks whether the given input is FEN
     *
     * @param PGN PGN input
     * @return <code>String</code> - FEN or empty string
     */
    private String isFEN(String PGN) {
        return PGN.matches(FENRegex) ? PGN.trim() : "";
    }

    private void openFile(Uri uri) {
        if (uri != null) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = bf.readLine()) != null) stringBuilder.append(line).append('\n');

                String pgnContent = stringBuilder.toString();
                Log.d(TAG, "openFile: Content:\n" + pgnContent);
                readPGN(pgnContent);
            } catch (IOException e) {
                Log.e(TAG, "openFile: Error while reading uri file", e);
            }
        } else Log.i(TAG, "openFile: Uri is null!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gameLoaded = false;
        if (dialog != null) dialog.dismiss();
    }

    /**
     * Game viewer to display and view game
     * {@inheritDoc}
     */
    static class GameViewer implements GameLogicInterface, PGNRecyclerViewInterface {
        private static final String TAG = "GameViewer";
        private final ArrayList<BoardModel> boardModels;
        private final ArrayList<String> FENs;
        private final CompactBoard compactBoard;
        private final boolean vibrationEnabled, sound, animate;
        private final int length;
        private Vibrator vibrator;
        private final FragmentLoadGameBinding binding;
        private final RecyclerView pgnRecyclerView;
        private final EvalBar evalBar;
        private final LinkedHashMap<Integer, String> commentsMap, alternateMoveSequence, evalMap;
        private final LinkedHashMap<Integer, Annotation> annotationMap;
        private BoardModel boardModel;
        private int pointer, previousPosition;
        private final ImageButton moveAutoplay;
        private boolean autoplayRunning, reverse;
        private final long delay = 800;
        private CountDownTimer countDownTimer;
        private MediaPlayer mediaPlayer;

        /**
         * @param binding     LoadGame fragment binding
         * @param boardModels List of board models
         * @param pgn         PGN of the game
         */
        public GameViewer(Activity activity, FragmentLoadGameBinding binding, ArrayList<BoardModel> boardModels, ArrayList<String> FENs, PGN pgn) {
            DataManager dataManager = new DataManager(activity);
            this.boardModels = boardModels;
            this.FENs = FENs;
            this.binding = binding;

            PGNData data = pgn.getPGNData();
            this.commentsMap = data.getCommentsMap();
            this.annotationMap = data.getAnnotationMap();
            this.alternateMoveSequence = data.getAlternateMoveSequence();
            this.evalMap = data.getEvalMap();

            boardModel = boardModels.get(0);
            compactBoard = binding.analysisBoard;
            pgnRecyclerView = binding.pgnRecyclerView;
            evalBar = binding.gameEvalBar;

            Resources resources = activity.getResources();
            String draw = resources.getString(R.string.draw), victory = resources.getString(R.string.trophy);

            String result = pgn.getResult();
            if (result.isEmpty()) {
                Matcher matcher = resultPattern.matcher(pgn.toString());
                if (matcher.find()) result = matcher.group();
            }

            String whiteName = pgn.getWhite(), blackName = pgn.getBlack();
            switch (result) {
                case PGN.RESULT_DRAW:
                    whiteName += " " + draw;
                    blackName += " " + draw;
                    break;
                case PGN.RESULT_WHITE_WON:
                    whiteName += " " + victory;
                    break;
                case PGN.RESULT_BLACK_WON:
                    blackName += " " + victory;
                    break;
            }
            compactBoard.setPlayersData(pgn.getPGNData().getTagOrDefault(PGN.TAG_WHITE_TITLE, ""), whiteName, pgn.getPGNData().getTagOrDefault(PGN.TAG_BLACK_TITLE, ""), blackName);

            length = boardModels.size();
            pointer = 0;

            vibrationEnabled = dataManager.getBoolean(DataManager.VIBRATION);
            if (vibrationEnabled) vibrator = (Vibrator) activity.getSystemService(VIBRATOR_SERVICE);
            sound = dataManager.getBoolean(DataManager.SOUND);
            if (sound) mediaPlayer = MediaPlayer.create(activity, R.raw.move_sound);
            animate = dataManager.getBoolean(DataManager.ANIMATION);

            compactBoard.setBoardData(this, true);
            compactBoard.getBoard().setAnalysis(true);
            update();

            pgnRecyclerView.setAdapter(new PGN.PGNRecyclerViewAdapter(activity, pgn, this));
            pgnRecyclerView.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));

            autoplayRunning = false;
            moveAutoplay = binding.moveAutoplay;
            moveAutoplay.setOnClickListener(v -> {
                if (autoplayRunning) stopAutoplay();
                else {
                    countDownTimer = new CountDownTimer(Long.MAX_VALUE, delay) {
                        @Override
                        public void onTick(long l) {
                            moveNext();
//                            Log.d(TAG, "GameViewer: Autoplayed move");
                        }

                        @Override
                        public void onFinish() {
                            Log.e(TAG, "onFinish: Maximum time limit reached");
//                            Toast.makeText(activity, "Maximum time limit reached!", Toast.LENGTH_SHORT).show();
                            cancel();
                        }
                    }.start();
                    autoplayRunning = true;
                    moveAutoplay.setImageResource(R.drawable.ic_pause);
//                    Log.d(TAG, "GameViewer: Autoplay started");
                }
            });
        }

        public void stopAutoplay() {
            if (!autoplayRunning) return;
            autoplayRunning = false;
            moveAutoplay.setImageResource(R.drawable.ic_play);
            if (countDownTimer != null) countDownTimer.cancel();
//            Log.d(TAG, "stopAutoplay: Autoplay stopped");
        }

        @Override
        public Piece pieceAt(int row, int col) {
            return boardModel.pieceAt(row, col);
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
        public void terminateByTimeOut(Player player) {
        }

        @Override
        public BoardModel getBoardModel() {
            return boardModel;
        }

        @Override
        public HashMap<String, HashSet<Integer>> getAllLegalMoves() {
            return null;
        }

        @Override
        public boolean isWhiteToPlay() {
            return false;
        }

        @Override
        public boolean isGameTerminated() {
            return false;
        }

        @Override
        public boolean isPieceToPlay(Piece piece) {
            return false;
        }

        private void movePrevious() {
            if (pointer > 0) {
                previousPosition = pointer;
                pointer--;
                reverse = true;
                update();
            }
        }

        private void moveNext() {
            if (pointer < length - 1) {
                previousPosition = pointer;
                pointer++;
                update();
            } else stopAutoplay();
        }

        private boolean moveToFirst() {
            previousPosition = pointer;
            pointer = 0;
            update();
            return true;
        }

        private boolean moveToLast() {
            previousPosition = pointer;
            pointer = length - 1;
            update();
            return true;
        }

        @Override
        public void jumpToMove(int position) {
            if (position < length - 1) {
                previousPosition = pointer;
                pointer = position + 1;
                update();
            }
        }

        @Override
        public int getPosition() {
            return pointer - 1;
        }

        /**
         * Updates board, moves, check status and other views
         */
        @SuppressLint("SetTextI18n")
        private void update() {
            pgnRecyclerView.scrollToPosition(pointer == 0 ? 0 : pointer - 1);
            RecyclerView.ViewHolder holder = pgnRecyclerView.findViewHolderForAdapterPosition(previousPosition - 1);
            if (holder != null)
                holder.itemView.findViewById(R.id.move).setBackgroundResource(R.drawable.pgn_move_bg);

            pgnRecyclerView.post(() -> {
                RecyclerView.ViewHolder holder1 = pgnRecyclerView.findViewHolderForAdapterPosition(pointer - 1);
                if (holder1 != null)
                    holder1.itemView.findViewById(R.id.move).setBackgroundResource(R.drawable.pgn_move_highlight);
            });

            if (animate && reverse)
                compactBoard.initializeReverseAnimation(boardModel.toSquare, boardModel.fromSquare);
            boardModel = boardModels.get(pointer);
            Log.d(TAG, "update: FEN of current boardModel:" + FENs.get(pointer));
            compactBoard.setAnnotation(-1);
            if (annotationMap.containsKey(pointer - 1)) {
                Annotation annotation = annotationMap.get(pointer - 1);
                Log.d(TAG, "update: Annotation: " + annotation);
                if (annotation != null) compactBoard.setAnnotation(annotation.getResID());
            }
            compactBoard.invalidate();
            if (animate && !reverse)
                compactBoard.initializeAnimation(boardModel.fromSquare, boardModel.toSquare);

            long start = System.nanoTime();
            boolean isChecked = false;
            King whiteKing = boardModel.getWhiteKing();
            King blackKing = boardModel.getBlackKing();
            Player.WHITE.setInCheck(false);
            Player.BLACK.setInCheck(false);
            if (whiteKing.isChecked(this)) {
                isChecked = true;
                Player.WHITE.setInCheck(true);
                Log.i(TAG, "isChecked: White King checked");
            }
            if (blackKing.isChecked(this)) {
                isChecked = true;
                Player.BLACK.setInCheck(true);
                Log.i(TAG, "isChecked: Black King checked");
            }
            long end = System.nanoTime();
            HomeFragment.printTime(TAG, "identifying checks", end - start, -1);

            if (vibrationEnabled && isChecked) {
                long vibrationDuration = 150;
                vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            if (sound && pointer != 0) mediaPlayer.start();

            binding.movePrevious.setAlpha(pointer == 0 ? 0.5f : 1f);
            binding.moveNext.setAlpha(pointer == length - 1 ? 0.5f : 1f);

            int blackCapturedValue = 0, whiteCapturedValue = 0, difference;

            compactBoard.setWhiteValue("");
            compactBoard.setBlackValue("");

            ArrayList<Piece> capturedPieces = boardModel.getCapturedPieces();
            for (Piece piece : capturedPieces)
                if (piece.isWhite()) blackCapturedValue += piece.getRank().getValue();
                else whiteCapturedValue += piece.getRank().getValue();
            difference = Math.abs(blackCapturedValue - whiteCapturedValue);

            if (difference != 0) {
                if (whiteCapturedValue > blackCapturedValue)
                    compactBoard.setWhiteValue("+" + difference);
                else compactBoard.setBlackValue("+" + difference);
            }

            if (commentsMap.containsKey(pointer - 1))
                Log.d(TAG, "update: Comment: " + commentsMap.get(pointer - 1));
            if (alternateMoveSequence.containsKey(pointer - 1))
                Log.d(TAG, "update: Alternate Move Sequence: " + alternateMoveSequence.get(pointer - 1));
            if (evalMap.containsKey(pointer - 1)) {
                String eval = evalMap.get(pointer - 1);
                evalBar.setEvaluation(eval);
                Log.d(TAG, "update: Eval: " + eval);
            }
            reverse = false;
        }

        public String getFEN() {
            return FENs.get(pointer);
        }
    }
}