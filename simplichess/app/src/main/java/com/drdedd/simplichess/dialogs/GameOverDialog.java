package com.drdedd.simplichess.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.game.PGN;
import com.drdedd.simplichess.R;

import java.util.Objects;

/**
 * Custom dialog to show after game is over
 */
public class GameOverDialog extends Dialog {
    private final PGN pgn;
    private final String termination;
    private final ClipboardManager clipboard;

    public GameOverDialog(@NonNull Context context, PGN pgn) {
        super(context);
        setCancelable(false);
        this.termination = pgn.getTermination();
        this.pgn = pgn;
        clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_game_over);
        findViewById(R.id.close_dialog).setOnClickListener(v -> dismiss());

        TextView terminationMessage = findViewById(R.id.termination_message), pgnTextView = findViewById(R.id.copy_pgn_textView);

        terminationMessage.setText(termination);
        pgnTextView.setText(pgn.toString());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(v -> copyPGN());

        Objects.requireNonNull(getWindow()).setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void copyPGN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("PGN", pgn.toString()));
        Toast.makeText(getContext(), "PGN copied", Toast.LENGTH_SHORT).show();
    }
}