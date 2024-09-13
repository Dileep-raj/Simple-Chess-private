package com.drdedd.simplichess.game.pieces;

import android.util.Log;

import com.drdedd.simplichess.game.gameData.Player;
import com.drdedd.simplichess.game.gameData.Rank;
import com.drdedd.simplichess.interfaces.GameLogicInterface;

import java.util.HashSet;

/**
 * {@inheritDoc}
 */
public class King extends Piece {
    private boolean castled, longCastled, shortCastled;

    /**
     * Creates a new <code>King</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public King(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.KING, resID, unicode);
        moved = false;
        castled = shortCastled = longCastled = false;
    }

    @Override
    public boolean canMoveTo(GameLogicInterface gameLogicInterface, int row, int col) {
        return getPossibleMoves(gameLogicInterface).contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(GameLogicInterface gameLogicInterface, Piece capturingPiece) {
        return canMoveTo(gameLogicInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    /**
     * Checks whether the <code>King</code> can short castle
     *
     * @return <code>True|False</code>
     */
    public boolean canShortCastle(GameLogicInterface gameLogicInterface) {
        if (!getPlayer().isInCheck() && hasNotMoved() && !castled) {
            for (int i = getCol() + 1; i < 7; i++)
                if (gameLogicInterface.pieceAt(getRow(), i) != null) return false;
            Piece rook = gameLogicInterface.pieceAt(getRow(), 7);
            if (rook != null) if (rook.getRank() == Rank.ROOK) return rook.hasNotMoved();
        }
        return false;
    }

    /**
     * Checks whether the <code>King</code> can long castle
     *
     * @return <code>True|False</code>
     */
    public boolean canLongCastle(GameLogicInterface gameLogicInterface) {
        if (!getPlayer().isInCheck() && hasNotMoved() && !castled) {
            for (int i = getCol() - 1; i > 0; i--)
                if (gameLogicInterface.pieceAt(getRow(), i) != null) return false;
            Piece rook = gameLogicInterface.pieceAt(getRow(), 0);
            if (rook != null) if (rook.getRank() == Rank.ROOK) return rook.hasNotMoved();
        }
        return false;
    }

    /**
     * Long castles the <code>King</code>
     */
    public void longCastle(GameLogicInterface gameLogicInterface) {
        Piece rook = gameLogicInterface.pieceAt(getRow(), 0);
        rook.moveTo(getRow(), 3);
        this.moveTo(getRow(), getCol() - 2);
        castled = longCastled = true;
    }

    /**
     * Short castles the <code>King</code>
     */
    public void shortCastle(GameLogicInterface gameLogicInterface) {
        Piece rook = gameLogicInterface.pieceAt(getRow(), 7);
        rook.moveTo(getRow(), 5);
        this.moveTo(getRow(), getCol() + 2);
        castled = shortCastled = true;
    }

    /**
     * Returns whether the <code>King</code> has castled yet or not
     */
    public boolean isCastled() {
        return castled;
    }

    /**
     * Returns whether the <code>King</code> has not short castled
     *
     * @return <code>True|False</code>
     */
    public boolean isNotShortCastled() {
        return !shortCastled && !castled;
    }

    /**
     * Returns whether the <code>King</code> has not long castled
     *
     * @return <code>True|False</code>
     */
    public boolean isNotLongCastled() {
        return !longCastled && !castled;
    }

    /**
     * Checks if the <code>King</code> is checked by any opponent piece
     *
     * @return <code>True|False</code>
     */
    public boolean isChecked(GameLogicInterface gameLogicInterface) {
        String TAG = "King";
        HashSet<Piece> pieces = gameLogicInterface.getBoardModel().pieces;
        for (Piece piece : pieces)
            if (piece.getPlayer() != getPlayer() && !piece.isCaptured())
                if (piece.canCapture(gameLogicInterface, this)) {
                    Log.d(TAG, String.format("isChecked: %s checked by %s", getPlayer(), piece.getPosition()));
                    return true;
                }
        return false;
    }

    @Override
    public HashSet<Integer> getPossibleMoves(GameLogicInterface gameLogicInterface) {
        HashSet<Integer> possibleMoves = new HashSet<>();
        int row = getRow(), col = getCol(), i, j, newRow, newCol;
        for (i = -1; i <= 1; i++)
            for (j = -1; j <= 1; j++) {
                newRow = row + i;
                newCol = col + j;
                if (newRow == row && newCol == col || newCol < 0 || newCol > 7 || newRow < 0 || newRow > 7)
                    continue;
                addMove(possibleMoves, gameLogicInterface.pieceAt(newRow, newCol), newRow, newCol);
            }
        if (!getPlayer().isInCheck() && canShortCastle(gameLogicInterface))
            possibleMoves.add(getRow() * 8 + getCol() + 2);
        if (!getPlayer().isInCheck() && canLongCastle(gameLogicInterface))
            possibleMoves.add(getRow() * 8 + getCol() - 2);
        return possibleMoves;
    }
}