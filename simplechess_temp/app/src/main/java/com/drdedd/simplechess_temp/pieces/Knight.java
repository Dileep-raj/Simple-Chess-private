package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

public class Knight extends Piece {
    public Knight(Player player, int row, int col, int absolutePosition, int resID) {
        super(player, row, col, absolutePosition, Rank.KNIGHT, resID);
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
    public HashSet<Integer> getLegalMoves(BoardInterface boardInterface) {
        HashSet<Integer> legalMoves = new HashSet<>();
        Piece tempPiece;
        int row = getRow(), col = getCol(), i, j, newRow, newCol;
        for (i = -1; i <= 1; i += 2)
            for (j = -2; j <= 2; j += 4) {
                newRow = row + i;
                newCol = col + j;
                if (newCol >= 0 && newCol <= 7 && newRow >= 0 && newRow <= 7) {
                    tempPiece = boardInterface.pieceAt(newRow, newCol);
                    if (tempPiece == null) legalMoves.add(newRow * 8 + newCol);
                    else if (tempPiece.getPlayerType() != getPlayerType())
                        legalMoves.add(newRow * 8 + newCol);
                }
                newRow = row + j;
                newCol = col + i;
                if (newCol >= 0 && newCol <= 7 && newRow >= 0 && newRow <= 7) {
                    tempPiece = boardInterface.pieceAt(newRow, newCol);
                    if (tempPiece == null) legalMoves.add(newRow * 8 + newCol);
                    else if (tempPiece.getPlayerType() != getPlayerType())
                        legalMoves.add(newRow * 8 + newCol);
                }
            }
        return legalMoves;
    }
}
