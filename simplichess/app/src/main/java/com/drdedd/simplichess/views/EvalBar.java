package com.drdedd.simplichess.views;

import static com.drdedd.simplichess.misc.MiscMethods.dpToPixel;
import static com.drdedd.simplichess.misc.MiscMethods.spToPixel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.game.pgn.PGN;

public class EvalBar extends View {
    private static final String TAG = "EvalBar";
    private static final int maxEval = 7, speed = 4;
    private static final float totalDivisions = maxEval * 2f;
    private String evaluation;
    private Paint p;
    private float dp1, dp2, dp5, dp28, eval, tempPosition;
    private int evalColor;

    public EvalBar(@NonNull Context context) {
        super(context);
        initializeView(null);
    }

    public EvalBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeView(context.obtainStyledAttributes(attrs, R.styleable.EvalBar));
    }

    public EvalBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView(context.obtainStyledAttributes(attrs, R.styleable.EvalBar));
    }

    private void initializeView(TypedArray attrs) {
        String eval = null;
        if (attrs != null) {
            eval = attrs.getString(R.styleable.EvalBar_eval);
            if (eval == null && attrs.hasValue(R.styleable.EvalBar_eval))
                eval = String.valueOf(attrs.getFloat(R.styleable.EvalBar_eval, 0.0f));
        }

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        evalColor = ContextCompat.getColor(getContext(), R.color.alice_dark);
        Typeface evalTypeface = getResources().getFont(R.font.noto_sans_bold);

        p = new Paint();
        p.setTextSize(spToPixel(displayMetrics, 14));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(evalTypeface);
        dp1 = dpToPixel(displayMetrics, 1);
        dp2 = dpToPixel(displayMetrics, 2);
        dp5 = dpToPixel(displayMetrics, 5);
        dp28 = dpToPixel(displayMetrics, 28);

        if (eval == null) resetEval();
        else setEvaluation(eval);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, (int) dp28);
    }

    public void resetEval() {
        setEvaluation("0.0");
        eval = 0;
        tempPosition = -1;
//        invalidate();
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
        updateEval();
        invalidate();
    }

    private void updateEval() {
        if (evaluation == null || evaluation.isEmpty()) evaluation = "0.0";

        if (evaluation.equals(PGN.RESULT_DRAW)) eval = 0;
        else if (evaluation.equals(PGN.RESULT_WHITE_WON)) eval = maxEval;
        else if (evaluation.equals(PGN.RESULT_BLACK_WON)) eval = -maxEval;
        else if (evaluation.contains("M")) {
            if (evaluation.contains("-")) eval = -maxEval;
            else eval = maxEval;
        } else {
            try {
                eval = Float.parseFloat(evaluation);
                if (eval < -maxEval) eval = -maxEval;
                else if (eval > maxEval) eval = maxEval;
            } catch (Exception e) {
                Log.e(TAG, "updateEval: Error while updating eval bar", e);
                eval = 0;
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float top = h - dp5, div = w / totalDivisions;
        float finalPosition = div * (maxEval + eval);

        if (tempPosition == -1) tempPosition = finalPosition;

        float increment = Math.round((finalPosition - tempPosition) / speed);
        tempPosition += increment;

        if (Math.abs(tempPosition - finalPosition) <= 1f || (increment == 0 && finalPosition != tempPosition)) {
            tempPosition = finalPosition;
//            Log.d(TAG, "onDraw: Forced assignment");
        }

        p.setColor(ContextCompat.getColor(getContext(), R.color.eval_white));
        canvas.drawRect(0, 0, tempPosition, h, p);

        p.setColor(ContextCompat.getColor(getContext(), R.color.eval_black));
        canvas.drawRect(tempPosition, 0, w, h, p);

        p.setColor(evalColor);
        for (int i = 1; i < totalDivisions; i++) {
            if (i == maxEval) canvas.drawRect(div * i - dp2 / 2, top - 5, div * i + dp2 / 2, h, p);
            else canvas.drawRect(div * i - dp1 / 2, top, div * i + dp1 / 2, h, p);
        }

        canvas.drawText(evaluation.equals(PGN.RESULT_DRAW) ? "Draw" : evaluation, w / 2f, h / 2f - (p.descent() + p.ascent()) / 2, p);
        if (finalPosition != tempPosition) invalidate();
//        else Log.d(TAG, "onDraw: animation ended");
    }
}