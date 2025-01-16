package com.drdedd.simplichess.interfaces;

public interface GameUI {
    void updateViews();

    void terminateGame(String termination);

    boolean saveProgress();
}