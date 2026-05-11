package com.monopolydeal.model;

import com.monopolydeal.interfaces.IGameObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that manages the overall game state and flow.
 * Responsible for initializing the game, managing turn progression,
 * checking victory conditions, and notifying observers of game events.
 *
 * Design patterns used:
 *  - Singleton  : only one GameManager exists per game session
 *  - Observer   : notifies all registered IGameObserver objects on game events
 *
 * Week 10-11 completed:
 *  - initGame()     : set up players, shuffle deck, deal 5 cards each
 *  - nextTurn()     : advance turn index, reset actions to 3, call player.draw()
 *  - checkWin()     : scan all players for 3 complete property sets
 *
 * Week 11-12 stub (for the next member):
 *  - collectRent()  : calculate rent, call payer.payAmount(), call recipient.receivePayment()
 */
public class GameManager {

    /** Singleton instance of the GameManager. */
    private static GameManager instance;

    private List<Player> players;           // All players in the game (order = turn order)
    private int turn;                       // Index of the player whose turn it currently is
    private List<IGameObserver> observers;  // Registered observers for game events
    private boolean gameOver;              // True once a winner has been found

    /** Private constructor – called only by getInstance(). */
    private GameManager() {
        players = new ArrayList<>();
        observers = new ArrayList<>();
        turn = 0;
        gameOver = false;
    }

    /**
     * Get the singleton GameManager instance.
     * Creates a new one if it doesn't exist yet.
     * @return the singleton GameManager instance
     */
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    /**
     * Reset the singleton instance (used when starting a completely new game).
     * Also resets the Deck so everything is fresh.
     */
    public static void reset() {
        instance = null;
    }

    // =====================================================================
    //  Game Initialization  (Week 10-11)
    // =====================================================================

    /**
     * Initialize a new game with the specified number of players.
     * Steps:
     *  1. Create player objects (Player 1 … Player N)
     *  2. Shuffle the deck
     *  3. Deal 5 cards to each player
     *  4. Set turn index to 0 (Player 1 goes first)
     *
     * @param count the number of players (2–5)
     */
    public void initGame(int count) {
        if (count < 2 || count > 5) {
            throw new IllegalArgumentException("Monopoly Deal requires 2–5 players.");
        }

        players.clear();
        gameOver = false;
        turn = 0;

        // 1. Create players
        for (int i = 0; i < count; i++) {
            players.add(new Player(String.valueOf(i + 1), "Player " + (i + 1)));
        }

        // 2. Shuffle the deck
        Deck deck = Deck.getInstance();
        deck.shuffle();

        // 3. Deal 5 starting cards to every player
        for (Player p : players) {
            List<Card> startCards = deck.draw(5);
            for (Card c : startCards) {
                p.getHand().add(c);
            }
        }

        notifyAllObservers("Game initialized with " + count + " players. Deck has "
                + deck.drawPileSize() + " cards remaining.");
    }

    // =====================================================================
    //  Turn Management  (Week 10-11)
    // =====================================================================

    /**
     * Advance to the next player's turn.
     * Steps:
     *  1. End the current player's turn (enforce hand limit)
     *  2. Move to the next player (wrap around with %)
     *  3. Give the new current player 3 actions
     *  4. Call draw() so the player draws 2 (or 5 if hand empty)
     */
    public void nextTurn() {
        // End current player's turn (enforces 7-card hand limit)
        Player current = getCurrentPlayer();
        current.endTurn();

        // Advance to next player (wraps around: last player → Player 1 again)
        turn = (turn + 1) % players.size();

        // Set up the new current player for their turn
        Player next = getCurrentPlayer();
        next.setActions(Player.ACTIONS_PER_TURN);   // 3 actions per turn
        next.draw();                                 // draw 2 cards (or 5 if hand is empty)

        notifyAllObservers("=== " + next.getName() + "'s turn. Actions: "
                + next.getActions() + " | Hand: " + next.getHand().size() + " cards ===");
    }

    // =====================================================================
    //  Victory Check  (Week 10-11)
    // =====================================================================

    /**
     * Check if any player has met the victory condition (3 complete property sets).
     * If a winner is found, all observers are notified and gameOver is set to true.
     * @return true if the game has been won by any player
     */
    public boolean checkWin() {
        for (Player p : players) {
            if (p.getPropertyArea().countCompleteSets() >= 3) {
                gameOver = true;
                notifyAllObservers("🏆 " + p.getName() + " wins the game with 3 complete property sets!");
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  Rent Collection  (Week 11-12 stub — for next member)
    // =====================================================================

    /**
     * Collect rent from a target player on behalf of the current player.
     *
     * HOW TO COMPLETE THIS (Week 11-12):
     *  1. Look up the rent amount from PropertySet.getRent() for the chosen color.
     *  2. Call target.payAmount(rentAmount) → returns the list of cards paid.
     *  3. Call getCurrentPlayer().receivePayment(paidCards) → cards go to recipient's bank.
     *  4. Call checkWin() in case the game ends after this interaction.
     *
     * Example (uncomment and fill in when ready):
     *
     *   public void collectRent(Player target, PropertyType color) {
     *       PropertySet set = getCurrentPlayer().getPropertyArea().getSet(color);
     *       if (set == null) return;
     *       int rentAmount = set.getRent();
     *
     *       List<Card> paid = target.payAmount(rentAmount);
     *       getCurrentPlayer().receivePayment(paid);
     *
     *       notifyAllObservers(getCurrentPlayer().getName()
     *           + " collected " + rentAmount + "M rent from " + target.getName());
     *       checkWin();
     *   }
     *
     * @param target     the player who must pay rent
     * @param rentAmount the amount of rent owed
     */
    public void collectRent(Player target, int rentAmount) {
        // Week 11-12: implement rent logic here
        // Stub so the project compiles now
        Player collector = getCurrentPlayer();
        List<Card> paid = target.payAmount(rentAmount);
        collector.receivePayment(paid);
        notifyAllObservers(collector.getName() + " collected " + rentAmount
                + "M rent from " + target.getName() + ".");
        checkWin();
    }

    // =====================================================================
    //  Observer Pattern
    // =====================================================================

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
     * @param event description of the event
     */
    public void notifyAllObservers(String event) {
        for (IGameObserver o : observers) {
            o.onGameUpdate(event);
        }
    }

    // =====================================================================
    //  Getters
    // =====================================================================

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

    /** @return true if the game is over */
    public boolean isGameOver() {
        return gameOver;
    }
}
