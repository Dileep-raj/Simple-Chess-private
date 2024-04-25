package com.drdedd.simplechess_temp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.R;

import java.util.Objects;

public class ProgressBarDialog extends Dialog {
    private final String title;

    public ProgressBarDialog(@NonNull Context context, String title) {
        super(context);
        this.title = title;
        setCancelable(false);
        setContentView(R.layout.dialog_progress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.progress_title)).setText(title);
        Objects.requireNonNull(getWindow()).setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95), ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}