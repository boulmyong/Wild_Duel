package com.wildduel;

public enum GameState {
    LOBBY,      // Waiting for players, game not set
    PREPARING,  // Game set, waiting to start
    FARMING,    // Farming phase
    BATTLE      // Battle phase
}
