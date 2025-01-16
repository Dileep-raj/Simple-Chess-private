package com.drdedd.simplichess.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.game.gameData.Rank;

/**
 * Custom dialog for pawn promotion
 */
public class PromoteDialog extends Dialog implements View.OnClickListener {
    private Rank rank = null;

    /**
     * Dialog for pawn promotion
     *
     * @param context Context of the activity/fragment
     */
    public PromoteDialog(@NonNull Context context) {
        super(context);
        this.setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_promotion);
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
                break;
            case R.id.promote_to_bishop:
                rank = Rank.BISHOP;
                break;
            case R.id.promote_to_knight:
                rank = Rank.KNIGHT;
                break;
            case R.id.promote_to_rook:
                rank = Rank.ROOK;
                break;
        }
        if (rank != null) dismiss();
    }

    /**
     * Rank selected for promotion
     *
     * @return <code>Queen|Rook|Knight|Bishop</code>
     */
    public Rank getRank() {
        return rank;
    }
}