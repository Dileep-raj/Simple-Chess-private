package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.BoardModel;
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
        if (Math.abs(getCol() - capturingPiece.getCol()) == 1 && (capturingPiece.getRow() - getRow()) * direction == 1) {
            moved = true;
            return true;
        }
        return false;
    }

    public boolean canCaptureEnPassant() {
        Pawn enPassantPawn = BoardModel.enPassantPawn;
        if (enPassantPawn != null)
            return enPassantPawn.getRow() == getRow() && Math.abs(getCol() - enPassantPawn.getCol()) == 1;
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
            if (tempPiece != null) if (tempPiece.getPlayer() != getPlayer())
                legalMoves.add((row + direction) * 8 + col + i);
        }
        if (canCaptureEnPassant())
            legalMoves.add(BoardModel.enPassantPawn.getCol() + (BoardModel.enPassantPawn.getRow() + direction) * 8);
        return legalMoves;
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        if (Math.abs(col - getCol()) == 1) return canCaptureEnPassant();
        if (getCol() == col)
            if ((row - getRow()) * direction == 1 || (!moved && (row - getRow()) * direction == 2 && boardInterface.pieceAt(row - direction, col) == null))
                return moved = true;
        return false;
    }

    public boolean canPromote() {
        return getRow() == lastRank;
    }
}