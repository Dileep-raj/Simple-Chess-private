package com.drdedd.simplechess_temp.fragments;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplechess_temp.data.Regexes.FENPattern;
import static com.drdedd.simplechess_temp.data.Regexes.commentsRegex;
import static com.drdedd.simplechess_temp.data.Regexes.resultPattern;
import static com.drdedd.simplechess_temp.data.Regexes.singleMovePattern;
import static com.drdedd.simplechess_temp.data.Regexes.tagsPattern;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
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
import com.drdedd.simplechess_temp.databinding.FragmentLoadGameBinding;
import com.drdedd.simplechess_temp.dialogs.ProgressBarDialog;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;

public class LoadGameFragment extends Fragment implements Serializable {
    private static final String TAG = "LoadGameFragment";
    protected static final String LOAD_GAME_KEY = "LoadGame", LOAD_PGN_KEY = "LoadPGN", PGN_FILE_KEY = "PGNFile";
    public HashMap<String, String> tagsMap;
    public LinkedList<String> moves;
    //    HashMap<Integer, String> comments = new HashMap<>();
    private FragmentLoadGameBinding binding;
    boolean gameLoaded = false;
    private final static int gone = View.GONE, visible = View.VISIBLE;
    private ClipboardManager clipboardManager;
    private NavController navController;
    private PGN pgn;
    private ProgressBarDialog progressBarDialog;
    private Dialog dialog;
    private EditText pgnTxt;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadGameBinding.inflate(inflater, container, false);
        clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        navController = Navigation.findNavController(container);
        binding.notLoadedLayout.findViewById(R.id.btn_load_game).setOnClickListener(v -> inputFEN());
        tagsMap = new HashMap<>();
        moves = new LinkedList<>();
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(GameFragment.LOAD_GAME_KEY) && args.containsKey(LOAD_PGN_KEY)) {
                Stack<BoardModel> boardModelStack = (Stack<BoardModel>) args.getSerializable(LOAD_GAME_KEY);
                PGN pgn = (PGN) args.getSerializable(LOAD_PGN_KEY);
                if (boardModelStack != null) try {
                    this.pgn = pgn;
                    loadGame(new ArrayList<>(boardModelStack));
                    gameLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "onCreateView: Error while loading game", e);
                }
            } else if (args.containsKey(PGN_FILE_KEY)) {
                String pgn = args.getString(PGN_FILE_KEY);
                progressBarDialog = new ProgressBarDialog(requireContext(), "Loading PGN");
                progressBarDialog.show();
                gameLoaded = true;
                validatePGNSyntax(pgn);
                loadPGN();
            }
        }
