package com.drdedd.simplichess.views;

import static com.drdedd.simplichess.misc.MiscMethods.spToPixel;
import static com.drdedd.simplichess.misc.MiscMethods.toCol;
import static com.drdedd.simplichess.misc.MiscMethods.toRow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.drdedd.simplichess.game.gameData.BoardTheme;
import com.drdedd.simplichess.game.gameData.MoveAnnotation;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.fragments.HomeFragment;
import com.drdedd.simplichess.interfaces.GameLogicInterface;
import com.drdedd.simplichess.game.pieces.King;
import com.drdedd.simplichess.game.pieces.Piece;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom View to create a Chess board and design game interface
 */
public class ChessBoard extends View {
    private static final String TAG = "ChessBoard";
    private static final int animationSpeed = 10;
    private final float offsetX = 0f, offsetY = 0f;
    private final boolean cheatMode;
    private final Set<Integer> resIDs = Set.of(R.drawable.kb, R.drawable.qb, R.drawable.rb, R.drawable.bb, R.drawable.nb, R.drawable.pb, R.drawable.kbi, R.drawable.qbi, R.drawable.rbi, R.drawable.bbi, R.drawable.nbi, R.drawable.pbi, R.drawable.kw, R.drawable.qw, R.drawable.rw, R.drawable.bw, R.drawable.nw, R.drawable.pw, R.drawable.guide_blue, R.drawable.highlight, R.drawable.check, R.drawable.move_square);
    private final HashMap<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint p = new Paint();
    private final Typeface font;
    private GameLogicInterface gameLogicInterface;
    private HashMap<Piece, HashSet<Integer>> allLegalMoves;
    private HashSet<Integer> legalMoves = new HashSet<>();
    private Piece previousSelectedPiece = null;
    private String fromSquare, toSquare;
    private Bitmap bitmap;
    private float sideLength = 130f, x, y, fontSize;
    private int lightColor, darkColor, fromCol = -1, fromRow = -1, floatingPieceX = -1, floatingPieceY = -1, startX = -1, startY = -1, endX = -1, endY = -1;
    private boolean viewOnly, invertBlackPieces, flipBoard;
    public int annotation = -1;

