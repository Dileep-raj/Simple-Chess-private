package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.GameData.Rank;

public class PromoteDialog extends Dialog implements View.OnClickListener {
    private Rank rank;

    public PromoteDialog(@NonNull Context context) {
        super(context);
        this.setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.promotion_dialog);
        findViewById(R.id.promote_to_queen).setOnClickListener(this);
        findViewById(R.id.promote_to_rook).setOnClickListener(this);
        findViewById(R.id.promote_to_knight).setOnClickListener(this);
        findViewById(R.id.promote_to_bishop).setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.promote_to_queen:
                rank = Rank.QUEEN;
                dismiss();
                break;
            case R.id.promote_to_bishop:
                rank = Rank.BISHOP;
                dismiss();
                break;
            case R.id.promote_to_knight:
                rank = Rank.KNIGHT;
                dismiss();
                break;
            case R.id.promote_to_rook:
                rank = Rank.ROOK;
                dismiss();
                break;
            default:
                break;
        }
    }

    public Rank getRank() {
        return rank;
    }
}
