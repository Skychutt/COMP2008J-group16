package com.monopolydeal.gui;

import com.monopolydeal.ai.AIPlayerBrain;
import com.monopolydeal.ai.AITurnExecutor;
import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Main local game window.
 */
public class GameFrame implements IGameObserver {

    private final Stage stage;
    private final GameManager gameManager;
    private final GameLogic gameLogic;
    private final Runnable homeCallback;

    private final CardImageResolver imageResolver;
    private final TopStatusPanel topStatusPanel;
    private final PropertyAreaPanel propertyPanel;
    private final PlayerPanel playerPanel;
    private final GameBoardPane board;
    private final ControlPanel controlPanel;
    private final RecentLogPanel recentLogPanel;

    private boolean returnedHome = false;
    private String latestEvent = "Welcome to Monopoly Deal.";
    private boolean discardMode = false;
    private int discardRemaining = 0;
    private Player propertyPreviewPlayer;
    private final boolean vsAiMode;
    private final AIPlayerBrain aiBrain;
    private AITurnExecutor aiExecutor;

    public GameFrame(GameManager gameManager, GameLogic gameLogic,
                     Stage stage, Runnable homeCallback) {
        this(gameManager, gameLogic, stage, homeCallback, false, null);
    }

    public GameFrame(GameManager gameManager, GameLogic gameLogic,
                     Stage stage, Runnable homeCallback,
                     boolean vsAiMode, AIPlayerBrain aiBrain) {
        this.gameManager = gameManager;
        this.gameLogic = gameLogic;
        this.stage = stage;
        this.homeCallback = homeCallback;
        this.vsAiMode = vsAiMode;
        this.aiBrain = aiBrain;
        this.imageResolver = new CardImageResolver();

        if (vsAiMode && aiBrain != null) {
            gameLogic.getActionHandler().setDecisionResolver(aiBrain);
            aiExecutor = new AITurnExecutor(
                    aiBrain,
                    gameLogic,
                    gameLogic.getActionHandler(),
                    this::refreshUI
            );
        }

        topStatusPanel = new TopStatusPanel();
        propertyPanel = new PropertyAreaPanel(this);
        playerPanel = new PlayerPanel(this);
        controlPanel = new ControlPanel(this);
        recentLogPanel = new RecentLogPanel();

        topStatusPanel.setCardDropHandler(this::handleCenterCardDrop);
        topStatusPanel.setCardDropValidator(this::canDropCardInCenter);
        playerPanel.setBankDropHandler(this::bankCardById);
        playerPanel.setPropertyDropHandler(this::placePropertyById);
        playerPanel.setEndTurnHandler(this::endCurrentTurn);

        board = new GameBoardPane(this, topStatusPanel, propertyPanel, playerPanel, controlPanel, recentLogPanel);

        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1440, Math.max(1100, screen.getWidth() - 40));
        double height = Math.min(960, Math.max(700, screen.getHeight() - 60));

        Scene scene = new Scene(board, width, height);
        stage.setTitle(vsAiMode ? "Monopoly Deal - vs AI" : "Monopoly Deal");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.setOnCloseRequest(e -> returnToHomeScreen());

