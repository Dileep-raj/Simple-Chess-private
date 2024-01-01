package com.drdedd.simplechess_temp.pieces;

import com.drdedd.simplechess_temp.BoardInterface;
import com.drdedd.simplechess_temp.GameData.Player;
import com.drdedd.simplechess_temp.GameData.Rank;

import java.util.HashSet;

public class Queen extends Piece {
    public Queen(Player player, int row, int col, int resID) {
        super(player, row, col, Rank.QUEEN, resID);
    }

    @Override
    public boolean canMoveTo(BoardInterface boardInterface, int row, int col) {
        HashSet<Integer> possibleMoves = getPossibleMoves(boardInterface);
        return possibleMoves.contains(row * 8 + col);
    }

    @Override
    public boolean canCapture(BoardInterface boardInterface, Piece capturingPiece) {
        return canMoveTo(boardInterface, capturingPiece.getRow(), capturingPiece.getCol());
    }

    @Override
    public HashSet<Integer> getPossibleMoves(BoardInterface boardInterface) {
        HashSet<Integer> possibleMoves = new HashSet<>();
        int i, j;
//        Column top
        for (i = getRow() + 1, j = getCol(); i < 8; i++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Column bottom
        for (i = getRow() - 1, j = getCol(); i >= 0; i--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Row right
        for (i = getRow(), j = getCol() + 1; j < 8; j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Row left
        for (i = getRow(), j = getCol() - 1; j >= 0; j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Top right diagonal
        for (i = getRow() + 1, j = getCol() + 1; i < 8 && j < 8; i++, j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Bottom left diagonal
        for (i = getRow() - 1, j = getCol() - 1; i >= 0 && j >= 0; i--, j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Bottom right diagonal
        for (i = getRow() - 1, j = getCol() + 1; i >= 0 && j < 8; i--, j++)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;

//        Top left diagonal
        for (i = getRow() + 1, j = getCol() - 1; i < 8 && j >= 0; i++, j--)
            if (!addMove(possibleMoves, boardInterface.pieceAt(i, j), i, j)) break;
        return possibleMoves;
    }
}