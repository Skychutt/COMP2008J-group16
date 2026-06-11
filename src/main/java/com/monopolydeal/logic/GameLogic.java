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
import com.monopolydeal.enums.PropertyType;

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
    private int pendingDoubleRentCount;

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
        this.ruleValidator = new RuleValidator(gameManager);
        this.victoryChecker = new VictoryChecker();
        this.pendingDoubleRentCount = 0;
    }

    /** Add one pending Double The Rent effect for the current turn. */
    public void addPendingDoubleRent() {
        pendingDoubleRentCount++;
    }

    /** @return how many Double The Rent cards are pending this turn */
    public int getPendingDoubleRentCount() {
        return pendingDoubleRentCount;
    }

    /**
     * Consume pending Double The Rent effects and return the rent multiplier.
     * 0 pending -> x1, 1 pending -> x2, 2 pending -> x4.
     */
    public int consumeRentMultiplier() {
        // Consume once so the effect only applies to the next rent event.
        int multiplier = 1;
        for (int i = 0; i < pendingDoubleRentCount; i++) {
            multiplier *= 2;
        }
        pendingDoubleRentCount = 0;
        return multiplier;
    }

    /** Clear any unused pending Double The Rent effects (end-of-turn cleanup). */
    public void clearPendingDoubleRent() {
        pendingDoubleRentCount = 0;
    }

    /** @return how many cards the player still needs to discard to reach the hand limit */
    public int getRequiredDiscardCount(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, player.getHand().size() - Player.MAX_HAND_SIZE);
    }

    /**
     * Discard one card from the given player's hand into the discard pile.
     * @return true if the card was removed from hand and sent to discard
     */
    public boolean discardCard(Player player, int cardId) {
        if (gameManager.isGameOver() || player == null) {
            return false;
        }

        Card discard = player.getHand().removeCard(cardId);
        if (discard == null) {
            return false;
        }

        Deck.getInstance().addToDiscard(discard);
        return true;
    }

    /**
     * Starts a new game loop: begins the first turn (draw + actions) and syncs the turn manager.
     */
    public void startGame() {
        clearPendingDoubleRent();
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
        String reason = ruleValidator.explainPlayCardFailure(player, card);
        if (reason != null) {
            gameManager.notifyAllObservers(reason);
            return;
        }

        if (card instanceof PropertyCard) {
            if (player.placeProperty(card.getId())) {
                checkGameOver();
            }
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

    /**
     * Places a property card, optionally with a locked color choice for two-color wilds.
     */
    public void placeProperty(Player player, PropertyCard card, PropertyType chosenColor) {
        if (gameManager.isGameOver() || player == null || card == null) {
            return;
        }
        if (!player.equals(gameManager.getCurrentPlayer())) {
            gameManager.notifyAllObservers("Only the active player may play a card.");
            return;
        }
        String reason = ruleValidator.explainPlayCardFailure(player, card);
        if (reason != null) {
            gameManager.notifyAllObservers(reason);
            return;
        }
        if (player.placeProperty(card.getId(), chosenColor)) {
            checkGameOver();
        }
    }

    private void playActionCard(Player player, ActionCard card) {
        int originalIndex = player.getHand().getCards().indexOf(card);
        if (originalIndex < 0) {
            return;
        }
        if (card.getType() == ActionType.HOUSE || card.getType() == ActionType.HOTEL) {
            player.getHand().removeCard(card.getId());
            player.setActions(player.getActions() - 1);
            if (!actionHandler.tryAttachBuilding(player, card)) {
                player.getHand().insertAt(originalIndex, card);
                player.setActions(player.getActions() + 1);
            }
            return;
        }

        player.getHand().removeCard(card.getId());
        player.setActions(player.getActions() - 1);
        boolean resolved = actionHandler.executeAction(player, card);
        if (!resolved) {
            restorePlayedCard(player, card, originalIndex);
            return;
        }

        Deck.getInstance().addToDiscard(card);
    }

    /** Put a cancelled card back into the hand at its original slot. */
    private void restorePlayedCard(Player player, Card card, int originalIndex) {
        player.getHand().insertAt(originalIndex, card);
        player.setActions(player.getActions() + 1);
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
            gameManager.notifyAllObservers("Rent: no rent can be charged on that property set.");
            return;
        }
        int amount = RentCalculator.calculateFinalRent(set, isDoubleRent);
        if (amount <= 0) {
            gameManager.notifyAllObservers("Rent: amount is 0M for the chosen set.");
            return;
        }
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
        Player current = gameManager.getCurrentPlayer();
        actionHandler.enforceEndTurnDiscard(current);
        clearPendingDoubleRent();
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
