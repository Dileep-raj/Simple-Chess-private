package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;
import java.util.Set;

public class King extends Piece {
    private boolean castled, longCastled, shortCastled;

    public King(Player player, int row, int col, int resID) {
        super(player, row, col, Rank.KING, resID);
        moved = false;
        castled = shortCastled = longCastled = false;
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
//        return Math.abs(row - getRow()) <= 1 && Math.abs(col - getCol()) <= 1;
        return getLegalMoves(boardInterface).contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        if (capturingPiece.isKing()) return false;
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public Set<Integer> getLegalMoves(BoardInterface boardInterface) {
        HashSet<Integer> legalMoves = new HashSet<>();
        int row = getRow(), col = getCol(), i, j, newRow, newCol;
        for (i = -1; i <= 1; i++)
            for (j = -1; j <= 1; j++) {
                newRow = row + i;
                newCol = col + j;
                if (newRow == row && newCol == col || newCol < 0 || newCol > 7 || newRow < 0 || newRow > 7)
                    continue;
                addMove(legalMoves, boardInterface.pieceAt(newRow, newCol), newRow, newCol);
            }
        if (canShortCastle(boardInterface)) legalMoves.add(getRow() * 8 + getCol() + 2);
        if (canLongCastle(boardInterface)) legalMoves.add(getRow() * 8 + getCol() - 2);
        return legalMoves;
    }

    public boolean canShortCastle(BoardInterface boardInterface) {
        if (hasNotMoved() && !castled) {
            for (int i = getCol() + 1; i < 7; i++)
                if (boardInterface.pieceAt(getRow(), i) != null) return false;
            Piece rook = boardInterface.pieceAt(getRow(), 7);
            if (rook != null) if (rook.getRank() == Rank.ROOK) return rook.hasNotMoved();
        }
        return false;
    }

    public boolean canLongCastle(BoardInterface boardInterface) {
        if (hasNotMoved() && !castled) {
            for (int i = getCol() - 1; i > 0; i--)
                if (boardInterface.pieceAt(getRow(), i) != null) return false;
            Piece rook = boardInterface.pieceAt(getRow(), 0);
            if (rook != null) if (rook.getRank() == Rank.ROOK) return rook.hasNotMoved();
        }
        return false;
    }

    public void longCastle(BoardInterface boardInterface) {
        Piece rook = boardInterface.pieceAt(getRow(), 0);
        rook.moveTo(getRow(), 3);
        this.moveTo(getRow(), getCol() - 2);
        castled = longCastled = true;
    }

    public void shortCastle(BoardInterface boardInterface) {
        Piece rook = boardInterface.pieceAt(getRow(), 7);
        rook.moveTo(getRow(), 5);
        this.moveTo(getRow(), getCol() + 2);
        castled = shortCastled = true;
    }

    public boolean isCastled() {
        return castled;
    }

    public boolean isNotShortCastled() {
        return !shortCastled && !castled;
    }

    public boolean isNotLongCastled() {
        return !longCastled && !castled;
    }

    public boolean isChecked(BoardInterface boardInterface) {
        HashSet<Piece> pieces = boardInterface.getBoardModel().pieces;
        for (Piece piece : pieces)
            if (piece.getPlayer() != getPlayer())
                if (piece.canCapture(boardInterface, this)) return true;
        return false;
    }
}