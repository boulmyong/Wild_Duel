package com.wildduel.game;

public enum GameState {
    LOBBY,      // In lobby world, waiting for setup
    COUNTDOWN,  // Countdown before starting the game
    FARMING,    // Farming phase in the game world
    BATTLE,     // Battle phase in the game world
    ENDED       // Game has ended, waiting for reset
}
