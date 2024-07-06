package com.drdedd.simplechess_temp;

import static com.drdedd.simplechess_temp.data.DataConverter.toCol;
import static com.drdedd.simplechess_temp.data.DataConverter.toRow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.drdedd.simplechess_temp.GameData.BoardTheme;
import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.data.MoveAnnotation;
import com.drdedd.simplechess_temp.fragments.HomeFragment;
import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.pieces.King;
import com.drdedd.simplechess_temp.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom View to create a Chess board and design game interface
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class ChessBoard extends View {
    private static final String TAG = "ChessBoard";
    private final static int animationSpeed = 10;
    private BoardInterface boardInterface;
    private final float offsetX = 0f, offsetY = 0f;
    private final boolean cheatMode;
    private final Set<Integer> resIDs = Set.of(R.drawable.kb, R.drawable.qb, R.drawable.rb, R.drawable.bb, R.drawable.nb, R.drawable.pb, R.drawable.kbi, R.drawable.qbi, R.drawable.rbi, R.drawable.bbi, R.drawable.nbi, R.drawable.pbi, R.drawable.kw, R.drawable.qw, R.drawable.rw, R.drawable.bw, R.drawable.nw, R.drawable.pw, R.drawable.guide_blue, R.drawable.highlight, R.drawable.check, R.drawable.move_square);
    private final HashMap<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint p = new Paint();
    private final Resources res = getResources();
    private float sideLength = 130f, x, y;
    private int lightColor, darkColor, fromCol = -1, fromRow = -1, floatingPieceX = -1, floatingPieceY = -1, startX = -1, startY = -1, endX = -1, endY = -1;
    public int annotation = -1;
    private Piece previousSelectedPiece = null;
    public String fromSquare, toSquare;
    private HashMap<Piece, HashSet<Integer>> allLegalMoves;
    private HashSet<Integer> legalMoves = new HashSet<>();
    private boolean viewOnly;
    private Bitmap bitmap;

    public ChessBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
        DataManager dataManager = new DataManager(this.getContext());
        setTheme(dataManager.getBoardTheme());
        cheatMode = dataManager.cheatModeEnabled();
        MoveAnnotation.loadBitmaps(context);
    }

    public void setData(BoardInterface boardInterface, boolean invalidate) {
        this.boardInterface = boardInterface;
        this.viewOnly = invalidate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int min = Math.min(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(min, min);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        long start = System.nanoTime();
        super.onDraw(canvas);
        canvas.drawColor(getResources().getColor(R.color.black));
        int width = getWidth(), height = getHeight();
        float boardSize = Math.min(width, height);
        sideLength = boardSize / 8f;
        drawBoard(canvas);
        drawCoordinates(canvas);

        fromSquare = boardInterface.getBoardModel().fromSquare;
        toSquare = boardInterface.getBoardModel().toSquare;
//        Log.d(TAG, String.format("onDraw: fromSquare: %s toSquare: %s", fromSquare, toSquare));
        if (fromSquare != null && !fromSquare.isEmpty())
            highlightSquare(canvas, toRow(fromSquare), toCol(fromSquare), R.drawable.move_square);
        if (toSquare != null && !toSquare.isEmpty())
            highlightSquare(canvas, toRow(toSquare), toCol(toSquare), R.drawable.move_square);

        drawPieces(canvas);
        if (!cheatMode && !boardInterface.isGameTerminated()) {
            if (previousSelectedPiece != null)
                legalMoves = allLegalMoves.getOrDefault(previousSelectedPiece, null);
            drawGuides(canvas);
        }

        King whiteKing = boardInterface.getBoardModel().getWhiteKing(), blackKing = boardInterface.getBoardModel().getBlackKing();
        if (previousSelectedPiece != null && !boardInterface.isGameTerminated())
            highlightSquare(canvas, previousSelectedPiece.getRow(), previousSelectedPiece.getCol(), R.drawable.highlight);
        if (Player.WHITE.isInCheck())
            highlightSquare(canvas, whiteKing.getRow(), whiteKing.getCol(), R.drawable.check);
        if (Player.BLACK.isInCheck())
            highlightSquare(canvas, blackKing.getRow(), blackKing.getCol(), R.drawable.check);
        if (annotation != -1) drawAnnotation(canvas, toRow(toSquare), toCol(toSquare), annotation);
        long end = System.nanoTime();
        HomeFragment.printTime(TAG, "drawing board", end - start, -1);
    }

    public void setTheme(BoardTheme theme) {
        lightColor = res.getColor(theme.getLightColor());
        darkColor = res.getColor(theme.getDarkColor());
    }

    private void loadBitmaps() {
        for (Integer id : resIDs)
            bitmaps.put(id, getBitmap(getContext(), id));
    }

    private void highlightSquare(Canvas canvas, int row, int col, int resID) {
        Bitmap b = bitmaps.get(resID);
        if (b != null)
            canvas.drawBitmap(b, null, new RectF(offsetX + sideLength * col, offsetY + sideLength * (7 - row), offsetX + sideLength * (col + 1), offsetY + sideLength * (7 - row + 1)), p);
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
        Piece piece;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                if ((i != 7 - endY || j != endX) && (i != fromRow || j != fromCol)) {
                    piece = boardInterface.pieceAt(i, j);
                    if (piece != null) drawPieceAt(canvas, i, j, piece.getResID());
                }
        if (endX != -1 || endY != -1) drawAnimatedPiece(canvas, bitmap);
        else {
            piece = boardInterface.pieceAt(fromRow, fromCol);
            if (piece != null)
                if (cheatMode || !boardInterface.isGameTerminated() && boardInterface.isPieceToPlay(piece)) {
                    Bitmap b = bitmaps.get(piece.getResID());
                    if (b != null)
                        canvas.drawBitmap(b, null, new RectF(floatingPieceX - sideLength / 2, floatingPieceY - sideLength / 2, floatingPieceX + sideLength / 2, floatingPieceY + sideLength / 2), p);
                } else drawPieceAt(canvas, piece.getRow(), piece.getCol(), piece.getResID());
        }
    }

    private void drawPieceAt(@NonNull Canvas canvas, int row, int col, int resID) {
        Bitmap b = bitmaps.get(resID);
        if (b != null)
            canvas.drawBitmap(b, null, new RectF(offsetX + sideLength * col, offsetY + sideLength * (7 - row), offsetX + sideLength * (col + 1), offsetY + sideLength * (7 - row + 1)), p);
    }

    private void drawAnimatedPiece(Canvas canvas, Bitmap pieceBitmap) {
        try {
            if (pieceBitmap != null) {
                if (endX != -1 && x != endX * sideLength) {
                    float incrementX = ((endX - startX) * sideLength) / animationSpeed;
                    if (incrementX > 0 && x + incrementX > endX * sideLength || incrementX < 0 && x + incrementX < endX * sideLength)
                        x = endX * sideLength + offsetX;
                    else x += incrementX;
                }
                if (endY != -1 && y != endY * sideLength) {
                    float incrementY = ((endY - startY) * sideLength) / animationSpeed;
                    if (incrementY > 0 && y + incrementY > endY * sideLength || incrementY < 0 && y + incrementY < endY * sideLength)
                        y = endY * sideLength + offsetY;
                    else y += incrementY;
                }
                Thread.sleep(5);
                canvas.drawBitmap(pieceBitmap, null, new RectF(x, y, x + sideLength, y + sideLength), null);
                if (x == endX * sideLength + offsetX && y == endY * sideLength + offsetY)
                    endX = endY = -1;
                else if (endX != -1 || endY != -1) invalidate();
            } else {
                Piece piece = boardInterface.pieceAt(7 - endY, endX);
                if (piece != null) drawPieceAt(canvas, 7 - endY, endX, piece.getResID());
            }
        } catch (Exception e) {
            Log.e(TAG, "drawAnimatedPiece: Animation failed", e);
        }
    }

    private void drawGuides(Canvas canvas) {
        if (legalMoves == null) return;

        if (!legalMoves.isEmpty()) {
            Bitmap b = bitmaps.get(R.drawable.guide_blue);
            for (Integer move : legalMoves) {
                int row = move / 8, col = move % 8;
                if (b != null)
                    canvas.drawBitmap(b, null, new RectF(offsetX + sideLength * col, offsetY + sideLength * (7 - row), offsetX + sideLength * (col + 1), offsetY + sideLength * (7 - row + 1)), p);
            }
            legalMoves = null;
        }
    }

    private void drawAnnotation(Canvas canvas, int row, int col, int annotation) {
        Bitmap b = MoveAnnotation.getBitmap(annotation);
        if (b != null)
            canvas.drawBitmap(b, null, new RectF(offsetX + sideLength * (col + 0.6f), offsetY + sideLength * (7 - row - 0.1f), offsetX + sideLength * (col + 1.1f), offsetY + sideLength * (7 - row + 0.4f)), p);
    }

    public void initializeAnimation() {
        fromSquare = boardInterface.getBoardModel().fromSquare;
        toSquare = boardInterface.getBoardModel().toSquare;
        if (fromSquare != null && !fromSquare.isEmpty()) {
            startY = 7 - toRow(fromSquare);
            startX = toCol(fromSquare);
            x = offsetX + sideLength * startX;
            y = offsetY + sideLength * startY;
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
            Piece piece = boardInterface.pieceAt(7 - endY, endX);
            if (piece != null) bitmap = bitmaps.get(piece.getResID());
        } else Log.d(TAG, "initializeAnimation: To square empty");
//        Log.d(TAG, "initializeAnimation: Animation initialized");
    }

    public void initializeReverseAnimation(String fromSquare, String toSquare) {
        if (fromSquare != null && !fromSquare.isEmpty()) {
            startY = 7 - toRow(fromSquare);
            startX = toCol(fromSquare);
            x = offsetX + sideLength * startX;
            y = offsetY + sideLength * startY;
            Piece piece = boardInterface.pieceAt(7 - startY, startX);
            if (piece != null) bitmap = bitmaps.get(piece.getResID());
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
        } else Log.d(TAG, "initializeAnimation: To square empty");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int toCol, toRow;
        Piece selectedPiece;
        if (viewOnly || boardInterface.isGameTerminated()) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                allLegalMoves = boardInterface.getLegalMoves();
                fromCol = (int) ((event.getX() - offsetX) / sideLength);
                fromRow = 7 - (int) ((event.getY() - offsetY) / sideLength);
                selectedPiece = boardInterface.pieceAt(fromRow, fromCol);
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

                if (selectedPiece != null) {
                    if (boardInterface.isPieceToPlay(selectedPiece)) {
                        if (fromRow == toRow && fromCol == toCol)
                            previousSelectedPiece = selectedPiece;
                        if (!cheatMode)
                            legalMoves = allLegalMoves.getOrDefault(selectedPiece, null);
                    } else previousSelectedPiece = null;
                }
                if (selectedPiece != null && previousSelectedPiece != null) {
                    if (selectedPiece != previousSelectedPiece && selectedPiece.getPlayer() == previousSelectedPiece.getPlayer())
                        previousSelectedPiece = selectedPiece;
                } else if (boardInterface.movePiece(fromRow, fromCol, toRow, toCol))
                    previousSelectedPiece = null;
                else previousSelectedPiece = null;

                fromRow = fromCol = -1;
                invalidate();
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

    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else {
            if (drawable instanceof VectorDrawable) {
                return getBitmap((VectorDrawable) drawable);
            } else {
                throw new IllegalArgumentException("unsupported drawable type");
            }
        }
    }

    public void clearSelection() {
        previousSelectedPiece = null;
    }
}