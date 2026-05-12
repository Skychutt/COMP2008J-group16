package com.monopolydeal.logic;

import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;

/**
 * The central game logic controller.
 * Orchestrates the game flow, turn management, and interaction between all other logic components.
 */
public class GameLogic {

    private final GameManager gameManager;
    private final TurnManager turnManager;
    private final RentCalculator rentCalculator;
    private final AssetTransferManager assetTransferManager;
    private final ActionHandler actionHandler;
    private final RuleValidator ruleValidator;
    private final VictoryChecker victoryChecker;

    /**
     * Constructor for the main game logic controller.
     * Initializes all sub-components.
     * @param gameManager the main game manager instance
     */
    public GameLogic(GameManager gameManager) {
        this.gameManager = gameManager;
        this.turnManager = new TurnManager();
        this.rentCalculator = new RentCalculator();
        this.assetTransferManager = new AssetTransferManager();
        this.actionHandler = new ActionHandler(this);
        this.ruleValidator = new RuleValidator();
        this.victoryChecker = new VictoryChecker();
    }

    /**
     * Starts a new game loop.
     */
    public void startGame() {}

    /**
     * Processes the current player's turn step-by-step.
     */
    public void processTurn() {}

    /**
     * Handles the execution of a played card.
     * @param player the player playing the card
     * @param card the card being played
     */
    public void playCard(Player player, Card card) {}

    /**
     * Handles the rent collection process initiated by a player.
     * @param collector the player collecting rent
     * @param target the player being charged rent
     * @param set the property set the rent is being collected from
     * @param isDoubleRent whether the rent is doubled
     */
    public void collectRent(Player collector, Player target, PropertySet set, boolean isDoubleRent) {}

    /**
     * Checks if the game has ended and a winner is determined.
     * @return true if a player has won
     */
    public boolean checkGameOver() {
        return victoryChecker.checkWinner(gameManager.getCurrentPlayer());
    }

    /**
     * Ends the current player's turn and passes to the next player.
     */
    public void endTurn() {}

    // Getters for sub-managers to allow access from handlers
    public TurnManager getTurnManager() { return turnManager; }
    public RentCalculator getRentCalculator() { return rentCalculator; }
    public AssetTransferManager getAssetTransferManager() { return assetTransferManager; }
    public ActionHandler getActionHandler() { return actionHandler; }
    public RuleValidator getRuleValidator() { return ruleValidator; }
    public VictoryChecker getVictoryChecker() { return victoryChecker; }
}