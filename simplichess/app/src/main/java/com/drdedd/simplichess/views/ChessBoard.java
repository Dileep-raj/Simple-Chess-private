package com.drdedd.simplichess.views;

import static com.drdedd.simplichess.misc.MiscMethods.spToPixel;
import static com.drdedd.simplichess.misc.MiscMethods.toCol;
import static com.drdedd.simplichess.misc.MiscMethods.toNotation;
import static com.drdedd.simplichess.misc.MiscMethods.toRow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.drdedd.simplichess.R;
import com.drdedd.simplichess.data.DataManager;
import com.drdedd.simplichess.data.Regexes;
import com.drdedd.simplichess.fragments.HomeFragment;
import com.drdedd.simplichess.game.gameData.BoardTheme;
import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.pieces.King;
import com.drdedd.simplichess.game.pieces.Piece;
import com.drdedd.simplichess.interfaces.GameLogicInterface;
import com.drdedd.simplichess.misc.MiscMethods;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Custom View to create a Chess board and design game interface
 */
public class ChessBoard extends View {
    private static final String TAG = "ChessBoard";
    private static final int animationSpeed = 10;
    private static final float radius = 10, arrowAngle = (float) (Math.PI / 4.0f);
    private final int DEFAULT_LIGHT, DEFAULT_DARK;
    private final boolean cheatMode;
    private final Set<Integer> resIDs = Set.of(R.drawable.kb, R.drawable.qb, R.drawable.rb, R.drawable.bb, R.drawable.nb, R.drawable.pb, R.drawable.kw, R.drawable.qw, R.drawable.rw, R.drawable.bw, R.drawable.nw, R.drawable.pw, R.drawable.guide_blue, R.drawable.highlight, R.drawable.check, R.drawable.move_square, R.drawable.wooden_board, R.drawable.annotation_best, R.drawable.annotation_brilliant, R.drawable.annotation_book, R.drawable.annotation_great, R.drawable.annotation_blunder, R.drawable.annotation_inaccuracy, R.drawable.annotation_interesting, R.drawable.annotation_mistake);
    private final HashMap<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint p = new Paint(), arrowPaint;
    private final Matrix annotationMatrix = new Matrix(), pieceMatrix = new Matrix();
    private final HashSet<String> arrows = new HashSet<>();
    private GameLogicInterface gameLogicInterface;
    private HashMap<String, HashSet<Integer>> allLegalMoves;
    private HashSet<Integer> legalMoves = new HashSet<>();
    private Piece previousSelectedPiece = null;
    private String position, fromSquare, toSquare, annotationSquare, selectedSquare, tempArrow;
    private Bitmap bitmap;
    private float sideLength = 130f, x, y, fontSize;
    private int lightColor, darkColor, fromCol = -1, fromRow = -1, floatingPieceX = -1, floatingPieceY = -1, startX = -1, startY = -1, endX = -1, endY = -1, annotation = -1;
    private boolean viewOnly, invertBlackPieces, flipped, boardImage, analysis;

