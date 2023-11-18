package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

public class Bishop extends Piece {
    public Bishop(Player player, int row, int col,  int resID) {
        super(player, row, col,  Rank.BISHOP, resID);
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        HashSet<Integer> legalMoves = getLegalMoves(boardInterface);
        return legalMoves.contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public HashSet<Integer> getLegalMoves(BoardInterface boardInterface) {
        HashSet<Integer> legalMoves = new HashSet<>();
        int i, j;
        Piece tempPiece;
//        Top right diagonal
        for (i = getRow() + 1, j = getCol() + 1; i < 8 && j < 8; i++, j++) {
            tempPiece = boardInterface.pieceAt(i, j);
            if (tempPiece == null) {
                legalMoves.add(i * 8 + j);
            } else if (tempPiece.getPlayerType() != getPlayerType()) {
                legalMoves.add(i * 8 + j);
                break;
            } else if (tempPiece.getPlayerType() == getPlayerType()) break;
        }
//        Bottom left diagonal
        for (i = getRow() - 1, j = getCol() - 1; i >= 0 && j >= 0; i--, j--) {
            tempPiece = boardInterface.pieceAt(i, j);
            if (tempPiece == null) {
                legalMoves.add(i * 8 + j);
            } else if (tempPiece.getPlayerType() != getPlayerType()) {
                legalMoves.add(i * 8 + j);
                break;
            } else if (tempPiece.getPlayerType() == getPlayerType()) break;
        }
//        Bottom right diagonal
        for (i = getRow() - 1, j = getCol() + 1; i >= 0 && j < 8; i--, j++) {
            tempPiece = boardInterface.pieceAt(i, j);
            if (tempPiece == null) {
                legalMoves.add(i * 8 + j);
            } else if (tempPiece.getPlayerType() != getPlayerType()) {
                legalMoves.add(i * 8 + j);
                break;
            } else if (tempPiece.getPlayerType() == getPlayerType()) break;
        }
//        Top left diagonal
        for (i = getRow() + 1, j = getCol() - 1; i < 8 && j >= 0; i++, j--) {
            tempPiece = boardInterface.pieceAt(i, j);
            if (tempPiece == null) {
                legalMoves.add(i * 8 + j);
            } else if (tempPiece.getPlayerType() != getPlayerType()) {
                legalMoves.add(i * 8 + j);
                break;
            } else if (tempPiece.getPlayerType() == getPlayerType()) break;
        }
        return legalMoves;
    }

    @Override
    public boolean hasMoved() {
        return moved;
    }
}
