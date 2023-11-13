package com.drdedd.simplechess_temp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.ChessState;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom View to create a Chess board and design game interface
 */
public class ChessBoard extends View {

    public BoardInterface boardInterface;
    private static final String TAG = "ChessBoard";
    private float offsetX = 10f, offsetY = 10f, sideLength = 130f;
    private int lightColor, darkColor, fromCol = -1, fromRow = -1, floatingPieceX = -1, floatingPieceY = -1;
    private final Set<Integer> resIDs = Set.of(R.drawable.kb, R.drawable.qb, R.drawable.rb, R.drawable.bb, R.drawable.nb, R.drawable.pb, R.drawable.kw, R.drawable.qw, R.drawable.rw, R.drawable.bw, R.drawable.nw, R.drawable.pw, R.drawable.guide_blue);
    private final HashMap<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint p = new Paint();
    private Piece previousSelectedPiece = null;
    private Set<Integer> legalMoves = new HashSet<>();

    public ChessBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int min = Math.min(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(min, min);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        loadBitmaps();
        int width = getWidth(), height = getHeight();
        float scaleFactor = 0.95f;
        float boardSize = Math.min(width, height) * scaleFactor;
        sideLength = boardSize / 8f;
        offsetX = (width - boardSize) / 2;
        offsetY = (height - boardSize) / 2;
        drawBoard(canvas);
        drawPieces(canvas);
        drawCoordinates(canvas);
    }

    private void loadBitmaps() {
        for (Integer id : resIDs) {
            Bitmap b = BitmapFactory.decodeResource(getResources(), id);
            bitmaps.put(id, b);
        }
    }

    public void setTheme(BoardTheme theme) {
        lightColor = theme.getLightColor();
        darkColor = theme.getDarkColor();
    }

    private void drawBoard(Canvas canvas) {
        int i, j;
        for (i = 0; i < 8; i++)
            for (j = 0; j < 8; j++) {
                p.setColor(((i + j) % 2 == 0) ? lightColor : darkColor);
                canvas.drawRect(offsetX + j * sideLength, offsetY + i * sideLength, offsetX + (j + 1) * sideLength, offsetY + (i + 1) * sideLength, p);
            }
    }

    private void drawCoordinates(Canvas canvas) {
        int i;
        for (i = 0; i < 8; i++) {
            p.setTextSize(35);
            p.setColor(Color.BLACK);
            canvas.drawText(String.valueOf(8 - i), offsetX, offsetY + sideLength * (i + 0.25f), p); //Draw row numbers
            canvas.drawText(String.valueOf((char) (i + 'a')), offsetX + sideLength * (i + 0.85f), offsetY + sideLength * 7.95f, p);  //Draw column alphabets
        }
    }

    private void drawPieces(Canvas canvas) {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                if (i != fromRow || j != fromCol) {
                    Piece piece = boardInterface.pieceAt(i, j);
                    if (piece != null) drawPieceAt(canvas, i, j, piece.getResID());
                }
        Piece piece = boardInterface.pieceAt(fromRow, fromCol);
        if (piece != null) {
            Bitmap b = bitmaps.get(piece.getResID());
            canvas.drawBitmap(b, null, new RectF(floatingPieceX - sideLength / 2, floatingPieceY - sideLength / 2, floatingPieceX + sideLength / 2, floatingPieceY + sideLength / 2), p);
        }
        drawGuides(canvas);
    }

    private void drawPieceAt(@NonNull Canvas canvas, int row, int col, int resID) {
        Bitmap b = bitmaps.get(resID);
        canvas.drawBitmap(b, null, new RectF(offsetX + col * sideLength, offsetY + (7 - row) * sideLength, offsetX + (col + 1) * sideLength, offsetY + (7 - row + 1) * sideLength), p);
    }