    public ChessBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
        DataManager dataManager = new DataManager(getContext());
        setTheme(dataManager.getBoardTheme());
        cheatMode = dataManager.cheatModeEnabled();
        MoveAnnotation.loadBitmaps(context);
        font = ResourcesCompat.getFont(getContext(), R.font.roboto_medium);
        flipBoard = false;
    }

    /**
     * Sets board UI data
     *
     * @param gameLogicInterface GameLogicInterface of the board
     * @param viewOnly       View the game without touch event
     */
    public void setData(GameLogicInterface gameLogicInterface, boolean viewOnly) {
        this.gameLogicInterface = gameLogicInterface;
        this.viewOnly = viewOnly;
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
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.black));
        int width = getWidth(), height = getHeight();
        float boardSize = Math.min(width, height);
        sideLength = boardSize / 8f;
        fontSize = spToPixel(getResources().getDisplayMetrics(), (int) (sideLength / 12));
        drawBoard(canvas);
        drawCoordinates(canvas);

        fromSquare = gameLogicInterface.getBoardModel().fromSquare;
        toSquare = gameLogicInterface.getBoardModel().toSquare;
        if (fromSquare != null && !fromSquare.isEmpty())
            highlightSquare(canvas, toRow(fromSquare), toCol(fromSquare), R.drawable.move_square);
        if (toSquare != null && !toSquare.isEmpty())
            highlightSquare(canvas, toRow(toSquare), toCol(toSquare), R.drawable.move_square);

        drawPieces(canvas);
        if (!cheatMode && !gameLogicInterface.isGameTerminated()) {
            if (previousSelectedPiece != null)
                legalMoves = allLegalMoves.getOrDefault(previousSelectedPiece, null);
            drawGuides(canvas);
        }

        King whiteKing = gameLogicInterface.getBoardModel().getWhiteKing(), blackKing = gameLogicInterface.getBoardModel().getBlackKing();
        if (previousSelectedPiece != null && !gameLogicInterface.isGameTerminated())
            highlightSquare(canvas, previousSelectedPiece.getRow(), previousSelectedPiece.getCol(), R.drawable.highlight);
        if (Player.WHITE.isInCheck())
            highlightSquare(canvas, whiteKing.getRow(), whiteKing.getCol(), R.drawable.check);
        if (Player.BLACK.isInCheck())
            highlightSquare(canvas, blackKing.getRow(), blackKing.getCol(), R.drawable.check);
        if (annotation != -1) drawAnnotation(canvas, toRow(toSquare), toCol(toSquare));
        long end = System.nanoTime();
        HomeFragment.printTime(TAG, "drawing board", end - start, -1);
    }

    /**
     * Sets theme of the ChessBoard
     *
     * @param theme Theme of the board
     */
    public void setTheme(BoardTheme theme) {
        lightColor = ContextCompat.getColor(getContext(), theme.getLightColor());
        darkColor = ContextCompat.getColor(getContext(), theme.getDarkColor());
    }

    public void setInvertBlackPieces(boolean invertBlackPieces) {
        this.invertBlackPieces = invertBlackPieces;
    }

    public void flipBoard() {
        flipBoard = !flipBoard;
        invalidate();
    }

    /**
     * Loads all bitmaps used in the ChessBoard
     */
    private void loadBitmaps() {
        for (Integer id : resIDs)
            bitmaps.put(id, getBitmap(getContext(), id));
    }

    /**
     * Highlights a piece at a particular position
     *
     * @param canvas Canvas of the ChessBoard
     * @param row    Row of the piece
     * @param col    Column of the piece
     * @param resID  Resource id highlighting square
     */
    private void highlightSquare(Canvas canvas, int row, int col, int resID) {
        Bitmap b = bitmaps.get(resID);
        if (b != null) {
            float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale(scaleX, scaleY);
            matrix.postTranslate(offsetX + sideLength * (flipBoard ? 7 - col : col), offsetY + sideLength * (flipBoard ? row : 7 - row));
            canvas.drawBitmap(b, matrix, p);
        }
    }

    /**
     * Draws every component on the board
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawBoard(Canvas canvas) {
        int i, j;
        for (i = 0; i < 8; i++)
            for (j = 0; j < 8; j++) {
                p.setColor((i + j) % 2 == 0 ? lightColor : darkColor);
                canvas.drawRect(offsetX + j * sideLength, offsetY + i * sideLength, offsetX + (j + 1) * sideLength, offsetY + (i + 1) * sideLength, p);
            }
    }

    /**
     * Draws coordinates on the board
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawCoordinates(Canvas canvas) {
        int i;
        for (i = 0; i < 8; i++) {
            p.setTypeface(font);
            p.setTextSize(fontSize);
            p.setColor(i % 2 == 0 ? darkColor : lightColor);
            canvas.drawText(String.valueOf(flipBoard ? i + 1 : 8 - i), offsetX, offsetY + sideLength * (i + 0.25f), p); //Draw row numbers
            p.setColor(i % 2 == 0 ? lightColor : darkColor);
            canvas.drawText(String.valueOf((char) ((flipBoard ? 7 - i : i) + 'a')), offsetX + sideLength * (i + 0.85f), offsetY + sideLength * 7.95f, p);  //Draw column alphabets
        }
    }

    /**
     * Draws all pieces on the board
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawPieces(Canvas canvas) {
        for (Piece piece : gameLogicInterface.getBoardModel().pieces) {
            if (piece.isCaptured()) continue;
            int row = piece.getRow(), col = piece.getCol();
            if (flipBoard && row == endY && col == 7 - endX || !flipBoard && row == 7 - endY && col == endX || row == fromRow && col == fromCol)
                continue;
            drawPieceAt(canvas, row, col, piece);
        }
        if (endX != -1 || endY != -1) drawAnimatedPiece(canvas, bitmap);
        else {
            Piece piece = gameLogicInterface.pieceAt(fromRow, fromCol);
            if (piece != null)
                if (cheatMode || !gameLogicInterface.isGameTerminated() && gameLogicInterface.isPieceToPlay(piece)) {
                    Bitmap b = bitmaps.get(piece.getResID());
                    if (b != null) {
                        float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleX, scaleY);
                        matrix.postTranslate(floatingPieceX - sideLength / 2, floatingPieceY - sideLength / 2);
                        canvas.drawBitmap(b, matrix, p);
//                        canvas.drawBitmap(b, null, new RectF(floatingPieceX - sideLength / 2, floatingPieceY - sideLength / 2, floatingPieceX + sideLength / 2, floatingPieceY + sideLength / 2), p);
                    }
                } else drawPieceAt(canvas, piece.getRow(), piece.getCol(), piece);
        }
    }

    /**
     * Draws a piece at a particular position
     *
     * @param canvas Canvas of the ChessBoard
     * @param row    Row of the piece
     * @param col    Column of the piece
     * @param piece  Piece to be drawn
     */
    private void drawPieceAt(@NonNull Canvas canvas, int row, int col, Piece piece) {
        Bitmap b = bitmaps.get(piece.getResID());
        if (b != null) {
            float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale(scaleX, scaleY);
            if (invertBlackPieces && !piece.isWhite()) {
                matrix.postScale(1f, -1f);
                matrix.postTranslate(0, sideLength);
            }
            matrix.postTranslate(offsetX + sideLength * (flipBoard ? 7 - col : col), offsetY + sideLength * (flipBoard ? row : 7 - row));
            canvas.drawBitmap(b, matrix, p);
        }
    }

    /**
     * Draws a piece in the path of its animation
     *
     * @param canvas      Canvas of the ChessBoard
     * @param pieceBitmap Bitmap of the piece to be drawn
     */
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
                if (x == endX * sideLength + offsetX && y == endY * sideLength + offsetY) {
                    endX = endY = -1;
                    invalidate();
                } else if (endX != -1 || endY != -1) invalidate();
            } else {
                Piece piece = gameLogicInterface.pieceAt(7 - endY, endX);
                if (piece != null) drawPieceAt(canvas, 7 - endY, endX, piece);
            }
        } catch (Exception e) {
            Log.e(TAG, "drawAnimatedPiece: Animation failed", e);
        }
    }

    /**
     * Draws guides to show legal moves of the selected piece
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawGuides(Canvas canvas) {
        if (legalMoves == null) return;

        if (!legalMoves.isEmpty()) {
            Bitmap b = bitmaps.get(R.drawable.guide_blue);
            if (b != null) {
                float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
                for (Integer move : legalMoves) {
                    int row = move / 8, col = move % 8;
                    Matrix matrix = new Matrix();
                    matrix.postScale(scaleX, scaleY);
                    matrix.postTranslate(offsetX + sideLength * (flipBoard ? 7 - col : col), offsetY + sideLength * (flipBoard ? row : 7 - row));
                    canvas.drawBitmap(b, matrix, p);

                }
            }
            legalMoves = null;
        }
    }

    /**
     * Draws annotation for a move
     *
     * @param canvas Canvas of the ChessBoard
     * @param row    Row of the piece
     * @param col    Column of the piece
     */
    private void drawAnnotation(Canvas canvas, int row, int col) {
        Bitmap b = MoveAnnotation.getBitmap(annotation);
        if (b != null) {
            float scale = sideLength / 2.5f / b.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(offsetX + sideLength * (0.6f + (flipBoard ? 7 - col : col)), offsetY + sideLength * ((flipBoard ? row : 7 - row) - 0.1f));
            canvas.drawBitmap(b, matrix, p);
        }
    }

    /**
     * Initializes animation variables
     */
    public void initializeAnimation() {
        fromSquare = gameLogicInterface.getBoardModel().fromSquare;
        toSquare = gameLogicInterface.getBoardModel().toSquare;
        if (fromSquare != null && !fromSquare.isEmpty()) {
            startY = 7 - toRow(fromSquare);
            startX = toCol(fromSquare);
            if (flipBoard) {
                startY = 7 - startY;
                startX = 7 - startX;
            }
            y = offsetY + sideLength * startY;
            x = offsetX + sideLength * startX;
//            Log.d(TAG, "initializeAnimation: Start square: " + toNotation((flipBoard ? startY : 7 - startY), (flipBoard ? 7 - startX : startX)));
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
            if (flipBoard) {
                endY = 7 - endY;
                endX = 7 - endX;
            }
//            Log.d(TAG, "initializeAnimation: End square: " + toNotation((flipBoard ? endY : 7 - endY), (flipBoard ? 7 - endX : endX)));
            Piece piece = gameLogicInterface.pieceAt((flipBoard ? endY : 7 - endY), (flipBoard ? 7 - endX : endX));
            if (piece != null) bitmap = bitmaps.get(piece.getResID());
        } else Log.d(TAG, "initializeAnimation: To square empty");
//        Log.d(TAG, "initializeAnimation: Animation initialized");
    }

    /**
     * Initializes reverse animation for a move
     *
     * @param fromSquare Starting position of the piece
     * @param toSquare   Ending position of the piece
     */
    public void initializeReverseAnimation(String fromSquare, String toSquare) {
        if (fromSquare != null && !fromSquare.isEmpty()) {
            startY = 7 - toRow(fromSquare);
            startX = toCol(fromSquare);
            if (flipBoard) {
                startY = 7 - startY;
                startX = 7 - startX;
            }
            y = offsetY + sideLength * startY;
            x = offsetX + sideLength * startX;
            Piece piece = gameLogicInterface.pieceAt(7 - startY, startX);
            if (piece != null) bitmap = bitmaps.get(piece.getResID());
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
            if (flipBoard) {
                endY = 7 - endY;
                endX = 7 - endX;
            }
        } else Log.d(TAG, "initializeAnimation: To square empty");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int toCol, toRow;
        Piece selectedPiece;
        if (viewOnly || gameLogicInterface.isGameTerminated()) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                allLegalMoves = gameLogicInterface.getLegalMoves();
                fromCol = (int) ((event.getX() - offsetX) / sideLength);
                fromRow = 7 - (int) ((event.getY() - offsetY) / sideLength);
                if (flipBoard) {
                    fromCol = 7 - fromCol;
                    fromRow = 7 - fromRow;
                }
                selectedPiece = gameLogicInterface.pieceAt(fromRow, fromCol);
                if (previousSelectedPiece != null && selectedPiece != previousSelectedPiece)
                    if (gameLogicInterface.move(previousSelectedPiece.getRow(), previousSelectedPiece.getCol(), fromRow, fromCol)) {
                        previousSelectedPiece = null;
                        fromRow = fromCol = -1;
                        break;
                    }
                break;

            case MotionEvent.ACTION_UP:
                toCol = (int) ((event.getX() - offsetX) / sideLength);
                toRow = 7 - (int) ((event.getY() - offsetY) / sideLength);
                if (flipBoard) {
                    toCol = 7 - toCol;
                    toRow = 7 - toRow;
                }
                selectedPiece = gameLogicInterface.pieceAt(toRow, toCol);

                if (selectedPiece != null) {
                    if (gameLogicInterface.isPieceToPlay(selectedPiece)) {
                        if (fromRow == toRow && fromCol == toCol)
                            previousSelectedPiece = selectedPiece;
                        if (!cheatMode)
                            legalMoves = allLegalMoves.getOrDefault(selectedPiece, null);
                    } else previousSelectedPiece = null;
                }
                if (selectedPiece != null && previousSelectedPiece != null) {
                    if (selectedPiece != previousSelectedPiece && selectedPiece.getPlayer() == previousSelectedPiece.getPlayer())
                        previousSelectedPiece = selectedPiece;
                } else if (gameLogicInterface.move(fromRow, fromCol, toRow, toCol))
                    previousSelectedPiece = null;
                else previousSelectedPiece = null;

                fromRow = fromCol = -1;
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                floatingPieceX = (int) event.getX();
                floatingPieceY = (int) event.getY();
                previousSelectedPiece = null;
//                if (flipBoard) {
//                    floatingPieceX = 7 - floatingPieceX;
//                    floatingPieceY = 7 - floatingPieceY;
//                }
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