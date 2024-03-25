package com.drdedd.simplechess_temp.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;

//@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class GameOverDialog extends Dialog {
    private final PGN pgn;
    private final Context context;
    //    private final String[] permissions;
    //    private final Activity activity;
    private final String termination;
    private final ClipboardManager clipboard;

    public GameOverDialog(@NonNull Context context, PGN pgn) {
        super(context);
        setCancelable(false);
        this.context = context;
//        this.activity = activity;
        this.termination = pgn.getTermination();
        this.pgn = pgn;
        clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
//            permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        else permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.game_over_dialog);
        findViewById(R.id.close_dialog).setOnClickListener(v -> dismiss());

        TextView terminationMessage, pgnTextView;
        terminationMessage = findViewById(R.id.termination_message);
        pgnTextView = findViewById(R.id.copy_pgn_textView);

        terminationMessage.setText(termination);
        pgnTextView.setText(pgn.toString());
        findViewById(R.id.btn_copy_pgn).setOnClickListener(v -> copyPGN());
        findViewById(R.id.btn_export_pgn).setOnClickListener(v -> exportPGN());
    }

    private void copyPGN() {
        clipboard.setPrimaryClip(ClipData.newPlainText("PGN", pgn.toString()));
    }

    private void exportPGN() {
//        if (context.checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
//            try {
//                String dir = pgn.exportPGN();
//                Toast.makeText(context, "PGN saved in " + dir, Toast.LENGTH_LONG).show();
//            } catch (IOException e) {
//                Toast.makeText(context, "File not saved!", Toast.LENGTH_SHORT).show();
//                String TAG = "GameOverDialog";
//                Log.d(TAG, "exportPGN: \n" + e);
//            }
//        } else {
//            Toast.makeText(context, "Write permission is required to export PGN", Toast.LENGTH_SHORT).show();
////            ActivityCompat.requestPermissions(activity, permissions, 0);
//        }
    }
}