    private void drawGuides(Canvas canvas) {
        if (!legalMoves.isEmpty()) {
            Bitmap b = bitmaps.get(R.drawable.guide_blue);
            for (Integer move : legalMoves) {
                int row = move / 8, col = move % 8;
                canvas.drawBitmap(b, null, new RectF(offsetX + col * sideLength, offsetY + (7 - row) * sideLength, offsetX + (col + 1) * sideLength, offsetY + (7 - row + 1) * sideLength), p);
            }
            legalMoves.clear();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int toCol, toRow;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                legalMoves.clear();
                fromCol = (int) ((event.getX() - offsetX) / sideLength);
                fromRow = 7 - (int) ((event.getY() - offsetY) / sideLength);
                Piece selectedPiece = boardInterface.pieceAt(fromRow, fromCol);
                if (previousSelectedPiece != null && selectedPiece != previousSelectedPiece)
                    if (boardInterface.movePiece(previousSelectedPiece.getRow(), previousSelectedPiece.getCol(), fromRow, fromCol)) {
                        previousSelectedPiece = null;
                        fromRow = fromCol = -1;
                        break;
                    }
                break;

            case MotionEvent.ACTION_UP:
                toCol = (int) ((event.getX() - offsetX) / sideLength);
                toRow = 7 - (int) ((event.getY() - offsetY) / sideLength);
                selectedPiece = boardInterface.pieceAt(toRow, toCol);

                if (selectedPiece != null) if (boardInterface.isPieceToPlay(selectedPiece)) {
                    legalMoves = selectedPiece.getLegalMoves(boardInterface);
                    invalidate();
                }

                if (selectedPiece != null && previousSelectedPiece != null) {
                    if (selectedPiece != previousSelectedPiece && selectedPiece.getPlayerType() == previousSelectedPiece.getPlayerType())
                        previousSelectedPiece = selectedPiece;
                } else if (boardInterface.movePiece(fromRow, fromCol, toRow, toCol)) {
                    previousSelectedPiece = null;
                    legalMoves.clear();
                } else previousSelectedPiece = boardInterface.pieceAt(toRow, toCol);
                fromRow = fromCol = -1;
                break;

            case MotionEvent.ACTION_MOVE:
                floatingPieceX = (int) event.getX();
                floatingPieceY = (int) event.getY();
                previousSelectedPiece = null;
                invalidate();
                break;
        }
        return true;
    }

    private boolean isPieceToPlay(@NonNull Piece piece) {
        return piece.getPlayerType() == Player.WHITE && GameActivity.getGameState() == ChessState.WHITETOPLAY || piece.getPlayerType() == Player.BLACK && GameActivity.getGameState() == ChessState.BLACKTOPLAY;
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        Piece movingPiece = boardInterface.pieceAt(fromRow, fromCol);
        if (movingPiece == null || fromRow == toRow && fromCol == toCol || toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7)
            return false;
        if (!isPieceToPlay(movingPiece)) return false;
        Piece toPiece = boardInterface.pieceAt(toRow, toCol);
        if (toPiece != null)
            if (movingPiece.getPlayerType() != toPiece.getPlayerType() && movingPiece.canCapture(boardInterface, toPiece)) {
                movingPiece.moveTo(toRow, toCol);
                boardInterface.removePiece(toPiece);
                Log.d(TAG, "Move capture: " + toNotation(fromRow, fromCol) + " to " + toNotation(toRow, toCol));
                boardInterface.addToPGN(movingPiece);
                return true;
            }
        if (toPiece == null && movingPiece.canMoveTo(boardInterface, toRow, toCol)) {
            movingPiece.moveTo(toRow, toCol);
            Log.d(TAG, "Move: " + toNotation(fromRow, fromCol) + " to " + toNotation(toRow, toCol));
            boardInterface.addToPGN(movingPiece);
            return true;
        }
        Log.d(TAG, "Move invalid: " + toNotation(fromRow, fromCol) + " to " + toNotation(toRow, toCol));
        return false;   //Default return false
    }

    public String toNotation(int row, int col) {
        return "" + (char) ('a' + col) + (row + 1);
    }
}