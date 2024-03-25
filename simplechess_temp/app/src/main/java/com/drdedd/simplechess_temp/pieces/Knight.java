package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

/**
 * {@inheritDoc}
 */
public class Knight extends Piece {

    /**
     * Creates a new <code>Knight</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public Knight(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.KNIGHT, resID, unicode);
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        return Math.abs(row - getRow()) == 2 && Math.abs(col - getCol()) == 1 || Math.abs(row - getRow()) == 1 && Math.abs(col - getCol()) == 2;
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public HashSet<Integer> getPossibleMoves(BoardInterface boardInterface) {
        HashSet<Integer> possibleMoves = new HashSet<>();
        int row = getRow(), col = getCol(), i, j, newRow, newCol;
        for (i = -1; i <= 1; i += 2)
            for (j = -2; j <= 2; j += 4) {
                newRow = row + i;
                newCol = col + j;
                if (newCol >= 0 && newCol <= 7 && newRow >= 0 && newRow <= 7)
                    addMove(possibleMoves, boardInterface.pieceAt(newRow, newCol), newRow, newCol);

                newRow = row + j;
                newCol = col + i;
                if (newCol >= 0 && newCol <= 7 && newRow >= 0 && newRow <= 7)
                    addMove(possibleMoves, boardInterface.pieceAt(newRow, newCol), newRow, newCol);
            }
        return possibleMoves;
    }
}
