package com.monopolydeal.gui;

import com.monopolydeal.ai.AIPlayerBrain;
import com.monopolydeal.ai.AITurnExecutor;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Main game window. Wraps a JavaFX {@link Stage} and implements {@link IGameObserver}.
 * Replaces the Swing {@code GameFrame extends JFrame}.
 */
public class GameFrame implements IGameObserver {

    private final Stage stage;
    private final GameManager gameManager;
    private final GameLogic gameLogic;
    private final Runnable homeCallback;

    private final CardImageResolver imageResolver;
    private final TopStatusPanel topStatusPanel;
    private final PlayerPanel playerPanel;
    private final GameBoardPane board;
    private final ControlPanel controlPanel;

    private boolean gameOverDialogShown = false;
    private String latestEvent = "Welcome to Monopoly Deal.";
    private boolean discardMode = false;
    private int discardRemaining = 0;
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
        this.gameManager  = gameManager;
        this.gameLogic    = gameLogic;
        this.stage        = stage;
        this.homeCallback = homeCallback;
        this.vsAiMode     = vsAiMode;
        this.aiBrain      = aiBrain;
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

        // ── Build the component tree ──
        topStatusPanel = new TopStatusPanel();
        playerPanel    = new PlayerPanel(this);
        controlPanel   = new ControlPanel(this);

        topStatusPanel.setCardDropHandler(this::handleCenterCardDrop);
        playerPanel.setBankDropHandler(this::bankCardById);
        playerPanel.setPropertyDropHandler(this::placePropertyById);
        playerPanel.setEndTurnHandler(this::endCurrentTurn);

        board = new GameBoardPane(topStatusPanel, playerPanel, controlPanel);

        // ── Stage setup ──
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width  = Math.min(1440, Math.max(1100, screen.getWidth()  - 40));
        double height = Math.min(960,  Math.max(700,  screen.getHeight() - 60));

        Scene scene = new Scene(board, width, height);
        stage.setTitle(vsAiMode ? "Monopoly Deal - vs AI" : "Monopoly Deal");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(e -> returnToHomeScreen());

