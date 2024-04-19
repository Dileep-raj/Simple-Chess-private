package com.drdedd.simplechess_temp.dialogs;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

public class ProgressBarDialog extends Dialog {
    public ProgressBarDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);

    }
}
