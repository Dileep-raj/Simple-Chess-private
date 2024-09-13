package com.drdedd.simplichess.interfaces;

public interface GameFragmentInterface {
    void updateViews();

    void terminateGame(String termination);

    boolean saveProgress();
}
