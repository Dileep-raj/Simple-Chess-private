package com.drdedd.simplechess_temp.pieces;

import androidx.annotation.NonNull;

import com.drdedd.simplechess_temp.interfaces.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

/**
 * {@inheritDoc}
 */
public class Pawn extends Piece {
    public final int direction, lastRank;

    /**
     * Creates a new <code>Pawn</code> piece
     *
     * @param player Player type (<code>WHITE|BLACK</code>)
     * @param row    Row number of the piece
     * @param col    Column number of the piece
     * @param resID  Resource ID of the piece
     */
    public Pawn(Player player, int row, int col, int resID, String unicode) {
        super(player, row, col, Rank.PAWN, resID, unicode);
        direction = isWhite() ? 1 : -1;
        lastRank = isWhite() ? 7 : 0;
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

    public boolean canCaptureEnPassant(BoardInterface boardInterface) {
        Pawn enPassantPawn = boardInterface.getBoardModel().enPassantPawn;
        if (enPassantPawn != null) if (enPassantPawn.getPlayer() != getPlayer())
            return enPassantPawn.getRow() == getRow() && Math.abs(getCol() - enPassantPawn.getCol()) == 1;
        return false;
    }

    @Override
    public HashSet<Integer> getPossibleMoves(BoardInterface boardInterface) {
        HashSet<Integer> possibleMoves = new HashSet<>();
        int col = getCol(), row = getRow(), i;
        if (boardInterface.pieceAt(row + direction, col) == null)
            possibleMoves.add((row + direction) * 8 + col);
        if (!moved && boardInterface.pieceAt(row + 2 * direction, col) == null && boardInterface.pieceAt(row + direction, col) == null) {
            possibleMoves.add((row + 2 * direction) * 8 + col);
        }
        for (i = -1; i <= 1; i += 2) {
            Piece tempPiece = boardInterface.pieceAt(row + direction, col + i);
            if (tempPiece != null) if (tempPiece.getPlayer() != getPlayer())
                possibleMoves.add((row + direction) * 8 + col + i);
        }
        if (canCaptureEnPassant(boardInterface))
            possibleMoves.add(boardInterface.getBoardModel().enPassantPawn.getCol() + (boardInterface.getBoardModel().enPassantPawn.getRow() + direction) * 8);
        return possibleMoves;
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        if (Math.abs(col - getCol()) == 1) return canCaptureEnPassant(boardInterface);
        if (getCol() == col)
            if ((row - getRow()) * direction == 1 || (!moved && (row - getRow()) * direction == 2 && boardInterface.pieceAt(row - direction, col) == null))
                return moved = true;
        return false;
    }

    public boolean canPromote() {
        return getRow() == (lastRank - direction);
    }
}