package com.drdedd.simplichess.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Custom dialog to show circle progress with custom text
 */
public class ProgressBarDialog extends Dialog {
    private static final float hundred = 100f;
    private final String title;
    private final CircularProgressIndicator progressIndicator;
    private final TextView progressTextView;
    private final boolean indeterminate;

    public ProgressBarDialog(@NonNull Context context, String title, boolean indeterminate) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_progress);
        this.title = title;
        this.indeterminate = indeterminate;
        progressIndicator = findViewById(R.id.progress_bar);
        progressTextView = findViewById(R.id.progress_tv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.task_title)).setText(title);
        if (indeterminate) progressTextView.setVisibility(View.GONE);
        Window window = getWindow();
        if (window != null)
            window.setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @SuppressLint("SetTextI18n")
    public void updateProgress(int progress, int total) {
        int v = (int) (hundred * progress / total);
        progressIndicator.setProgress(v, false);
        progressTextView.post(() -> progressTextView.setText(v + "%"));
        if (v == 100) dismiss();
    }
}