package com.drdedd.simplechess_temp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drdedd.simplechess_temp.PGN;
import com.drdedd.simplechess_temp.R;

public class EvalBar extends View {
    private static final String TAG = "EvalBar";
    private static final int maxEval = 7;
    private static final float totalDivisions = maxEval * 2f;
    private View whiteAdvantage, blackAdvantage;
    private TextView evalView;
    private String evaluation;
    private Paint p;
    private double whiteEval, blackEval;
    private float dp1, dp2, dp5, dp28;
    private int evalColor;
    private final int speed = 4;
    private float eval, tempPosition, increment;
    boolean view = true;

    public EvalBar(@NonNull Context context) {
        super(context);
        initializeView();
    }

    public EvalBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    private void initializeView() {
        if (view) {
            evalColor = getResources().getColor(R.color.alice_dark);
            Typeface evalTypeface = getResources().getFont(R.font.noto_sans_bold);

            p = new Paint();
            p.setTextSize(spToPixel(14));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(evalTypeface);
        }
//        else {
//            inflate(getContext(), R.layout.eval_bar, this);
//            whiteAdvantage = findViewById(R.id.whiteAdvantage);
//            blackAdvantage = findViewById(R.id.blackAdvantage);
//            evalView = findViewById(R.id.evalView);
//        }
        dp1 = dpToPixel(1);
        dp2 = dpToPixel(2);
        dp5 = dpToPixel(5);
        dp28 = dpToPixel(28);
        resetEval();
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

        if (view) {
//            tempPosition = eval;
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
            Log.d(TAG, "updateEval: Animation started");
            return;
        }

//        evalView.setText(evaluation.equals(PGN.RESULT_DRAW) ? "Draw" : evaluation);
//        if (evaluation.equals(PGN.RESULT_DRAW)) whiteEval = blackEval = maxEval;
//        else if (evaluation.equals(PGN.RESULT_WHITE_WON)) blackEval = 1 - (whiteEval = 1);
//        else if (evaluation.equals(PGN.RESULT_BLACK_WON)) blackEval = 1 - (whiteEval = 0);
//        else if (evaluation.contains("M"))
//            blackEval = 1 - (whiteEval = evaluation.contains("-") ? 0 : 1);
//        else try {
//                double eval = Double.parseDouble(evaluation);
//                whiteEval = maxEval + eval;
//                blackEval = maxEval - eval;
//            } catch (Exception e) {
//                Log.e(TAG, "updateEval: Error while updating eval bar", e);
//                whiteEval = blackEval = maxEval;
//                updateEvalBar();
//            }
//        updateEvalBar();
    }

//    private void updateEvalBar() {
//        LinearLayout.LayoutParams whiteParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) whiteEval);
//        whiteAdvantage.setLayoutParams(whiteParams);
//
//        LinearLayout.LayoutParams blackParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) blackEval);
//        blackAdvantage.setLayoutParams(blackParams);
//    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float top = h - dp5, div = w / totalDivisions;
        float finalPosition = div * (maxEval + eval);

        if (tempPosition == -1) tempPosition = finalPosition;

//        Log.d(TAG, "onDraw: tempPosition: " + tempPosition);

        increment = Math.round((finalPosition - tempPosition) / speed);
        tempPosition += increment;

        if (Math.abs(tempPosition - finalPosition) <= 1f || (increment == 0 && finalPosition != tempPosition)) {
            tempPosition = finalPosition;
//            Log.d(TAG, "onDraw: Forced assignment");
        }

        p.setColor(getResources().getColor(R.color.eval_white));
        canvas.drawRect(0, 0, tempPosition, h, p);

        p.setColor(getResources().getColor(R.color.eval_black));
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

    private float dpToPixel(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPixel(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}