package com.monopolydeal.interfaces;

/**
 * Observer interface for the Observer design pattern.
 * Implementing classes will receive notifications when game state changes occur.
 */
public interface IGameObserver {

    /**
     * Called when a game event occurs.
     * @param event a description of the game event
     */
    void onGameUpdate(String event);
}