//        comments = new HashMap<>();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (gameLoaded) {
            binding.loadedLayout.setVisibility(visible);
            binding.notLoadedLayout.setVisibility(gone);
            Log.d(TAG, "onViewCreated: Game Loaded");
        } else {
            binding.loadedLayout.setVisibility(gone);
            binding.notLoadedLayout.setVisibility(visible);
            binding.validateSamplePGNs.setOnClickListener(v -> validateSamplePGNs());
            try {
                inputFEN();
            } catch (Exception e) {
                Log.e(TAG, "onViewCreated: Error from dialog:\n", e);
                Toast.makeText(requireContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    void loadGame(ArrayList<BoardModel> boardModels) {
        GameViewer gameViewer = new GameViewer(requireContext(), binding, boardModels, pgn);

        binding.movePrevious.setOnClickListener(v -> gameViewer.movePrevious());
        binding.moveNext.setOnClickListener(v -> gameViewer.moveNext());
        binding.movePrevious.setOnLongClickListener(v -> gameViewer.moveToFirst());
        binding.moveNext.setOnLongClickListener(v -> gameViewer.moveToLast());

        binding.btnCopyPgn.setOnClickListener(v -> copyPGN());
    }

    private void copyPGN() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("PGN", pgn.toString()));
        Toast.makeText(requireContext(), "PGN copied", Toast.LENGTH_SHORT).show();
    }

    private boolean validatePGNSyntax(String pgn) {
        tagsMap = new HashMap<>();
        moves = new LinkedList<>();
//        comments = new HashMap<>();
        try {
            Matcher tags = tagsPattern.matcher(pgn);
            while (tags.find()) {
                String tag = tags.group();
                String tagName = tag.substring(1, tag.indexOf(' '));
                String tagValue = tag.substring(tag.indexOf('"') + 1, tag.lastIndexOf('"'));
                tagsMap.put(tagName, tagValue);
            }
            Log.i(TAG, "Total tags: " + tagsMap.size());

//            Log.v(TAG, "PGN before removing comments:\n************************Start************************\n" + pgn + "\n************************End************************\n");
            pgn = pgn.replaceAll(commentsRegex, "");
//            Log.v(TAG, "PGN after removing comments:\n************************Start************************\n" + pgn + "\n************************End************************\n");

            Matcher singleMoves = singleMovePattern.matcher(pgn);
            while (singleMoves.find()) {
                String singleMove = singleMoves.group();
                moves.add(singleMove);
            }
            Log.i(TAG, "Total moves count: " + moves.size());

            Matcher result = resultPattern.matcher(pgn);
            if (result.find()) Log.i(TAG, "\nResult: " + result.group().trim());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "validatePGN: Error while validating PGN\n", e);
            Toast.makeText(requireContext(), "Error while loading PGN", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void validateSamplePGNs() {
        AssetManager assets = requireContext().getAssets();
        ArrayList<String> failedPGNs = new ArrayList<>();
        try {
            String[] files = assets.list("PGNs");
            if (files != null) {
                long totalStart = System.nanoTime();
                for (String file : files) {
                    if (!file.endsWith(".pgn")) continue;

                    InputStream open = assets.open("PGNs/" + file);
                    BufferedInputStream bis = new BufferedInputStream(open);
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    for (int result = bis.read(); result != -1; result = bis.read())
                        buf.write((byte) result);
                    String PGN = buf.toString("UTF-8");

                    long start = System.nanoTime();
                    boolean result = validatePGNSyntax(PGN);
                    long end = System.nanoTime();
                    if (!result) failedPGNs.add(file);

                    HomeFragment.printTime(TAG, "validating PGN", end - start, PGN.length());
                }
                long totalEnd = System.nanoTime();
                HomeFragment.printTime(TAG, "validating all sample PGNs", totalEnd - totalStart, files.length);
                Log.d(TAG, "validateSamplePGNs: Failed PGNs: " + failedPGNs.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "validateSamplePGNs: Error while validating sample PGNs", e);
        }
    }

    private void inputFEN() {
        dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_load_game);
        dialog.setTitle("Load Game");
        pgnTxt = dialog.findViewById(R.id.load_pgn_txt);

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> {
            navController.popBackStack();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.load).setOnClickListener(v -> {

            if (TextUtils.isEmpty(pgnTxt.getText())) {
                pgnTxt.setError("Please enter a PGN");
                return;
            }
            String pgn = pgnTxt.getText().toString();

            String FEN = isFEN(pgn);
            if (!FEN.isEmpty()) {
                Bundle args = new Bundle();
                args.putString(GameFragment.FEN_KEY, FEN);
                args.putBoolean(GameFragment.NEW_GAME_KEY, false);
                Log.v(TAG, "inputFEN: Found FEN navigating to GameFragment");
                dialog.dismiss();
                navController.navigate(R.id.nav_game, args);
                return;
            }

            progressBarDialog = new ProgressBarDialog(requireContext(), "Loading PGN");
            progressBarDialog.show();

            long start = System.nanoTime();
            boolean result = validatePGNSyntax(pgnTxt.getText().toString());
            long end = System.nanoTime();

            HomeFragment.printTime(TAG, "validating PGN syntax", end - start, pgn.length());
            if (result) {
                Log.v(TAG, "inputFEN: No errors in PGN");
                dialog.dismiss();
                loadPGN();
            } else {
                Log.v(TAG, "inputFEN: Invalid PGN!");
                Toast.makeText(requireContext(), "Invalid PGN!", Toast.LENGTH_SHORT).show();
                pgnTxt.getText().clear();
            }
        });

        ImageButton paste = dialog.findViewById(R.id.paste_from_clipboard);
        boolean hasContent = false;
        if (clipboardManager.hasPrimaryClip())
            if (Objects.requireNonNull(clipboardManager.getPrimaryClipDescription()).hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                hasContent = true;
        if (!hasContent) paste.setVisibility(gone);
        paste.setOnClickListener(v -> {
            ClipData.Item item = Objects.requireNonNull(clipboardManager.getPrimaryClip()).getItemAt(0);
            ((EditText) dialog.findViewById(R.id.load_pgn_txt)).setText(item.getText());
        });
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        Objects.requireNonNull(dialog.getWindow()).setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void loadPGN() {
        boolean newGame = true;
        Bundle args = new Bundle();
        if (tagsMap.containsKey(PGN.SET_UP_TAG) && tagsMap.containsKey(PGN.FEN_TAG))
            if ("1".equals(tagsMap.get(PGN.SET_UP_TAG))) {
                String FEN = tagsMap.get(PGN.FEN_TAG);
                if (FEN != null && !FEN.isEmpty()) {
                    args.putString(GameFragment.LOAD_GAME_KEY, FEN);
                    newGame = false;
                }
            }
        args.putBoolean(GameFragment.NEW_GAME_KEY, newGame);
        args.putBoolean(GameFragment.LOAD_PGN_KEY, true);
        args.putSerializable(GameFragment.LOAD_GAME_FRAGMENT_KEY, this);
        if (navController != null) {
            if (navController.popBackStack())
                Log.d(TAG, "inputFEN: Fragment popped, navigating to GameFragment");
            navController.navigate(R.id.nav_game, args);
        }
    }

    private String isFEN(String PGN) {
        Matcher FENmatcher = FENPattern.matcher(PGN);
        String FEN = "";
        if (FENmatcher.find()) FEN = FENmatcher.group();
        return FEN;
    }

    public static String getTAG() {
        return TAG;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        private final boolean vibrationEnabled;
        private final int length;
        private final Vibrator vibrator;
        private final FragmentLoadGameBinding binding;
        private final RecyclerView pgnRecyclerView;
        private BoardModel boardModel;
        private int pointer, previousPosition;

        /**
         * @param context     Context
         * @param binding     LoadGame fragment binding
         * @param boardModels List of board models
         * @param pgn         PGN of the game
         */
        public GameViewer(Context context, FragmentLoadGameBinding binding, ArrayList<BoardModel> boardModels, PGN pgn) {
            DataManager dataManager = new DataManager(context);
            this.boardModels = boardModels;
            this.binding = binding;
            boardModel = boardModels.get(0);
            analysisBoard = binding.analysisBoard;
            pgnRecyclerView = binding.pgnRecyclerView;

            String whiteName = pgn.getWhite();
            String blackName = pgn.getBlack();

            Resources resources = context.getResources();
            String draw = resources.getString(R.string.draw);
            String victory = resources.getString(R.string.peace);

            switch (pgn.getAppendResult()) {
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

            vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            vibrationEnabled = dataManager.getVibration();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                analysisBoard.boardInterface = this;
                analysisBoard.setTheme(dataManager.getBoardTheme());
                analysisBoard.invalidate = false;
            }

            Player.WHITE.setInCheck(false);
            Player.BLACK.setInCheck(false);
            update();

            PGN.PGNRecyclerViewAdapter adapter = new PGN.PGNRecyclerViewAdapter(context, pgn, this);
            pgnRecyclerView.setAdapter(adapter);
            pgnRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
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
            }
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
            analysisBoard.invalidate();

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

            binding.movePrevious.setAlpha(pointer == 0 ? 0.5f : 1f);
            binding.moveNext.setAlpha(pointer == length - 1 ? 0.5f : 1f);

            StringBuilder whiteText = new StringBuilder(), blackText = new StringBuilder();
            int blackCapturedValue = 0, whiteCapturedValue = 0, difference;

            binding.whiteCaptured.setText("");
            binding.blackCaptured.setText("");

            binding.whiteValue.setText("");
            binding.blackValue.setText("");

            ArrayList<Piece> capturedPieces = boardModel.getCapturedPieces();
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
                if (whiteCapturedValue > blackCapturedValue)
                    binding.whiteValue.setText(" +" + difference);
                else binding.blackValue.setText(" +" + difference);
            }

            binding.whiteCaptured.setText(whiteText);
            binding.blackCaptured.setText(blackText);
        }
    }
}