        gameLogic.getActionHandler().setDialogOwner(stage);
        gameManager.attach(this);
        refreshUI();
    }

    public void show() {
        stage.show();
    }

    @Override
    public void onGameUpdate(String event) {
        latestEvent = event == null ? latestEvent : event;
        recentLogPanel.logEvent(event);
        refreshUI();
    }

    public void refreshUI() {
        if (gameManager.getPlayers() == null || gameManager.getPlayers().isEmpty()) {
            return;
        }

        Player current = gameManager.getCurrentPlayer();
        Player viewPlayer = getViewPlayer();
        boolean canControl = canControlCurrentPlayer();

        topStatusPanel.updateTableCenter(current, imageResolver,
                gameManager.isGameOver(), discardMode, discardRemaining);
        playerPanel.updatePlayerView(viewPlayer, gameManager.isGameOver(),
                discardMode, discardRemaining, canControl);
        refreshPropertyPanelOnly(viewPlayer, canControl);
        board.updateOpponents(gameManager.getPlayers(), viewPlayer, imageResolver);
        controlPanel.updateSelfAssets(viewPlayer);
        board.setWinnerBanner(gameManager.isGameOver() ? findWinnerBannerText() : null);

        scheduleAiIfNeeded();
    }

    public void playCard(Card card) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || card == null || gameManager.isGameOver() || discardMode) {
            return;
        }
        if (!canControlCurrentPlayer()) {
            return;
        }

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(current, card);
        if (reason != null) {
            reportProblem("Play Failed", reason);
            return;
        }

        if (requiresTargetedDrop(card)) {
            reportProblem("Play Failed", "Drag this card onto an opponent.");
            return;
        }

        if (card instanceof PropertyCard) {
            PropertyCard propertyCard = (PropertyCard) card;
            PropertyType chosen = resolvePlacementColor(propertyCard);
            if (propertyCard.needsColorChoiceOnPlacement() && chosen == null) {
                return;
            }
            gameLogic.placeProperty(current, propertyCard, chosen);
        } else {
            gameLogic.playCard(current, card);
        }
        refreshUI();
    }

    public void playCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || discardMode || !canControlCurrentPlayer()) {
            return;
        }
        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            return;
        }
        playCard(card);
    }

    public void playCardOnTarget(Player target, int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || target == null || !canTargetOpponentWithCard(cardId, target)) {
            return;
        }

        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            return;
        }

        gameLogic.getActionHandler().setPreferredTargetPlayer(target);
        try {
            gameLogic.playCard(current, card);
        } finally {
            gameLogic.getActionHandler().clearPreferredTargetPlayer();
        }
        refreshUI();
    }

    public void placePropertyById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) {
            return;
        }

        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof PropertyCard)) {
            reportProblem("Property Failed", "Only property cards can be placed in the property area.");
            return;
        }

        PropertyCard propertyCard = (PropertyCard) card;
        PropertyType chosen = resolvePlacementColor(propertyCard);
        if (propertyCard.needsColorChoiceOnPlacement() && chosen == null) {
            return;
        }

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(current, propertyCard);
        if (reason != null) {
            reportProblem("Property Failed", reason);
            return;
        }

        gameLogic.placeProperty(current, propertyCard, chosen);
        refreshUI();
    }

    public void placePropertyByIdToColor(int cardId, PropertyType color) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || color == null || !canControlCurrentPlayer()) {
            return;
        }

        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof PropertyCard)) {
            reportProblem("Property Failed", "Only property cards can be placed in the property area.");
            return;
        }
        if (!canPlacePropertyInColor(cardId, color)) {
            reportProblem("Property Failed", "This property cannot be placed in that color area.");
            return;
        }

        gameLogic.placeProperty(current, (PropertyCard) card, color);
        refreshUI();
    }

    public void bankCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) {
            return;
        }
        if (!canBankCard(cardId)) {
            reportProblem("Bank Failed", "This card cannot be moved to bank right now.");
            return;
        }

        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            reportProblem("Bank Failed", "This card is no longer in your hand.");
            return;
        }

        if (!current.putMoneyInBank(cardId)) {
            reportProblem("Bank Failed", "This bank move is no longer legal.");
            return;
        }
        refreshUI();
    }

    public void endCurrentTurn() {
        if (gameManager.isGameOver() || discardMode) {
            return;
        }
        Player current = gameManager.getCurrentPlayer();
        if (current == null || !canControlCurrentPlayer()) {
            return;
        }

        int needDiscard = gameLogic.getRequiredDiscardCount(current);
        if (needDiscard > 0) {
            discardMode = true;
            discardRemaining = needDiscard;
            gameManager.notifyAllObservers(current.getName()
                    + " must discard " + needDiscard + " card(s) before ending the turn.");
            return;
        }

        gameLogic.endTurn();
        refreshUI();
    }

    public void handleCenterCardDrop(int cardId) {
        if (discardMode) {
            discardCardById(cardId);
            return;
        }
        playCardById(cardId);
    }

    public boolean canDropCardInCenter(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || !canControlCurrentPlayer()) {
            return false;
        }

        Card card = current.getHand().findCard(cardId);
        if (card == null) {
            return false;
        }
        if (discardMode) {
            return true;
        }
        if (requiresTargetedDrop(card)) {
            return false;
        }
        return gameLogic.getRuleValidator().canPlayCard(current, card);
    }

    public boolean canBankCard(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) {
            return false;
        }

        Card card = current.getHand().findCard(cardId);
        if (card == null || current.getActions() <= 0) {
            return false;
        }
        if (card instanceof PropertyCard) {
            return ((PropertyCard) card).canBankAsMoney();
        }
        return true;
    }

    public boolean canPlacePropertyInColor(int cardId, PropertyType color) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || color == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) {
            return false;
        }

        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof PropertyCard)) {
            return false;
        }

        PropertyCard propertyCard = (PropertyCard) card;
        if (gameLogic.getRuleValidator().explainPlayCardFailure(current, propertyCard) != null) {
            return false;
        }
        return propertyCard.getAssignableColors().contains(color);
    }

    public boolean canTargetOpponentWithCard(int cardId, Player target) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || target == null || target == current
                || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) {
            return false;
        }

        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof ActionCard)) {
            return false;
        }

        ActionCard actionCard = (ActionCard) card;
        if (gameLogic.getRuleValidator().explainPlayCardFailure(current, actionCard) != null) {
            return false;
        }

        switch (actionCard.getType()) {
            case DEBT_DEAL:
                return true;
            case SLY_DEAL:
                return hasStealableProperty(target);
            case FORCED_DEAL:
                return hasSwappableProperty(current) && hasSwappableProperty(target);
            case DEAL_BREAKER:
                return hasCompleteSet(target);
            case RENT:
            case DOUBLE_RENT:
                return isAnyRentCard(actionCard);
            default:
                return false;
        }
    }

    public GameLogic getGameLogic() {
        return gameLogic;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public CardImageResolver getImageResolver() {
        return imageResolver;
    }

    public Stage getStage() {
        return stage;
    }

    /** Ask for confirmation, then return to the main menu (local / vs-AI modes). */
    public void requestExitToHome() {
        boolean confirmed = ThemedConfirmDialog.show(
                stage,
                "Exit Game",
                "Leave this game and return to the main menu?\nYour current progress will be lost.",
                "Leave",
                "Stay"
        );
        if (confirmed) {
            returnToHomeScreen();
            stage.close();
        }
    }

    public void showPropertyPreview(Player player) {
        if (player == null || player == getViewPlayer()) {
            return;
        }
        propertyPreviewPlayer = player;
        refreshPropertyPanelOnly(getViewPlayer(), canControlCurrentPlayer());
    }

    public void clearPropertyPreview() {
        if (propertyPreviewPlayer == null) {
            return;
        }
        propertyPreviewPlayer = null;
        refreshPropertyPanelOnly(getViewPlayer(), canControlCurrentPlayer());
    }

    private void discardCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || !discardMode) {
            return;
        }

        Card discard = current.getHand().findCard(cardId);
        if (discard == null) {
            reportProblem("Discard Failed", "This card is no longer in your hand.");
            return;
        }
        if (!gameLogic.discardCard(current, cardId)) {
            reportProblem("Discard Failed", "This discard move is no longer legal.");
            return;
        }

        discardRemaining = gameLogic.getRequiredDiscardCount(current);
        String event = current.getName() + " discarded [" + discard.getName() + "].";

        if (discardRemaining > 0) {
            event += " " + discardRemaining + " card(s) left to discard.";
            gameManager.notifyAllObservers(event);
            return;
        }

        event += " Turn will end now.";
        discardMode = false;
        discardRemaining = 0;
        gameManager.notifyAllObservers(event);
        gameLogic.endTurn();
    }

    private PropertyType resolvePlacementColor(PropertyCard card) {
        Player current = gameManager.getCurrentPlayer();
        PropertyType chosen;
        if (current != null && current.isAI() && aiBrain != null) {
            chosen = aiBrain.choosePropertyColor(current, card);
        } else {
            chosen = PropertyColorChooser.prompt(stage, card);
        }
        if (card.needsColorChoiceOnPlacement() && chosen == null) {
            return null;
        }
        return chosen;
    }

    private Player getViewPlayer() {
        Player current = gameManager.getCurrentPlayer();
        if (!vsAiMode || current == null || current.isHuman()) {
            return current;
        }
        for (Player player : gameManager.getPlayers()) {
            if (player.isHuman()) {
                return player;
            }
        }
        return current;
    }

    private Player resolvePropertyOwner(Player viewPlayer) {
        if (propertyPreviewPlayer == null) {
            return viewPlayer;
        }
        for (Player player : gameManager.getPlayers()) {
            if (player == propertyPreviewPlayer) {
                return player;
            }
        }
        propertyPreviewPlayer = null;
        return viewPlayer;
    }

    private void refreshPropertyPanelOnly(Player viewPlayer, boolean canControl) {
        Player propertyOwner = resolvePropertyOwner(viewPlayer);
        propertyPanel.updatePropertyArea(propertyOwner, canControl && propertyOwner == viewPlayer, previewRentMultiplier());
        board.setPropertyPreviewName(propertyOwner == viewPlayer ? null : propertyOwner.getName());
    }

    private boolean canControlCurrentPlayer() {
        if (gameManager.isGameOver()) {
            return false;
        }
        Player current = gameManager.getCurrentPlayer();
        return current != null && current.isHuman();
    }

    private void scheduleAiIfNeeded() {
        if (!vsAiMode || aiExecutor == null || gameManager.isGameOver()) {
            return;
        }
        Player current = gameManager.getCurrentPlayer();
        if (current != null && current.isAI()) {
            aiExecutor.startTurn(current);
        }
    }

    private String findWinnerBannerText() {
        for (Player player : gameManager.getPlayers()) {
            if (player.getPropertyArea().countCompleteSets() >= 3) {
                return player.getName() + " win";
            }
        }
        return null;
    }

    private void reportProblem(String title, String message) {
        ThemedDialog.showWarning(stage, title, message);
        gameManager.notifyAllObservers(message);
    }

    private boolean requiresTargetedDrop(Card card) {
        if (!(card instanceof ActionCard)) {
            return false;
        }

        ActionCard actionCard = (ActionCard) card;
        switch (actionCard.getType()) {
            case DEBT_DEAL:
            case SLY_DEAL:
            case FORCED_DEAL:
            case DEAL_BREAKER:
                return true;
            case RENT:
            case DOUBLE_RENT:
                return isAnyRentCard(actionCard);
            default:
                return false;
        }
    }

    private boolean isAnyRentCard(ActionCard actionCard) {
        return actionCard != null
                && actionCard.getType() == ActionType.RENT
                && actionCard.getName() != null
                && actionCard.getName().contains("Any");
    }

    private boolean hasStealableProperty(Player target) {
        for (PropertySet set : target.getPropertyArea().getSets()) {
            if (!set.isComplete() && !set.getCards().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSwappableProperty(Player player) {
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (!set.isComplete() && !set.getCards().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompleteSet(Player target) {
        for (PropertySet set : target.getPropertyArea().getSets()) {
            if (set.isComplete()) {
                return true;
            }
        }
        return false;
    }

    private int previewRentMultiplier() {
        int multiplier = 1;
        for (int i = 0; i < gameLogic.getPendingDoubleRentCount(); i++) {
            multiplier *= 2;
        }
        return multiplier;
    }

    private void returnToHomeScreen() {
        if (returnedHome) {
            return;
        }
        returnedHome = true;
        if (aiExecutor != null) {
            aiExecutor.stop();
        }
        gameManager.detach(this);
        if (homeCallback != null) {
            homeCallback.run();
        }
    }
}