    public ChessBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        DEFAULT_LIGHT = ContextCompat.getColor(context, R.color.theme_default_light);
        DEFAULT_DARK = ContextCompat.getColor(context, R.color.theme_default_dark);
        boolean setBoardTheme = true;

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ChessBoard);
            setColors(attributes.getColor(R.styleable.ChessBoard_lightColor, DEFAULT_LIGHT), attributes.getColor(R.styleable.ChessBoard_darkColor, DEFAULT_DARK));
            setBoardTheme = false;
            attributes.recycle();
        }
        loadBitmaps();
        DataManager dataManager = new DataManager(getContext());
        if (setBoardTheme) setTheme(dataManager.getBoardTheme());
        cheatMode = dataManager.getBoolean(DataManager.CHEAT_MODE);
        boardImage = dataManager.getBoolean(DataManager.USE_BOARD_IMAGE);
        p.setTypeface(ResourcesCompat.getFont(getContext(), R.font.roboto_medium));
        flipped = false;
        arrowPaint = new Paint();
        arrowPaint.setStrokeWidth(20f);
        arrowPaint.setColor(Color.RED);
        arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    /**
     * Sets board UI data
     *
     * @param gameLogicInterface GameLogicInterface of the board
     * @param viewOnly           View the game without touch event
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

        if (position != null && !position.isEmpty()) drawPieces(canvas);
        else if (gameLogicInterface != null) {
            fromSquare = gameLogicInterface.getBoardModel().fromSquare;
            toSquare = gameLogicInterface.getBoardModel().toSquare;
            if (fromSquare != null && !fromSquare.isEmpty())
                highlightSquare(canvas, toRow(fromSquare), toCol(fromSquare), R.drawable.move_square);
            if (toSquare != null && !toSquare.isEmpty())
                highlightSquare(canvas, toRow(toSquare), toCol(toSquare), R.drawable.move_square);

            drawPieces(canvas);
            if (!cheatMode && !gameLogicInterface.isGameTerminated()) drawGuides(canvas);

            King whiteKing = gameLogicInterface.getBoardModel().getWhiteKing(), blackKing = gameLogicInterface.getBoardModel().getBlackKing();
            if (previousSelectedPiece != null && !gameLogicInterface.isGameTerminated())
                highlightSquare(canvas, previousSelectedPiece.getRow(), previousSelectedPiece.getCol(), R.drawable.highlight);
            if (Player.WHITE.isInCheck())
                highlightSquare(canvas, whiteKing.getRow(), whiteKing.getCol(), R.drawable.check);
            if (Player.BLACK.isInCheck())
                highlightSquare(canvas, blackKing.getRow(), blackKing.getCol(), R.drawable.check);
            annotationSquare = toSquare;
        } else Log.d(TAG, "onDraw: GameLogicInterface null");
        if (annotation != -1 && annotationSquare != null && !annotationSquare.isEmpty())
            drawAnnotation(canvas, toRow(annotationSquare), toCol(annotationSquare));
        drawArrows(canvas);
        long end = System.nanoTime();
        HomeFragment.printTime(TAG, "drawing board", end - start, -1);
    }

    /**
     * Sets theme of the ChessBoard
     *
     * @param theme Theme of the board
     */
    public void setTheme(BoardTheme theme) {
        try {
            lightColor = ContextCompat.getColor(getContext(), theme.getLightColor());
            darkColor = ContextCompat.getColor(getContext(), theme.getDarkColor());
        } catch (Exception e) {
            lightColor = DEFAULT_LIGHT;
            darkColor = DEFAULT_DARK;
        }
    }

    /**
     * Set board image as background
     *
     * @param boardImage Use board image
     */
    public void setBoardImage(boolean boardImage) {
        this.boardImage = boardImage;
    }

    /**
     * Set light and dark square colors
     *
     * @param lightColor Light color int
     * @param darkColor  Dark color int
     */
    public void setColors(@ColorInt int lightColor, @ColorInt int darkColor) {
        this.lightColor = lightColor;
        this.darkColor = darkColor;
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
            pieceMatrix.reset();
            pieceMatrix.postScale(scaleX, scaleY);
            pieceMatrix.postTranslate(sideLength * (flipped ? 7 - col : col), sideLength * (flipped ? row : 7 - row));
            canvas.drawBitmap(b, pieceMatrix, p);
        }
    }

    /**
     * Draws every component on the board
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawBoard(Canvas canvas) {
        if (boardImage) {
            Bitmap b = bitmaps.get(R.drawable.wooden_board);
            if (b != null) {
                canvas.drawBitmap(b, null, new RectF(0, 0, getWidth(), getWidth()), p);
                return;
            }
        }
        int i, j;
        for (i = 0; i < 8; i++)
            for (j = 0; j < 8; j++) {
                p.setColor((i + j) % 2 == 0 ? lightColor : darkColor);
                canvas.drawRect(j * sideLength, i * sideLength, (j + 1) * sideLength, (i + 1) * sideLength, p);
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
            p.setTextSize(fontSize);
            p.setColor(i % 2 == 0 ? darkColor : lightColor);
            canvas.drawText(String.valueOf(flipped ? i + 1 : 8 - i), 0f, sideLength * (i + 0.25f), p); //Draw row numbers
            p.setColor(i % 2 == 0 ? lightColor : darkColor);
            canvas.drawText(String.valueOf((char) ((flipped ? 7 - i : i) + 'a')), sideLength * (i + 0.85f), sideLength * 7.95f, p);  //Draw column alphabets
        }
    }

    /**
     * Draws all pieces on the board
     *
     * @param canvas Canvas of the ChessBoard
     */
    private void drawPieces(Canvas canvas) {
        if (position != null) {
            StringTokenizer boardTokens = new StringTokenizer(position, "/");
            int i, row = 7, col;
            int resId;
            while (boardTokens.hasMoreTokens()) {
                String rank = boardTokens.nextToken();
                for (i = 0, col = 0; i < rank.length(); i++) {
                    char ch = rank.charAt(i);
                    if (Character.isDigit(ch)) {
                        col += ch - '0';
                        continue;
                    }
                    if (i > 8) {
                        Log.d(TAG, "parseFEN: Invalid FEN! found " + col + " columns in rank " + (i + 1));
                        return;
                    }
                    boolean isWhite = Character.isUpperCase(ch);
                    switch (Character.toLowerCase(ch)) {
                        case 'k':
                            resId = isWhite ? R.drawable.kw : R.drawable.kb;
                            break;
                        case 'q':
                            resId = isWhite ? R.drawable.qw : R.drawable.qb;
                            break;
                        case 'r':
                            resId = isWhite ? R.drawable.rw : R.drawable.rb;
                            break;
                        case 'b':
                            resId = isWhite ? R.drawable.bw : R.drawable.bb;
                            break;
                        case 'n':
                            resId = isWhite ? R.drawable.nw : R.drawable.nb;
                            break;
                        case 'p':
                            resId = isWhite ? R.drawable.pw : R.drawable.pb;
                            break;
                        default:
                            Log.d(TAG, String.format("parseFEN: Invalid FEN! found invalid character '%s' in FEN: %s", ch, position));
                            return;
                    }
                    highlightSquare(canvas, row, col, resId);
                    col++;
                }
                row--;
            }
        } else if (gameLogicInterface != null) {
            for (Piece piece : gameLogicInterface.getBoardModel().pieces) {
                if (piece.isCaptured()) continue;
                int row = piece.getRow(), col = piece.getCol();
                if (flipped && row == endY && col == 7 - endX || !flipped && row == 7 - endY && col == endX || row == fromRow && col == fromCol && !analysis)
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
                        }
                    } else drawPieceAt(canvas, piece.getRow(), piece.getCol(), piece);
            }
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
        if (MiscMethods.notWithinBounds(row) || MiscMethods.notWithinBounds(col)) return;
        Bitmap b = bitmaps.get(piece.getResID());
        if (b != null) {
            float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
            pieceMatrix.reset();
            pieceMatrix.postScale(scaleX, scaleY);
            if (invertBlackPieces && !piece.isWhite()) {
                pieceMatrix.postScale(1f, -1f);
                pieceMatrix.postTranslate(0, sideLength);
            }
            pieceMatrix.postTranslate(sideLength * (flipped ? 7 - col : col), sideLength * (flipped ? row : 7 - row));
            canvas.drawBitmap(b, pieceMatrix, p);
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
                        x = endX * sideLength;
                    else x += incrementX;
                }
                if (endY != -1 && y != endY * sideLength) {
                    float incrementY = ((endY - startY) * sideLength) / animationSpeed;
                    if (incrementY > 0 && y + incrementY > endY * sideLength || incrementY < 0 && y + incrementY < endY * sideLength)
                        y = endY * sideLength;
                    else y += incrementY;
                }
                Thread.sleep(5);
                canvas.drawBitmap(pieceBitmap, null, new RectF(x, y, x + sideLength, y + sideLength), null);
                if (x == endX * sideLength && y == endY * sideLength) {
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
        if (selectedSquare != null) legalMoves = allLegalMoves.getOrDefault(selectedSquare, null);
        else if (previousSelectedPiece != null)
            legalMoves = allLegalMoves.getOrDefault(previousSelectedPiece.getSquare(), null);

        if (legalMoves != null && !legalMoves.isEmpty()) {
            Bitmap b = bitmaps.get(R.drawable.guide_blue);
            if (b != null) {
                float scaleX = (sideLength) / b.getWidth(), scaleY = (sideLength) / b.getHeight();
                for (Integer move : legalMoves) {
                    int row = move / 8, col = move % 8;
                    Matrix matrix = new Matrix();
                    matrix.postScale(scaleX, scaleY);
                    matrix.postTranslate(sideLength * (flipped ? 7 - col : col), sideLength * (flipped ? row : 7 - row));
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
        if (endX != -1 || endY != -1) return;
        Bitmap b = bitmaps.get(annotation);
        if (b != null) {
            float scale = sideLength / 2.5f / b.getWidth();
            annotationMatrix.reset();
            annotationMatrix.postScale(scale, scale);
            annotationMatrix.postTranslate(sideLength * (0.6f + (flipped ? 7 - col : col)), sideLength * ((flipped ? row : 7 - row) - 0.1f));
            canvas.drawBitmap(b, annotationMatrix, p);
        }
    }

    private void drawArrows(Canvas canvas) {
        if (tempArrow != null && tempArrow.length() >= 4) {
            String from = tempArrow.substring(0, 2), to = tempArrow.substring(2, 4);
            if (!from.equals(to)) {
                int fromRow = toRow(from), fromCol = toCol(from), toRow = toRow(to), toCol = toCol(to);
                if (!MiscMethods.notWithinBounds(fromCol) && !MiscMethods.notWithinBounds(fromRow) && !MiscMethods.notWithinBounds(toRow) && !MiscMethods.notWithinBounds(toCol))
                    drawArrow(canvas, (fromCol + 0.5f) * sideLength, (7.5f - fromRow) * sideLength, (toCol + 0.5f) * sideLength, (7.5f - toRow) * sideLength);
            }
        }
        for (String s : arrows) {
            if (s == null || s.length() < 4) continue;
            String from = s.substring(0, 2), to = s.substring(2, 4);
            if (from.equals(to)) continue;
            int fromRow = toRow(from), fromCol = toCol(from), toRow = toRow(to), toCol = toCol(to);
            if (MiscMethods.notWithinBounds(fromCol) || MiscMethods.notWithinBounds(fromRow) || MiscMethods.notWithinBounds(toRow) || MiscMethods.notWithinBounds(toCol))
                continue;
            drawArrow(canvas, (fromCol + 0.5f) * sideLength, (7.5f - fromRow) * sideLength, (toCol + 0.5f) * sideLength, (7.5f - toRow) * sideLength);
        }
    }

    private void drawArrow(@NonNull Canvas canvas, float from_x, float from_y, float to_x, float to_y) {
        float lineAngle = (float) (Math.atan2(to_y - from_y, to_x - from_x));

        canvas.drawLine(from_x, from_y, to_x, to_y, arrowPaint);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(to_x, to_y);
        path.lineTo((float) (to_x - radius * Math.cos(lineAngle - (arrowAngle / 2.0))), (float) (to_y - radius * Math.sin(lineAngle - (arrowAngle / 2.0))));
        path.lineTo((float) (to_x - radius * Math.cos(lineAngle + (arrowAngle / 2.0))), (float) (to_y - radius * Math.sin(lineAngle + (arrowAngle / 2.0))));
        path.close();

        canvas.drawPath(path, arrowPaint);
    }

    /**
     * Initializes animation variables
     */
    public void initializeAnimation(String fromSquare, String toSquare) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        if (fromSquare != null && !fromSquare.isEmpty()) {
            startY = 7 - toRow(fromSquare);
            startX = toCol(fromSquare);
            if (MiscMethods.notWithinBounds(startY) || MiscMethods.notWithinBounds(startX)) return;
            if (flipped) {
                startY = 7 - startY;
                startX = 7 - startX;
            }
            y = sideLength * startY;
            x = sideLength * startX;
//            Log.d(TAG, "initializeAnimation: Start square: " + toNotation((flipBoard ? startY : 7 - startY), (flipBoard ? 7 - startX : startX)));
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
            if (MiscMethods.notWithinBounds(endY) || MiscMethods.notWithinBounds(endX)) return;
            if (flipped) {
                endY = 7 - endY;
                endX = 7 - endX;
            }
//            Log.d(TAG, "initializeAnimation: End square: " + toNotation((flipBoard ? endY : 7 - endY), (flipBoard ? 7 - endX : endX)));
            Piece piece = gameLogicInterface.pieceAt((flipped ? endY : 7 - endY), (flipped ? 7 - endX : endX));
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
            if (MiscMethods.notWithinBounds(startY) || MiscMethods.notWithinBounds(startX)) return;
            if (flipped) {
                startY = 7 - startY;
                startX = 7 - startX;
            }
            y = sideLength * startY;
            x = sideLength * startX;
            Piece piece = gameLogicInterface.pieceAt(7 - startY, startX);
            if (piece != null) bitmap = bitmaps.get(piece.getResID());
        } else Log.d(TAG, "initializeAnimation: From square empty");
        if (toSquare != null && !toSquare.isEmpty()) {
            endY = 7 - toRow(toSquare);
            endX = toCol(toSquare);
            if (MiscMethods.notWithinBounds(endY) || MiscMethods.notWithinBounds(endX)) return;
            if (flipped) {
                endY = 7 - endY;
                endX = 7 - endX;
            }
        } else Log.d(TAG, "initializeAnimation: To square empty");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int toCol, toRow;

        if (analysis) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    fromCol = (int) ((event.getX()) / sideLength);
                    fromRow = 7 - (int) ((event.getY()) / sideLength);
                    if (flipped) {
                        fromCol = 7 - fromCol;
                        fromRow = 7 - fromRow;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    toCol = (int) ((event.getX()) / sideLength);
                    toRow = 7 - (int) ((event.getY()) / sideLength);
                    if (flipped) {
                        toCol = 7 - toCol;
                        toRow = 7 - toRow;
                    }
                    if (fromRow == toRow && fromCol == toCol) clearArrows();
                    else addArrow(toNotation(fromRow, fromCol) + toNotation(toRow, toCol));
                    tempArrow = null;
                    fromRow = fromCol = -1;
                    invalidate();
                    break;

                case MotionEvent.ACTION_MOVE:
                    toCol = (int) ((event.getX()) / sideLength);
                    toRow = 7 - (int) ((event.getY()) / sideLength);
                    if (flipped) {
                        toCol = 7 - toCol;
                        toRow = 7 - toRow;
                    }
                    tempArrow = toNotation(fromRow, fromCol) + toNotation(toRow, toCol);
                    invalidate();
                    break;
            }
            return true;
        }

        Piece selectedPiece;
        if (viewOnly || gameLogicInterface == null || gameLogicInterface.isGameTerminated()) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                allLegalMoves = gameLogicInterface.getAllLegalMoves();
                fromCol = (int) ((event.getX()) / sideLength);
                fromRow = 7 - (int) ((event.getY()) / sideLength);
                if (flipped) {
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
                toCol = (int) ((event.getX()) / sideLength);
                toRow = 7 - (int) ((event.getY()) / sideLength);
                if (flipped) {
                    toCol = 7 - toCol;
                    toRow = 7 - toRow;
                }
                selectedPiece = gameLogicInterface.pieceAt(toRow, toCol);

                if (selectedPiece != null) {
                    if (gameLogicInterface.isPieceToPlay(selectedPiece)) {
                        if (fromRow == toRow && fromCol == toCol)
                            previousSelectedPiece = selectedPiece;
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

    /**
     * Rotate the board view
     */
    public void flipBoard() {
        flipped = !flipped;
        invalidate();
    }

    public void setInvertBlackPieces(boolean invertBlackPieces) {
        this.invertBlackPieces = invertBlackPieces;
    }

    /**
     * Select a piece in a given position
     *
     * @param row Row number of the piece
     * @param col Column number of the piece
     */
    public void setSelection(int row, int col) {
        if (gameLogicInterface != null)
            previousSelectedPiece = gameLogicInterface.pieceAt(row, col);
        Log.d(TAG, String.format("setSelection: Row: %d Col: %d Piece: %s", row, col, previousSelectedPiece));
    }

    public void setAllLegalMoves(HashMap<String, HashSet<Integer>> allLegalMoves) {
        this.allLegalMoves = allLegalMoves;
    }

    /**
     * Enable/Disable touch interaction with the board
     *
     * @param viewOnly Set board to view only
     */
    public void setViewOnly(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    public void setAnalysis(boolean analysis) {
        this.analysis = analysis;
    }

    /**
     * Set the position of the board from an FEN
     *
     * @param p FEN of the position
     */
    public void setPosition(String p) {
        if (p == null) {
            position = null;
            return;
        }
        if (p.matches(Regexes.FENRegex)) position = p.substring(0, p.indexOf(' '));
        else Log.d(TAG, "setPosition: Invalid FEN!: " + p);
    }

    /**
     * Annotation of the move played
     *
     * @param annotation Annotation resource id
     */
    public void setAnnotation(int annotation) {
        this.annotation = annotation;
    }

    /**
     * Set a square for annotation
     *
     * @param annotationSquare Square to annotate
     */
    public void setAnnotationSquare(String annotationSquare) {
        this.annotationSquare = annotationSquare;
    }

    public void addArrow(String fromTo) {
        if (fromTo == null || fromTo.isEmpty() || fromTo.length() < 4) return;
        if (arrows.contains(fromTo)) arrows.remove(fromTo);
        else arrows.add(fromTo);
        invalidate();
    }

    public void resetAll() {
        previousSelectedPiece = null;
        allLegalMoves = new HashMap<>();
        arrows.clear();
    }

    /**
     * Clears any selected piece on board
     */
    public void clearSelection() {
        previousSelectedPiece = null;
    }

    public void clearArrows() {
        arrows.clear();
    }

    /**
     * @return <code>true|false</code> - board flipped
     */
    public boolean isFlipped() {
        return flipped;
    }
}