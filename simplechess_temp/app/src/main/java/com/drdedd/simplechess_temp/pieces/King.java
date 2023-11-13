package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;
import java.util.Set;

public class King extends Piece {
    boolean moved;

    public King(Player player, int row, int col, int absolutePosition, int resID) {
        super(player, row, col, absolutePosition, Rank.KING, resID);
        moved = false;
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        return Math.abs(row - getRow()) <= 1 && Math.abs(col - getCol()) <= 1;
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public Set<Integer> getLegalMoves(BoardInterface boardInterface) {
        HashSet<Integer> legalMoves = new HashSet<>();
        int row = getRow(), col = getCol(), i, j, newRow, newCol;
        Piece tempPiece;
        for (i = -1; i <= 1; i++)
            for (j = -1; j <= 1; j++) {
                newRow = row + i;
                newCol = col + j;
                if (newRow == row && newCol == col || newCol < 0 || newCol > 7 || newRow < 0 || newRow > 7)
                    continue;
                tempPiece = boardInterface.pieceAt(newRow, newCol);
                if (tempPiece == null) legalMoves.add(newRow * 8 + newCol);
                else if (tempPiece.getPlayerType() != getPlayerType())
                    legalMoves.add(newRow * 8 + newCol);
            }
        return legalMoves;
    }

}