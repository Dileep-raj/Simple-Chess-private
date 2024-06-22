package com.drdedd.simplechess_temp.fragments;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplechess_temp.data.MoveAnnotation.BOOK;
import static com.drdedd.simplechess_temp.data.Regexes.FENPattern;
import static com.drdedd.simplechess_temp.data.Regexes.commentNumberStrictRegex;
import static com.drdedd.simplechess_temp.data.Regexes.moveAnnotationPattern;
import static com.drdedd.simplechess_temp.data.Regexes.moveNumberRegex;
import static com.drdedd.simplechess_temp.data.Regexes.moveNumberStrictRegex;
import static com.drdedd.simplechess_temp.data.Regexes.numberedAnnotationRegex;
import static com.drdedd.simplechess_temp.data.Regexes.resultPattern;
import static com.drdedd.simplechess_temp.data.Regexes.resultRegex;
import static com.drdedd.simplechess_temp.data.Regexes.singleMoveStrictRegex;
import static com.drdedd.simplechess_temp.data.Regexes.startingMovePattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.ChessBoard;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.data.MoveAnnotation;
import com.drdedd.simplechess_temp.data.Openings;
import com.drdedd.simplechess_temp.databinding.FragmentLoadGameBinding;
import com.drdedd.simplechess_temp.dialogs.ProgressBarDialog;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LoadGameFragment extends Fragment {
    private static final String TAG = "LoadGameFragment";
    protected static final String LOAD_GAME_KEY = "LoadGame", LOAD_PGN_KEY = "LoadPGN", PGN_FILE_KEY = "PGNFile", FILE_EXISTS_KEY = "FileExists";
    public LinkedHashMap<String, String> tagsMap;
    public LinkedList<String> moves, invalidWords;
    public LinkedHashMap<Integer, String> commentsMap, moveAnnotationMap, alternateMoveSequence;
    private FragmentLoadGameBinding binding;
    boolean gameLoaded = false, fileExists;
    private final static int gone = View.GONE, visible = View.VISIBLE;
    private ClipboardManager clipboardManager;
    private NavController navController;
    private PGN pgn;
    private ProgressBarDialog progressBarDialog;
    private Dialog dialog;
    private EditText pgnTxt;
    private String opening;
//    private ChessBoard analysisBoard;

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadGameBinding.inflate(inflater, container, false);
        clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        navController = Navigation.findNavController(container);
        binding.notLoadedLayout.findViewById(R.id.btn_load_game).setOnClickListener(v -> inputPGNDialog());
        tagsMap = new LinkedHashMap<>();
        moves = new LinkedList<>();
        commentsMap = new LinkedHashMap<>();
        moveAnnotationMap = new LinkedHashMap<>();
        alternateMoveSequence = new LinkedHashMap<>();
        invalidWords = new LinkedList<>();
        Bundle args = getArguments();
        if (args != null) {
            fileExists = args.getBoolean(FILE_EXISTS_KEY, false);
            if (args.containsKey(LOAD_GAME_KEY) && args.containsKey(LOAD_PGN_KEY)) {
                Stack<BoardModel> boardModelStack = (Stack<BoardModel>) args.getSerializable(LOAD_GAME_KEY);
                PGN pgn = (PGN) args.getSerializable(LOAD_PGN_KEY);
                opening = args.getString(GameFragment.OPENING_KEY, "");
                if (boardModelStack != null) try {
                    this.pgn = pgn;
                    if (!opening.isEmpty()) binding.openingName.setText(opening);
                    loadGameView(new ArrayList<>(boardModelStack));
                    gameLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "onCreateView: Error while loading game", e);
                }
            } else if (args.containsKey(PGN_FILE_KEY)) {
                String pgn = args.getString(PGN_FILE_KEY);
                gameLoaded = true;
                long s = System.nanoTime();
                readPGN(pgn);
                long e = System.nanoTime();
                if (pgn != null)
                    HomeFragment.printTime(TAG, "reading parsed PGN", e - s, pgn.length());
                parsePGN();
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
        if (gameLoaded) {
            binding.loadedLayout.setVisibility(visible);
            binding.notLoadedLayout.setVisibility(gone);
            Log.d(TAG, "onViewCreated: Game Loaded");
        } else {
            binding.loadedLayout.setVisibility(gone);
            binding.notLoadedLayout.setVisibility(visible);
            inputPGNDialog();
        }
    }

    private void inputPGNDialog() {
        dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_load_game);
        dialog.setTitle("Load Game");
        pgnTxt = dialog.findViewById(R.id.load_pgn_txt);

        pgnTxt.setOnClickListener(v -> pgnTxt.selectAll());

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> {
            navController.popBackStack();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.load).setOnClickListener(v -> {
            if (TextUtils.isEmpty(pgnTxt.getText())) {
                pgnTxt.setError("Please enter a PGN");
                pgnTxt.requestFocus();
                return;
            }
            String pgn = pgnTxt.getText().toString();

            String FEN = isFEN(pgn);
            if (!FEN.isEmpty()) {
                Bundle args = new Bundle();
                args.putString(GameFragment.FEN_KEY, FEN);
                args.putBoolean(GameFragment.NEW_GAME_KEY, false);
                Log.v(TAG, "inputPGNDialog: Found FEN, navigating to GameFragment");
                dialog.dismiss();
                navController.navigate(R.id.nav_game, args);
                return;
            }

            moves.clear();
            tagsMap.clear();
            commentsMap.clear();
            moveAnnotationMap.clear();
            alternateMoveSequence.clear();

            long start = System.nanoTime();
            boolean result = readPGN(pgn);
            long end = System.nanoTime();

            if (result) {
                HomeFragment.printTime(TAG, "Reading PGN", end - start, pgn.length());
                Log.v(TAG, "inputPGNDialog: No errors in PGN");
                dialog.dismiss();
                fileExists = false;
                parsePGN();
            } else {
                Log.v(TAG, "inputPGNDialog: Invalid PGN!");
                Toast.makeText(requireContext(), "Invalid PGN!", Toast.LENGTH_SHORT).show();
                pgnTxt.getText().clear();
            }
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

    private void loadGameView(ArrayList<BoardModel> boardModels) {
        GameViewer gameViewer = new GameViewer(requireActivity(), binding, boardModels, pgn);
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
        binding.btnCopyPgn.setOnClickListener(v -> copyToClipboard("PGN", pgn.toString()));
        binding.btnCopyFen.setOnClickListener(v -> copyToClipboard("FEN", gameViewer.boardModel.toFEN()));
        if (fileExists) binding.btnSavePgn.setVisibility(gone);
        else binding.btnSavePgn.setOnClickListener(v -> savePGN());
    }

    private void savePGN() {
        DataManager dataManager = new DataManager(requireContext());
        String white = tagsMap.containsKey(PGN.TAG_WHITE) ? tagsMap.get(PGN.TAG_WHITE) : "White";
        String black = tagsMap.containsKey(PGN.TAG_BLACK) ? tagsMap.get(PGN.TAG_BLACK) : "Black";
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
        String name = "pgn_" + white + "vs" + black + "_" + date.format(new Date()) + ".pgn";
        if (dataManager.savePGN(pgn, name))
            Toast.makeText(requireContext(), "Saved PGN", Toast.LENGTH_SHORT).show();
        binding.btnSavePgn.setVisibility(View.GONE);
    }

    private void copyToClipboard(String label, String content) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, content));
        Toast.makeText(requireContext(), label + " copied", Toast.LENGTH_SHORT).show();
    }

    private void parsePGN() {
        progressBarDialog = new ProgressBarDialog(requireContext(), "Loading PGN");
        progressBarDialog.show();
        boolean newGame = true;
        Bundle args = new Bundle();
        if (tagsMap.containsKey(PGN.TAG_SET_UP) && tagsMap.containsKey(PGN.TAG_FEN))
            if ("1".equals(tagsMap.get(PGN.TAG_SET_UP))) {
                String FEN = tagsMap.get(PGN.TAG_FEN);
                if (FEN != null && !FEN.isEmpty()) {
                    args.putString(GameFragment.LOAD_GAME_KEY, FEN);
                    newGame = false;
                }
            }
        args.putBoolean(GameFragment.NEW_GAME_KEY, newGame);
        args.putBoolean(GameFragment.LOAD_PGN_KEY, true);
        args.putSerializable(GameFragment.MOVES_LIST_KEY, moves);
        args.putSerializable(GameFragment.TAGS_MAP_KEY, tagsMap);
        args.putSerializable(GameFragment.COMMENTS_KEY, commentsMap);
        args.putSerializable(GameFragment.ANNOTATION_MAP_KEY, moveAnnotationMap);
        args.putSerializable(GameFragment.ALTERNATE_MOVE_SEQUENCE_KEY, alternateMoveSequence);
        args.putString(GameFragment.OPENING_KEY, opening);
        args.putBoolean(FILE_EXISTS_KEY, fileExists);
        if (navController != null) {
            navController.popBackStack();
            navController.navigate(R.id.nav_game, args);
        }
    }

    private String isFEN(String PGN) {
        Matcher FENmatcher = FENPattern.matcher(PGN);
        String FEN = "";
        if (FENmatcher.find()) FEN = FENmatcher.group();
        return FEN;
    }

    private boolean readPGN(String pgnContent) {
        int moveCount = -1;
        readTags(pgnContent);

        Matcher startingMoveMatcher = startingMovePattern.matcher(pgnContent);
        boolean foundMoves = startingMoveMatcher.find();
        if (!foundMoves) {
            Log.v(TAG, "readPGN: No moves found in pgn!\n" + pgnContent);
            Toast.makeText(requireContext(), "No moves in PGN!", Toast.LENGTH_SHORT).show();
            return false;
        }

        Scanner PGNReader = new Scanner(pgnContent.substring(startingMoveMatcher.start()));

//      Iterate through every word in the PGN
        while (PGNReader.hasNext()) {
            String word = null;
            try {
                word = PGNReader.next();

//              If comment is found, extract full comment
                if (word.startsWith("{")) {
                    StringBuilder comment = new StringBuilder(word);
                    while (PGNReader.hasNext()) {
                        if (word.endsWith("}")) break;
                        else if (word.contains("}")) {
                            comment = new StringBuilder(word.substring(0, word.indexOf('}') + 1));
                            if (word.contains("("))
                                extractAlternateMoves(moveCount, word.substring(word.indexOf('(')), PGNReader);
                            break;
                        }

                        word = PGNReader.next();
                        if (word.contains("}")) {
                            comment.append(' ').append(word, 0, word.indexOf('}') + 1);
                            if (word.contains("("))
                                extractAlternateMoves(moveCount, word.substring(word.indexOf('(')), PGNReader);
                            break;
                        }
                        comment.append(' ').append(word);
                    }
                    commentsMap.put(moveCount, comment.toString());
                    findMoveFeedback(comment.toString(), moveCount);
                    continue;
                }

                if (word.startsWith("(")) {
                    extractAlternateMoves(moveCount, word, PGNReader);
                    continue;
                }

//              If a move is found add move to the moves list
                if (word.matches(singleMoveStrictRegex)) {
                    String move = word.replaceAll(moveNumberRegex, "");
                    moves.add(move);
                    moveCount++;
                    findMoveFeedback(word, moveCount);
                    continue;
                }

                if (word.matches(numberedAnnotationRegex)) {
                    moveAnnotationMap.put(moveCount, MoveAnnotation.parseNumberedAnnotation(word));
                    continue;
                }

                if (word.matches(moveNumberStrictRegex) || word.matches(commentNumberStrictRegex) || word.matches(resultRegex))
                    continue;
                invalidWords.add(word + ",after move: " + moves.getLast());
            } catch (Exception e) {
                Log.e(TAG, "readPGN: Error at :" + word, e);
            }
        }

        long start = System.nanoTime();
        Openings openings = Openings.getInstance(requireContext());
        String openingResult = openings.searchOpening(moves);
        long end = System.nanoTime();

        String[] split = openingResult.split(Openings.separator);
        int lastBookMove = Integer.parseInt(split[0]);
        if (lastBookMove != -1 && split.length == 2) {
            HomeFragment.printTime(TAG, "searching opening", end - start, lastBookMove);
            opening = split[1];
            for (int i = 0; i <= lastBookMove; i++) moveAnnotationMap.put(i, BOOK);
        } else {
            opening = "";
            Log.d(TAG, "readPGN: Opening not found!\n" + Arrays.toString(split));
        }

        return true;
    }

    private void extractAlternateMoves(int moveCount, String word, Scanner PGNReader) {
        StringBuilder movesBuilder = new StringBuilder(word.substring(word.indexOf("(")));
        while (PGNReader.hasNext()) {
            if (word.endsWith(")")) break;
            word = PGNReader.next();
            movesBuilder.append(' ').append(word);
            if (word.endsWith(")")) break;
        }
        alternateMoveSequence.put(moveCount, movesBuilder.toString());
    }

    /**
     * Extracts move feedback if any
     *
     * @param word      Move word or comment
     * @param moveCount Move number
     */
    private void findMoveFeedback(String word, int moveCount) {
        String feedback = null;
        Matcher feedbackMatcher = moveAnnotationPattern.matcher(word);
        if (feedbackMatcher.find()) feedback = feedbackMatcher.group();
        if (feedback != null) moveAnnotationMap.put(moveCount, feedback);
    }

    /**
     * Extracts Tags from the PGN
     *
     * @param pgn PGN in <code>String</code> format
     */
    private void readTags(String pgn) {
        Scanner tagReader = new Scanner(pgn);
        String word = null;
        while (tagReader.hasNext()) {
            try {
                word = tagReader.next();
                if (word.startsWith("1.")) return;
                if (word.startsWith("[")) {
                    String tag = word.substring(1);
                    StringBuilder tagBuilder = new StringBuilder();
                    while (tagReader.hasNext()) {
                        word = tagReader.next();
                        tagBuilder.append(word).append(' ');
                        if (word.endsWith("]")) break;
                    }
                    String value = tagBuilder.substring(tagBuilder.indexOf("\"") + 1, tagBuilder.lastIndexOf("\""));
                    tagsMap.put(tag, value);
                }
            } catch (Exception e) {
                Log.e(TAG, "readTags: Error at : " + word, e);
            }
        }
    }

    public static String getTAG() {
        return TAG;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gameLoaded = false;
        if (progressBarDialog != null) progressBarDialog.dismiss();
        if (dialog != null) dialog.dismiss();
    }

    /**
     * Game viewer to display and view game
     * {@inheritDoc}
     */
    static class GameViewer implements BoardInterface, PGNRecyclerViewInterface {
        private static final String TAG = "GameViewer";
        private final ArrayList<BoardModel> boardModels;
        private final ChessBoard analysisBoard;
        private final boolean vibrationEnabled, sound, animate;
        private final int length;
        private Vibrator vibrator;
        private final FragmentLoadGameBinding binding;
        private final RecyclerView pgnRecyclerView;
        private final LinkedHashMap<Integer, String> commentsMap, moveAnnotationMap, alternateMoveSequence;
        private BoardModel boardModel;
        private int pointer, previousPosition;
        private final ImageButton moveAutoplay;
        private boolean autoplayRunning;
        private final long delay = 800;
        private CountDownTimer countDownTimer;
        private MediaPlayer mediaPlayer;

        /**
         * @param binding     LoadGame fragment binding
         * @param boardModels List of board models
         * @param pgn         PGN of the game
         */
        public GameViewer(Activity activity, FragmentLoadGameBinding binding, ArrayList<BoardModel> boardModels, PGN pgn) {
            DataManager dataManager = new DataManager(activity);
            this.boardModels = boardModels;
            this.binding = binding;

            this.commentsMap = pgn.commentsMap == null ? new LinkedHashMap<>() : pgn.commentsMap;
            this.moveAnnotationMap = pgn.moveAnnotationMap == null ? new LinkedHashMap<>() : pgn.moveAnnotationMap;
            this.alternateMoveSequence = pgn.alternateMoveSequence == null ? new LinkedHashMap<>() : pgn.alternateMoveSequence;

            boardModel = boardModels.get(0);
            analysisBoard = binding.analysisBoard;
            pgnRecyclerView = binding.pgnRecyclerView;

            String whiteName = pgn.getWhite();
            String blackName = pgn.getBlack();

            Resources resources = activity.getResources();
            String draw = resources.getString(R.string.draw);
            String victory = resources.getString(R.string.peace);

            String result = pgn.getResult();

            if (result.isEmpty()) {
                Matcher matcher = resultPattern.matcher(pgn.toString());
                if (matcher.find()) result = matcher.group();
            }

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

            binding.whiteNameTV.setText(whiteName);
            binding.blackNameTV.setText(blackName);

            length = boardModels.size();
            pointer = 0;

            vibrationEnabled = dataManager.getVibration();
            if (vibrationEnabled) vibrator = (Vibrator) activity.getSystemService(VIBRATOR_SERVICE);
            sound = dataManager.getSound();
            if (sound) mediaPlayer = MediaPlayer.create(activity, R.raw.move_sound);
            animate = dataManager.getAnimation();

            analysisBoard.boardInterface = this;
            analysisBoard.invalidate = false;
            update();

            PGN.PGNRecyclerViewAdapter adapter = new PGN.PGNRecyclerViewAdapter(activity, pgn, this);
            pgnRecyclerView.setAdapter(adapter);
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
                            Toast.makeText(activity, "Maximum time limit reached!", Toast.LENGTH_SHORT).show();
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
        public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
            return false;
        }

        @Override
        public void addToPGN(Piece piece, String move, int fromRow, int fromCol) {
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
            return boardModel;
        }

        @Override
        public HashMap<Piece, HashSet<Integer>> getLegalMoves() {
            return null;
        }

        private void movePrevious() {
            if (pointer > 0) {
                previousPosition = pointer;
                pointer--;
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

            boardModel = boardModels.get(pointer);
            analysisBoard.annotation = -1;
            if (moveAnnotationMap.containsKey(pointer - 1)) {
                Log.d(TAG, "update: Annotation: " + moveAnnotationMap.get(pointer - 1));
                analysisBoard.annotation = MoveAnnotation.getAnnotationResource(moveAnnotationMap.get(pointer - 1));
            }
            analysisBoard.invalidate();
            if (animate) analysisBoard.initializeAnimation();

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                else vibrator.vibrate(vibrationDuration);
            }

            if (sound && pointer != 0) mediaPlayer.start();

            binding.movePrevious.setAlpha(pointer == 0 ? 0.5f : 1f);
            binding.moveNext.setAlpha(pointer == length - 1 ? 0.5f : 1f);

            StringBuilder whiteText = new StringBuilder(), blackText = new StringBuilder();
            int blackCapturedValue = 0, whiteCapturedValue = 0, difference;

            binding.whiteCaptured.setText("");
            binding.blackCaptured.setText("");

            binding.whiteValue.setText("");
            binding.blackValue.setText("");

            ArrayList<Piece> capturedPieces = boardModel.getCapturedPieces();
            for (Piece piece : capturedPieces)
                if (piece.isWhite()) {
                    blackText.append(piece.getUnicode());
                    blackCapturedValue += piece.getRank().getValue();
                } else {
                    whiteText.append(piece.getUnicode());
                    whiteCapturedValue += piece.getRank().getValue();
                }
            difference = Math.abs(blackCapturedValue - whiteCapturedValue);

            if (difference != 0) {
                if (whiteCapturedValue > blackCapturedValue)
                    binding.whiteValue.setText(" +" + difference);
                else binding.blackValue.setText(" +" + difference);
            }

            binding.whiteCaptured.setText(whiteText);
            binding.blackCaptured.setText(blackText);

            if (commentsMap.containsKey(pointer - 1))
                Log.d(TAG, "update: Comment: " + commentsMap.get(pointer - 1));
            if (alternateMoveSequence.containsKey(pointer - 1))
                Log.d(TAG, "update: Alternate Move Sequence: " + alternateMoveSequence.get(pointer - 1));
        }
    }
}