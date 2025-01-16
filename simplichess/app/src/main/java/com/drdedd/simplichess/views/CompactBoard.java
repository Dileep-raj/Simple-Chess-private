package com.drdedd.simplichess.views;

import static com.drdedd.simplichess.misc.MiscMethods.dpToPixel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.databinding.LayoutCompactBoardBinding;
import com.drdedd.simplichess.interfaces.GameLogicInterface;

import java.util.HashSet;
import java.util.Set;

public class CompactBoard extends LinearLayout {
    private final HashSet<String> chessTitles = new HashSet<>();
    private final LayoutCompactBoardBinding binding;
    private ChessBoard board;
    private TextView topTitle, bottomTitle, topName, bottomName, topTime, bottomTime, openingNameTV;
    private String whiteName, blackName, whiteTitle, blackTitle, whiteValue, blackValue;
    private boolean boardShrunk;
    private int boardMargin;

    public CompactBoard(Context context) {
        super(context);
        binding = LayoutCompactBoardBinding.inflate(LayoutInflater.from(getContext()), this, true);
        initialize(null);
    }

    public CompactBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = LayoutCompactBoardBinding.inflate(LayoutInflater.from(getContext()), this, true);
        initialize(context.obtainStyledAttributes(attrs, R.styleable.CompactBoard));
    }

    public CompactBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = LayoutCompactBoardBinding.inflate(LayoutInflater.from(getContext()), this, true);
        initialize(context.obtainStyledAttributes(attrs, R.styleable.CompactBoard));
    }

    private void initialize(TypedArray attrs) {
        boardShrunk = false;

        boardMargin = (int) dpToPixel(getResources().getDisplayMetrics(), 4);

        board = binding.chessBoard;

        topTitle = binding.topTitle;
        topName = binding.topName;
        topTime = binding.topTime;

        bottomTitle = binding.bottomTitle;
        bottomName = binding.bottomName;
        bottomTime = binding.bottomTime;

        openingNameTV = binding.openingName;

        if (attrs != null) {
            int light = attrs.getColor(R.styleable.CompactBoard_lightColor, ContextCompat.getColor(getContext(), R.color.theme_default_light));
            int dark = attrs.getColor(R.styleable.CompactBoard_darkColor, ContextCompat.getColor(getContext(), R.color.theme_default_dark));
            board.setColors(light, dark);
            attrs.recycle();
        }
        chessTitles.addAll(Set.of(getResources().getStringArray(R.array.chess_titles)));

        blackTitle = whiteTitle = "";
        whiteName = "White player";
        blackName = "Black player";
        whiteValue = blackValue = "";

        topTime.setText("");
        bottomTime.setText("");
        reloadData();
    }

    public void setPlayersData(String whiteTitle, String whiteName, String blackTitle, String blackName) {
        this.whiteTitle = validateTitle(whiteTitle);
        this.blackTitle = validateTitle(blackTitle);
        this.whiteName = whiteName;
        this.blackName = blackName;
        reloadData();
    }

    public void setBoardData(GameLogicInterface gameLogicInterface, boolean viewOnly) {
        board.setData(gameLogicInterface, viewOnly);
    }

    public void setToggleSizeButton(ImageButton toggleSizeButton, @DrawableRes int shrinkResId, @DrawableRes int expandResId) {
        if (toggleSizeButton == null) return;
        toggleSizeButton.setOnClickListener(v -> {
            toggleBoardSize();
            toggleSizeButton.setImageResource(boardShrunk ? expandResId : shrinkResId);
        });
    }

    @SuppressLint("SetTextI18n")
    public void reloadData() {
        if (board.isFlipped()) {
            topName.setText(whiteName + " " + whiteValue);
            bottomName.setText(blackName + " " + blackValue);

            topTitle.setText(whiteTitle);
            bottomTitle.setText(blackTitle);

            topTitle.setVisibility(whiteTitle.isEmpty() ? GONE : VISIBLE);
            bottomTitle.setVisibility(blackTitle.isEmpty() ? GONE : VISIBLE);
        } else {
            topName.setText(blackName + " " + blackValue);
            bottomName.setText(whiteName + " " + whiteValue);

            topTitle.setText(blackTitle);
            bottomTitle.setText(whiteTitle);

            topTitle.setVisibility(blackTitle.isEmpty() ? GONE : VISIBLE);
            bottomTitle.setVisibility(whiteTitle.isEmpty() ? GONE : VISIBLE);
        }
    }

    public void flipBoard() {
        board.flipBoard();
        reloadData();
    }

    public void toggleBoardSize() {
        LayoutParams layoutParams = (LayoutParams) board.getLayoutParams();
        if (boardShrunk) {
            layoutParams.setMarginStart(0);
            layoutParams.setMarginEnd(0);
        } else {
            layoutParams.setMarginStart(boardMargin);
            layoutParams.setMarginEnd(boardMargin);
        }
        board.setLayoutParams(layoutParams);
        boardShrunk = !boardShrunk;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public TextView getWhiteTimeTV() {
        return board.isFlipped() ? topTime : bottomTime;
    }

    public TextView getBlackTimeTV() {
        return board.isFlipped() ? bottomTime : topTime;
    }

    public void setWhiteName(String whiteName) {
        this.whiteName = whiteName;
    }

    public void setBlackName(String blackName) {
        this.blackName = blackName;
    }

    public void setWhiteTitle(String whiteTitle) {
        this.whiteTitle = validateTitle(whiteTitle);
    }

    public void setBlackTitle(String blackTitle) {
        this.blackTitle = validateTitle(blackTitle);
    }

    public void setWhiteValue(String whiteValue) {
        this.whiteValue = whiteValue;
    }

    public void setBlackValue(String blackValue) {
        this.blackValue = blackValue;
    }

    public void setOpening(String eco, String openingName) {
        if (eco == null || eco.isEmpty()) {
            openingNameTV.setText("");
            openingNameTV.setVisibility(GONE);
            return;
        }
        openingNameTV.setText(String.format("%s: %s", eco, openingName));
        openingNameTV.setVisibility(VISIBLE);
    }

    private String validateTitle(String title) {
        return chessTitles.contains(title) ? title : "";
    }

    public boolean isBoardShrunk() {
        return boardShrunk;
    }

    public void setAnnotation(String annotationSquare, @DrawableRes int annotation) {
        board.setAnnotationSquare(annotationSquare);
        board.setAnnotation(annotation);
    }

    public void setAnnotation(@DrawableRes int annotation) {
        board.setAnnotation(annotation);
    }

    /**
     * Initializes animation variables
     */
    public void initializeAnimation(String fromSquare, String toSquare) {
        board.initializeAnimation(fromSquare, toSquare);
    }

    /**
     * Initializes reverse animation for a move
     *
     * @param fromSquare Starting position of the piece
     * @param toSquare   Ending position of the piece
     */
    public void initializeReverseAnimation(String fromSquare, String toSquare) {
        board.initializeReverseAnimation(fromSquare, toSquare);
    }

    public void clearSelection() {
        board.clearSelection();
        board.invalidate();
    }

    public void setViewOnly(boolean viewOnly) {
        board.setViewOnly(viewOnly);
    }

    public void setPosition(String position) {
        board.setPosition(position);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        board.invalidate();
    }
}