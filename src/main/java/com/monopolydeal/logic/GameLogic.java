package com.monopolydeal.logic;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

import com.monopolydeal.enums.ActionType;

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
     * Starts a new game loop: begins the first turn (draw + actions) and syncs the turn manager.
     */
    public void startGame() {
        gameManager.beginCurrentPlayerTurn();
        Player current = gameManager.getCurrentPlayer();
        turnManager.startTurn(current);
        gameManager.notifyAllObservers("Game started - " + current.getName()
                + "'s turn | Hand: " + current.getHand().size()
                + " cards | Actions: " + current.getActions());
    }

    /**
     * Processes the current player's turn step-by-step (console-friendly status).
     */
    public void processTurn() {
        Player p = gameManager.getCurrentPlayer();
        System.out.println("[Turn] " + p.getName()
                + " | Hand: " + p.getHand().size()
                + " | Bank: " + p.getBankArea().total() + "M"
                + " | Complete sets: " + p.getPropertyArea().countCompleteSets()
                + " | Actions left: " + p.getActions());
    }

    /**
     * Handles the execution of a played card.
     * @param player the player playing the card
     * @param card the card being played
     */
    public void playCard(Player player, Card card) {
        if (gameManager.isGameOver() || player == null || card == null) {
            return;
        }
        if (!player.equals(gameManager.getCurrentPlayer())) {
            gameManager.notifyAllObservers("Only the active player may play a card.");
            return;
        }
        if (!ruleValidator.canPlayCard(player, card)) {
            gameManager.notifyAllObservers("Illegal play for " + player.getName() + ".");
            return;
        }

        if (card instanceof PropertyCard) {
            player.placeProperty(card.getId());
        } else if (card instanceof MoneyCard) {
            player.putMoneyInBank(card.getId());
        } else if (card instanceof ActionCard) {
            playActionCard(player, (ActionCard) card);
        } else {
            player.putMoneyInBank(card.getId());
        }

        if (checkGameOver()) {
            return;
        }
    }

    private void playActionCard(Player player, ActionCard card) {
        if (player.getHand().findCard(card.getId()) == null) {
            return;
        }
        if (card.getType() == ActionType.HOUSE || card.getType() == ActionType.HOTEL) {
            player.getHand().removeCard(card.getId());
            player.setActions(player.getActions() - 1);
            if (!actionHandler.tryAttachBuilding(player, card)) {
                player.getHand().add(card);
                player.setActions(player.getActions() + 1);
            }
            return;
        }

        player.getHand().removeCard(card.getId());
        player.setActions(player.getActions() - 1);
        actionHandler.executeAction(player, card);

        Deck.getInstance().addToDiscard(card);
    }

    /**
     * Handles the rent collection process initiated by a player.
     * @param collector the player collecting rent
     * @param target the player being charged rent
     * @param set the property set the rent is being collected from
     * @param isDoubleRent whether the rent is doubled
     */
    public void collectRent(Player collector, Player target, PropertySet set, boolean isDoubleRent) {
        if (!RentCalculator.canCollectRent(set)) {
            gameManager.notifyAllObservers("Rent: set is not complete.");
            return;
        }
        int amount = RentCalculator.calculateFinalRent(set, isDoubleRent);
        AssetTransferManager.PaymentResult result = assetTransferManager.processPayment(
                target, collector, amount, AssetTransferManager.PaymentMode.USE_MIXED);
        gameManager.notifyAllObservers(result.getMessage());
        checkGameOver();
    }

    /**
     * Checks if the game has ended and a winner is determined.
     * @return true if a player has won
     */
    public boolean checkGameOver() {
        for (Player p : gameManager.getPlayers()) {
            if (victoryChecker.checkWinner(p)) {
                gameManager.markWinner(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Ends the current player's turn and passes to the next player.
     */
    public void endTurn() {
        gameManager.nextTurn();
        turnManager.startTurn(gameManager.getCurrentPlayer());
    }

    public GameManager getGameManager() { return gameManager; }
    public TurnManager getTurnManager() { return turnManager; }
    public RentCalculator getRentCalculator() { return rentCalculator; }
    public AssetTransferManager getAssetTransferManager() { return assetTransferManager; }
    public ActionHandler getActionHandler() { return actionHandler; }
    public RuleValidator getRuleValidator() { return ruleValidator; }
    public VictoryChecker getVictoryChecker() { return victoryChecker; }
}
