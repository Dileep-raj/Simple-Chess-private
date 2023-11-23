package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

public class Pawn extends Piece {
    public final int direction, lastRank;

    public Pawn(Player player, int row, int col, int resID) {
        super(player, row, col, Rank.PAWN, resID);
        direction = this.isWhite() ? 1 : -1;

        if (player == Player.BLACK) lastRank = 0;
        else lastRank = 7;

        moved = false;
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, @NonNull Piece capturingPiece) {
        if (capturingPiece.getRank() == Rank.KING) return false;
        if (Math.abs(getCol() - capturingPiece.getCol()) == 1 && (capturingPiece.getRow() - getRow()) * direction == 1) {
            moved = true;
            return true;
        }
        return false;
    }

    @Override
    public HashSet<Integer> getLegalMoves(BoardInterface boardInterface) {
        HashSet<Integer> legalMoves = new HashSet<>();
        int col = getCol(), row = getRow(), i;
        if (boardInterface.pieceAt(row + direction, col) == null)
            legalMoves.add((row + direction) * 8 + col);
        if (!moved && boardInterface.pieceAt(row + 2 * direction, col) == null && boardInterface.pieceAt(row + direction, col) == null) {
            legalMoves.add((row + 2 * direction) * 8 + col);
        }
        for (i = -1; i <= 1; i += 2) {
            Piece tempPiece = boardInterface.pieceAt(row + direction, col + i);
            if (tempPiece != null) if (tempPiece.getPlayerType() != getPlayerType())
                legalMoves.add((row + direction) * 8 + col + i);
        }
        return legalMoves;
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        if (getCol() == col)
            if ((row - getRow()) * direction == 1 || (!moved && (row - getRow()) * direction == 2 && boardInterface.pieceAt(row - direction, col) == null))
                return moved = true;
        return false;
    }

    public boolean canPromote() {
        return row + direction == lastRank;
    }
}