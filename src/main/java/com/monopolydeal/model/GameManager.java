package com.monopolydeal.model;

import com.monopolydeal.interfaces.IGameObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that manages the overall game state and flow.
 * Responsible for initializing the game, managing turn progression,
 * checking victory conditions, and notifying observers of game events.
 * Uses the Singleton pattern to ensure only one game manager exists.
 */
public class GameManager {
    /** Singleton instance of the GameManager. */
    private static GameManager instance;

    private List<Player> players;              // List of all players in the game
    private int turn;                          // Index of the current player's turn
    private List<IGameObserver> observers;     // Registered observers for game events

    /** Private constructor - initializes empty player list and observer list. */
    private GameManager() {
        players = new ArrayList<>();
        observers = new ArrayList<>();
        turn = 0;
    }

    /**
     * Get the singleton GameManager instance. Creates a new one if it doesn't exist.
     * @return the singleton GameManager instance
     */
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    /** Reset the singleton instance (used for starting a new game). */
    public static void reset() {
        instance = null;
    }

    /**
     * Initialize a new game with the specified number of players.
     * Shuffles the deck and deals 5 cards to each player.
     * @param count the number of players (2-5)
     */
    public void initGame(int count) {
        players.clear();
        for (int i = 0; i < count; i++) {
            players.add(new Player(String.valueOf(i + 1), "Player " + (i + 1)));
        }
        Deck deck = Deck.getInstance();
        deck.shuffle();
        // Deal 5 cards to each player at game start
        for (Player p : players) {
            List<com.monopolydeal.model.card.Card> cards = deck.draw(5);
            for (com.monopolydeal.model.card.Card c : cards) {
                p.getHand().add(c);
            }
        }
        turn = 0;
        notifyAllObservers("Game initialized with " + count + " players.");
    }

    /**
     * Advance to the next player's turn.
     * Resets the action count to 3 and triggers the draw phase
     * (2 cards normally, 5 if the hand is empty).
     */
    public void nextTurn() {
        turn = (turn + 1) % players.size();
        Player current = getCurrentPlayer();
        current.setActions(3);
        current.draw();
        notifyAllObservers("Turn " + turn + ": " + current.getName() + "'s turn.");
    }

    /**
     * Check if any player has met the victory condition (3 complete property sets).
     * If a winner is found, all observers are notified.
     * @return true if the game has been won
     */
    public boolean checkWin() {
        for (Player p : players) {
            if (p.getPropertyArea().countCompleteSets() >= 3) {
                notifyAllObservers(p.getName() + " wins the game!");
                return true;
            }
        }
        return false;
    }

    /** @return the player whose turn it currently is */
    public Player getCurrentPlayer() {
        return players.get(turn);
    }

    /** @return the list of all players */
    public List<Player> getPlayers() {
        return players;
    }

    /** @return the current turn index */
    public int getTurn() {
        return turn;
    }

    /** Register an observer to receive game event notifications. */
    public void attach(IGameObserver o) {
        observers.add(o);
    }

    /** Remove a previously registered observer. */
    public void detach(IGameObserver o) {
        observers.remove(o);
    }

    /**
     * Notify all registered observers about a game event.
     * @param event description of the game event
     */
    public void notifyAllObservers(String event) {
        for (IGameObserver o : observers) {
            o.onGameUpdate(event);
        }
    }
}
