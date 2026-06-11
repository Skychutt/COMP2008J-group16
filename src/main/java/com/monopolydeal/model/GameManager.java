package com.monopolydeal.model;

import com.monopolydeal.enums.PlayerType;
import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that manages the overall game state and flow.
 * Responsible for initializing the game, managing turn progression,
 * checking victory conditions, and notifying observers of game events.
 * Uses the Singleton pattern to ensure only one game manager exists.
 */
public class GameManager {
    /** Carries display name and control type for one seat. */
    public static final class PlayerSetup {
        private final String name;
        private final PlayerType type;

        public PlayerSetup(String name, PlayerType type) {
            this.name = name == null || name.trim().isEmpty() ? "Player" : name.trim();
            this.type = type == null ? PlayerType.HUMAN : type;
        }

        public String getName() {
            return name;
        }

        public PlayerType getType() {
            return type;
        }
    }

    /** Singleton instance of the GameManager. */
    private static GameManager instance;

    private List<Player> players;              // List of all players in the game
    private int turn;                          // Index of the current player's turn
    private List<IGameObserver> observers;     // Registered observers for game events
    private boolean gameOver;              // True once a winner has been found

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
        List<String> defaultNames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            defaultNames.add("Player " + (i + 1));
        }
        initGame(count, defaultNames);
    }

    /**
     * Initialize a new game with custom display names for each player.
     * @param count number of players (2-5)
     * @param displayNames one name per player in seat order
     */
    public void initGame(int count, List<String> displayNames) {
        List<PlayerSetup> setups = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = "Player " + (i + 1);
            if (displayNames != null && i < displayNames.size()) {
                String custom = displayNames.get(i);
                if (custom != null && custom.trim().length() > 0) {
                    name = custom.trim();
                }
            }
            setups.add(new PlayerSetup(name, PlayerType.HUMAN));
        }
        initGameWithSetups(setups);
    }

    /**
     * LAN client mirror: create player seats without touching {@link Deck}.
     * The authoritative deck lives only on the server; clients must not shuffle or deal locally.
     */
    public void initPlayersOnly(int count, List<String> displayNames) {
        if (count < 2 || count > 5) {
            throw new IllegalArgumentException("Monopoly Deal supports 2 to 5 players.");
        }

        players.clear();
        gameOver = false;

        for (int i = 0; i < count; i++) {
            String name = "Player " + (i + 1);
            if (displayNames != null && i < displayNames.size()) {
                String custom = displayNames.get(i);
                if (custom != null && !custom.trim().isEmpty()) {
                    name = custom.trim();
                }
            }
            Player player = new Player(String.valueOf(i + 1), name);
            player.setPlayerType(PlayerType.HUMAN);
            players.add(player);
        }
        turn = 0;
    }

    /**
     * Initialize a game with explicit human/AI seat configuration.
     * @param playerSetups one setup per seat (2-5 players)
     */
    public void initGameWithSetups(List<PlayerSetup> playerSetups) {
        if (playerSetups == null || playerSetups.size() < 2 || playerSetups.size() > 5) {
            throw new IllegalArgumentException("Monopoly Deal supports 2 to 5 players.");
        }

        players.clear();
        gameOver = false;

        for (int i = 0; i < playerSetups.size(); i++) {
            PlayerSetup setup = playerSetups.get(i);
            Player player = new Player(String.valueOf(i + 1), setup.getName());
            player.setPlayerType(setup.getType());
            players.add(player);
        }

        Deck deck = Deck.getInstance();
        deck.shuffle();
        for (Player p : players) {
            p.getHand().getCards().addAll(deck.draw(5));
        }
        turn = 0;
        notifyAllObservers("Game initialized with " + playerSetups.size() + " players.");
    }

    /** @return true when at least one seat is AI controlled */
    public boolean hasAiPlayers() {
        for (Player player : players) {
            if (player.isAI()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Begins the current player's first draw phase and action allowance (call after {@link #initGame(int)}).
     * Draws 2 cards (or 5 if hand is empty) and sets actions to 3.
     */
    public void beginCurrentPlayerTurn() {
        Player p = getCurrentPlayer();
        p.setActions(Player.ACTIONS_PER_TURN);
        p.draw();
        notifyAllObservers(p.getName() + "'s turn started. Actions: " + p.getActions());
    }

    /**
     * Advance to the next player's turn.
     * Resets the action count to 3 and triggers the draw phase
     * (2 cards normally, 5 if the hand is empty).
     */
    public void nextTurn() {
        Player current = getCurrentPlayer();
        current.finalizeTurnEnd();
        turn = (turn + 1) % players.size();
        beginCurrentPlayerTurn();
        Player next = getCurrentPlayer();
        notifyAllObservers("Now " + next.getName() + "'s turn | Hand: " + next.getHand().size()
                + " cards | Actions: " + next.getActions());
    }

    /**
     * Check if any player has met the victory condition (3 complete property sets).
     * If a winner is found, all observers are notified.
     * @return true if the game has been won
     */
    public boolean checkWin() {
        for (Player p : players) {
            if (p.getPropertyArea().countCompleteSets() >= 3) {
                markWinner(p);
                return true;
            }
        }
        return false;
    }

    /** Marks the game as finished with the given winner (victory condition: 3 complete sets). */
    public void markWinner(Player winner) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        notifyAllObservers(winner.getName() + " wins the game!");
    }

    /**
     * @deprecated Use {@link com.monopolydeal.logic.GameLogic#collectRent} instead.
     */
    @Deprecated
    public void collectRent(Player target, int rentAmount) {
        Player collector = getCurrentPlayer();
        if (target == null || collector == null || rentAmount <= 0) {
            return;
        }
        List<Card> paid = target.payAmount(rentAmount);
        if (!paid.isEmpty()) {
            collector.receivePayment(paid);
        }
        notifyAllObservers(collector.getName() + " collected rent from " + target.getName() + ".");
        checkWin();
    }

    /**
     * Print the current game state for all players.
     * Useful for debugging and the project demo.
     */
    public void printGameState() {
        System.out.println("===== Game State =====");
        System.out.println("Current turn: " + getCurrentPlayer().getName());
        for (Player p : players) {
            System.out.println(p);  // calls Player.toString()
        }
        System.out.println("Deck remaining: " + Deck.getInstance().drawPileSize() + " cards");
        System.out.println("======================");
    }

    /**
     * Updates turn/game-over flags when rebuilding client-side mirror state from a LAN snapshot.
     */
    public void applyMirrorFlags(int turnIndex, boolean over) {
        if (players != null && !players.isEmpty()) {
            this.turn = Math.floorMod(turnIndex, players.size());
        }
        this.gameOver = over;
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

    /** @return true if the game is over */
    public boolean isGameOver() {
        return gameOver;
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
