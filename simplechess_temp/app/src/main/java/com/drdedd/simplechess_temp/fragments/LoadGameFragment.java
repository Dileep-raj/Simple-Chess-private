package com.drdedd.simplechess_temp.fragments;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.drdedd.simplechess_temp.data.Regexes.FENPattern;
import static com.drdedd.simplechess_temp.data.Regexes.commentsRegex;
import static com.drdedd.simplechess_temp.data.Regexes.resultsPattern;
import static com.drdedd.simplechess_temp.data.Regexes.singleMovePattern;
import static com.drdedd.simplechess_temp.data.Regexes.tagsPattern;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
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
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.interfaces.PGNRecyclerViewInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Pawn;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;

public class LoadGameFragment extends Fragment {
    private static final String TAG = "LoadGameFragment";
    private HashMap<String, String> tagsMap;
    private LinkedList<String> moves;
//    HashMap<Integer, String> comments = new HashMap<>();

    private FragmentLoadGameBinding binding;
    boolean gameLoaded = false;
    private final static int gone = View.GONE, visible = View.VISIBLE;
    private ClipboardManager clipboardManager;
    private NavController navController;
    private PGN pgn;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadGameBinding.inflate(inflater, container, false);
        clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        binding.notLoadedLayout.findViewById(R.id.btn_load_game).setOnClickListener(v -> showDialog());
        tagsMap = new HashMap<>();
        moves = new LinkedList<>();
        Bundle args = getArguments();
        if (args != null)
            if (args.containsKey(GameFragment.LOAD_GAME_KEY) && args.containsKey(GameFragment.LOAD_PGN_KEY)) {
                Stack<BoardModel> boardModelStack = (Stack<BoardModel>) args.getSerializable(GameFragment.LOAD_GAME_KEY);
                PGN pgn = (PGN) args.getSerializable(GameFragment.LOAD_PGN_KEY);
                if (boardModelStack != null) try {
                    this.pgn = pgn;
                    loadGame(new ArrayList<>(boardModelStack));
                    gameLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "onCreateView: Error while loading game", e);
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
            Log.d(TAG, "onViewCreated: Game Loaded and visible");
        } else {
            binding.loadedLayout.setVisibility(gone);
            binding.notLoadedLayout.setVisibility(visible);
            binding.validateSamplePGNs.setOnClickListener(v -> validateSamplePGNs());
            try {
                showDialog();
            } catch (Exception e) {
                Log.e(TAG, "onViewCreated: Error from dialog:\n", e);
                Toast.makeText(requireContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        navController = Navigation.findNavController(requireActivity(), R.id.main_fragment);
    }

    void loadGame(ArrayList<BoardModel> boardModels) {
        GameViewer gameViewer = new GameViewer(requireContext(), boardModels, binding);

        binding.movePrevious.setOnClickListener(v -> gameViewer.movePrevious());
        binding.moveNext.setOnClickListener(v -> gameViewer.moveNext());
        binding.movePrevious.setOnLongClickListener(v -> gameViewer.moveToFirst());
        binding.moveNext.setOnLongClickListener(v -> gameViewer.moveToLast());

        binding.btnCopyPgn.setOnClickListener(v -> copyPGN());
        RecyclerView pgnRecyclerView = binding.pgnRecyclerView;
        PGN.PGNRecyclerViewAdapter adapter = new PGN.PGNRecyclerViewAdapter(requireContext(), pgn, gameViewer);
        pgnRecyclerView.setAdapter(adapter);
        pgnRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
//        binding.analysisPgnTv.setText(pgn.getPGN());
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

            Matcher result = resultsPattern.matcher(pgn);
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

    /*
        private void loadPGN() {
            BoardModel boardModel = new BoardModel(requireContext(), true);
            int i;
            Rank rank;
            for (String move : moves) {
                for (i = 0; i < move.length(); i++) {
                    char ch = move.charAt(i);
                    switch (ch) {
                        case 'K':
                            rank = Rank.KING;
                            break;
                        case 'Q':
                            rank = Rank.QUEEN;
                            break;
                        case 'R':
                            rank = Rank.ROOK;
                            break;
                        case 'N':
                            rank = Rank.KNIGHT;
                            break;
                        case 'B':
                            rank = Rank.BISHOP;
                            break;
                        default:
                            rank = Rank.PAWN;
                    }
                }
            }
        }
    */
    private void showDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_load_game);
        dialog.setTitle("Load Game");

        dialog.findViewById(R.id.cancel).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.load).setOnClickListener(v -> {
            EditText pgnTxt = dialog.findViewById(R.id.load_pgn_txt);
            if (TextUtils.isEmpty(pgnTxt.getText())) {
                pgnTxt.setError("Please enter a PGN");
                return;
            }
            String PGN = pgnTxt.getText().toString();

            String FEN = isFEN(PGN);
            if (!FEN.isEmpty()) {
                Bundle args = new Bundle();
                args.putString(GameFragment.FEN_KEY, FEN);
                args.putBoolean(GameFragment.NEW_GAME_KEY, false);
                Log.v(TAG, "showDialog: Found FEN navigating to GameFragment");
                dialog.dismiss();
                navController.navigate(R.id.nav_game, args);
                return;
            }
            long start = System.nanoTime();
            boolean result = validatePGNSyntax(PGN);
            long end = System.nanoTime();

            HomeFragment.printTime(TAG, "validating PGN syntax", end - start, PGN.length());
            if (result) {
                Log.v(TAG, "onCreateView: No errors in PGN");
                Toast.makeText(requireContext(), "No errors in PGN", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Log.v(TAG, "onCreateView: Invalid PGN!");
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

    private String isFEN(String PGN) {
        Matcher FENmatcher = FENPattern.matcher(PGN);
        String FEN = "";
        if (FENmatcher.find()) FEN = FENmatcher.group();
        return FEN;
    }

    public static String getTAG() {
        return TAG;
    }

    static class GameViewer implements BoardInterface, PGNRecyclerViewInterface {
        private static final String TAG = "GameViewer";
        private final ArrayList<BoardModel> boardModels;
        private final ChessBoard analysisBoard;
        private final boolean vibrationEnabled;
        private final int length;
        private final Vibrator vibrator;
        private final FragmentLoadGameBinding binding;
        private BoardModel boardModel;
        private int pointer;

        public GameViewer(Context context, ArrayList<BoardModel> boardModels, FragmentLoadGameBinding binding) {
            DataManager dataManager = new DataManager(context);
            this.boardModels = boardModels;
            this.binding = binding;
            boardModel = boardModels.get(0);
            analysisBoard = binding.analysisBoard;

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
            binding.movePrevious.setAlpha(0.5f);
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
                pointer--;
                update();
            }
        }

        private void moveNext() {
            if (pointer < length - 1) {
                pointer++;
                update();
            }
        }

        private boolean moveToFirst() {
            pointer = 0;
            update();
            return true;
        }

        private boolean moveToLast() {
            pointer = length - 1;
            update();
            return true;
        }

        @Override
        public void jumpToMove(int position) {
            if (position < length - 1) {
                pointer = position + 1;
                update();
            }
        }

        /**
         * Updates Board, check status and other views
         */
        private void update() {
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
        }
    }
}