        gameManager.attach(this);
        refreshUI();
    }

    public void show() {
        stage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IGameObserver
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onGameUpdate(String event) {
        latestEvent = event == null ? latestEvent : event;
        controlPanel.logEvent(event);
        refreshUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI refresh
    // ─────────────────────────────────────────────────────────────────────────

    public void refreshUI() {
        if (gameManager.getPlayers() == null || gameManager.getPlayers().isEmpty()) return;

        Player current = gameManager.getCurrentPlayer();
        Player viewPlayer = getViewPlayer();
        boolean canControl = canControlCurrentPlayer();

        topStatusPanel.updateTableCenter(current, imageResolver, latestEvent,
                gameManager.isGameOver(), discardMode, discardRemaining);
        playerPanel.updatePlayerView(viewPlayer, gameManager.isGameOver(),
                discardMode, discardRemaining, canControl);
        board.updateOpponents(gameManager.getPlayers(), current, imageResolver);
        controlPanel.updateTurnStatus(current);
        controlPanel.updateSelfAssets(viewPlayer);

        scheduleAiIfNeeded();

        if (gameManager.isGameOver() && !gameOverDialogShown) {
            gameOverDialogShown = true;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText("Game over! Check the log for the winner.");
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card actions
    // ─────────────────────────────────────────────────────────────────────────

    public void playCard(Card card) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || card == null || gameManager.isGameOver() || discardMode) return;
        if (!canControlCurrentPlayer()) return;

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(current, card);
        if (reason != null) {
            reportProblem("Play Failed", reason);
            return;
        }

        if (card instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) card;
            PropertyType chosen = resolvePlacementColor(pc);
            if (pc.needsColorChoiceOnPlacement() && chosen == null) return;
            gameLogic.placeProperty(current, pc, chosen);
        } else {
            gameLogic.playCard(current, card);
        }
        refreshUI();
    }

    public void playCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || discardMode || !canControlCurrentPlayer()) return;
        Card card = current.getHand().findCard(cardId);
        if (card == null) return;
        playCard(card);
    }

    public void placePropertyById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) return;

        Card card = current.getHand().findCard(cardId);
        if (card == null) { reportProblem("Property Failed", "This card is no longer in your hand."); return; }
        if (!(card instanceof PropertyCard)) {
            reportProblem("Property Failed", "Only property cards can be placed in the property area.");
            return;
        }

        PropertyCard pc = (PropertyCard) card;
        PropertyType chosen = resolvePlacementColor(pc);
        if (pc.needsColorChoiceOnPlacement() && chosen == null) return;

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(current, pc);
        if (reason != null) { reportProblem("Property Failed", reason); return; }

        gameLogic.placeProperty(current, pc, chosen);
        refreshUI();
    }

    public void bankCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || discardMode || !canControlCurrentPlayer()) return;

        Card card = current.getHand().findCard(cardId);
        if (card == null) { reportProblem("Bank Failed", "This card is no longer in your hand."); return; }
        if (current.getActions() <= 0) {
            reportProblem("Bank Failed", "Cannot bank [" + card.getName()
                    + "] because " + current.getName() + " has no actions left.");
            return;
        }
        if (card instanceof PropertyCard && !((PropertyCard) card).canBankAsMoney()) {
            reportProblem("Bank Failed", "Cannot bank [" + card.getName() + "] because it has no monetary value.");
            return;
        }

        boolean success = current.putMoneyInBank(cardId);
        if (success) {
            gameManager.notifyAllObservers(current.getName() + " deposited [" + card.getName() + "] to bank.");
        } else {
            reportProblem("Bank Failed", "Cannot bank [" + card.getName() + "] — move is no longer legal.");
            return;
        }
        refreshUI();
    }

    public void endCurrentTurn() {
        if (gameManager.isGameOver() || discardMode) return;
        Player current = gameManager.getCurrentPlayer();
        if (current == null || !canControlCurrentPlayer()) return;

        int needDiscard = gameLogic.getRequiredDiscardCount(current);
        if (needDiscard > 0) {
            discardMode      = true;
            discardRemaining = needDiscard;
            gameManager.notifyAllObservers(current.getName()
                    + " must discard " + needDiscard + " card(s) before ending the turn.");
            return;
        }
        gameLogic.endTurn();
        refreshUI();
    }

    public void handleCenterCardDrop(int cardId) {
        if (discardMode) discardCardById(cardId);
        else             playCardById(cardId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors (used by child panels)
    // ─────────────────────────────────────────────────────────────────────────

    public GameLogic getGameLogic()         { return gameLogic; }
    public GameManager getGameManager()     { return gameManager; }
    public CardImageResolver getImageResolver() { return imageResolver; }
    public Stage getStage()                 { return stage; }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void discardCardById(int cardId) {
        Player current = gameManager.getCurrentPlayer();
        if (current == null || gameManager.isGameOver() || !discardMode) return;

        Card discard = current.getHand().findCard(cardId);
        if (discard == null) {
            reportProblem("Discard Failed", "This card is no longer in your hand.");
            return;
        }
        if (!gameLogic.discardCard(current, cardId)) {
            reportProblem("Discard Failed",
                    "Cannot discard [" + discard.getName() + "] — move is no longer legal.");
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
        discardMode      = false;
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
            chosen = PropertyColorChooser.prompt(null, card);
        }
        if (card.needsColorChoiceOnPlacement() && chosen == null) return null;
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

    private void reportProblem(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
        gameManager.notifyAllObservers(message);
    }

    private void returnToHomeScreen() {
        if (aiExecutor != null) {
            aiExecutor.stop();
        }
        if (homeCallback != null) homeCallback.run();
    }
}
