package com.drdedd.simplechess_temp.fragments;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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

import com.drdedd.simplechess_temp.BoardModel;
import com.drdedd.simplechess_temp.GameData.Rank;
import com.drdedd.simplechess_temp.R;
import com.drdedd.simplechess_temp.databinding.FragmentLoadGameBinding;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadGameFragment extends Fragment {
    private static final String TAG = "LoadGameFragment";
    private static final String tagsRegex = "\\[(\\w*) \"([-\\w.,/* ]*)\"]";
    private static final String commentsRegex = "(\\{[\\w!@#$%^&*();:,.<>\"'|/?\\-=+\\[\\]\\s]*\\}(\\s?[0-9]*\\.{3})?)|(;\\s[\\w!@#$%^&*();:,.<>\"'|/?\\-=+\\[\\]\\s]*\\s[0-9]*\\.{3})";
    private static final String singleMoveRegex = "[KQRNBP]?[a-h1-8]?x?[a-h][1-8][+#]?(=[QRNB])?|O-O|O-O-O";
    private static final String resultRegex = "(1/2-1/2|\\*|0-1|1-0)\\s*$";
    private static final String FENRegex = "([kqbnrp1-8]+/){7}[kqbnrp1-8]+ [wb] (-|[kq]+) (-|[a-h][1-8])( (-|[0-9]+) (-|[0-9]*))?";
    private static final Pattern FENPattern = Pattern.compile(FENRegex, Pattern.CASE_INSENSITIVE);

    private HashMap<String, String> tagsMap;
    private LinkedList<String> moves;
//    HashMap<Integer, String> comments = new HashMap<>();

    private FragmentLoadGameBinding binding;
    boolean gameLoaded = false;
    private final static int gone = View.GONE, visible = View.VISIBLE;
    private ClipboardManager clipboardManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadGameBinding.inflate(inflater, container, false);
        clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        binding.notLoadedLayout.findViewById(R.id.btn_load_game).setOnClickListener(v -> showDialog());
        tagsMap = new HashMap<>();
        moves = new LinkedList<>();
//        comments = new HashMap<>();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (gameLoaded) {
            binding.loadedLayout.setVisibility(visible);
            binding.notLoadedLayout.setVisibility(gone);
        } else {
            binding.loadedLayout.setVisibility(gone);
            binding.notLoadedLayout.setVisibility(visible);
        }
        try {
            showDialog();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: Error from dialog:\n", e);
            Toast.makeText(requireContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }
    }

    private boolean validatePGN(String pgn) {
        tagsMap = new HashMap<>();
        moves = new LinkedList<>();
//        comments = new HashMap<>();
        try {
            Matcher tags = Pattern.compile(tagsRegex).matcher(pgn);
            while (tags.find()) {
                String tag = tags.group();
                String tagName = tag.substring(1, tag.indexOf(' '));
                String tagValue = tag.substring(tag.indexOf('"') + 1, tag.lastIndexOf('"'));
                tagsMap.put(tagName, tagValue);
            }
            Log.i(TAG, "Total tags: " + tagsMap.size());

            Log.v(TAG, "PGN before removing comments:\n************************Start************************\n" + pgn + "\n************************End************************\n");
            pgn = pgn.replaceAll(commentsRegex, "");
            Log.v(TAG, "PGN after removing comments:\n************************Start************************\n" + pgn + "\n************************End************************\n");

            Matcher singleMoves = Pattern.compile(singleMoveRegex).matcher(pgn);
            while (singleMoves.find()) {
                String singleMove = singleMoves.group();
                moves.add(singleMove);
            }
            Log.i(TAG, "Total moves count: " + moves.size());

            Matcher result = Pattern.compile(resultRegex).matcher(pgn);
            if (result.find()) Log.i(TAG, "\nResult: " + result.group().trim());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "validatePGN: Error while validating PGN\n", e);
            Toast.makeText(requireContext(), "Error while loading PGN", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

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
            if (TextUtils.isEmpty(pgnTxt.getText())) pgnTxt.setError("Please enter a PGN");
            long start = System.nanoTime();
            boolean result = validatePGN(pgnTxt.getText().toString());
            long end = System.nanoTime();
            GameFragment.printTime(TAG, "validating PGN", end - start);
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

    public static String getTAG() {
        return TAG;
    }
}