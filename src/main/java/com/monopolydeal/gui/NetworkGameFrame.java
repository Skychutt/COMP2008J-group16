package com.monopolydeal.gui;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.network.ClientGameMirror;
import com.monopolydeal.network.DecisionPayload;
import com.monopolydeal.network.GameClient;
import com.monopolydeal.network.GameStateParser;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LAN online game window — mirrors local {@link GameFrame} interactions via network messages.
 */
public class NetworkGameFrame implements GamePanelHost {

    private final Stage stage;
    private final GameClient client;
    private final Stage homeStage;
    private final Runnable homeCallback;
    private final boolean hideHomeOnConnect;
    private volatile boolean homeHiddenForGame;

    private volatile int myPlayerIndex = -1;
    private volatile boolean myTurn = false;
    private volatile boolean discardMode = false;
    private volatile int discardRemaining = 0;

    private final CardImageResolver imageResolver;
    private GameManager mirrorManager;
    private GameLogic mirrorLogic;

    private TopStatusPanel topStatusPanel;
    private PlayerPanel playerPanel;
    private ControlPanel controlPanel;
    private PropertyAreaPanel propertyPanel;
    private RecentLogPanel recentLogPanel;
    private GameBoardPane board;

    private volatile GameStateParser.Snapshot snapshot;
    private boolean gameOverShown = false;
    private boolean mirrorReady = false;
    private volatile boolean exiting = false;
    private Player propertyPreviewPlayer;

    private NetworkGameFrame(GameClient client, Stage homeStage, Runnable homeCallback, boolean hideHomeOnConnect) {
        this.client = client;
        this.homeStage = homeStage;
        this.homeCallback = homeCallback;
        this.hideHomeOnConnect = hideHomeOnConnect;
        this.imageResolver = new CardImageResolver();
        this.mirrorManager = GameManager.getInstance();
        this.mirrorLogic = new GameLogic(mirrorManager);

        stage = new Stage();
        stage.setTitle("Monopoly Deal — LAN");
        stage.setMinWidth(1100);
        stage.setMinHeight(660);
        stage.setOnCloseRequest(e -> {
            e.consume();
            requestExitToHome();
        });

        buildUI();
        wireClient();
    }

    public static void openAsClient(String host, int port, String roomCode, String playerName, Stage homeStage) {
        openAsClient(host, port, roomCode, playerName, homeStage, true);
    }

    public static void openAsClient(String host, int port, String roomCode, String playerName,
                                    Stage homeStage, boolean hideHomeOnConnect) {
        openAsClient(host, port, roomCode, playerName, homeStage, hideHomeOnConnect, null);
    }

    public static void openAsClient(String host, int port, String roomCode, String playerName,
                                    Stage homeStage, boolean hideHomeOnConnect, Runnable homeCallback) {
        GameClient client = new GameClient(host, port);
        NetworkGameFrame frame = new NetworkGameFrame(client, homeStage, homeCallback, hideHomeOnConnect);

        Thread connectThread = new Thread(() -> {
            try {
                client.connect(roomCode, playerName);
                Platform.runLater(() -> {
                    if (!frame.stage.isShowing()) {
                        frame.stage.show();
                    }
                    if (hideHomeOnConnect) {
                        frame.hideHomeIfNeeded();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ThemedDialog.showError(homeStage, "Connection Failed",
                            "Cannot join room:\n" + e.getMessage());
                    frame.returnToHomeScreen();
                });
            }
        }, "NetworkConnect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void buildUI() {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1400, Math.max(1180, screen.getWidth() - 40));
        double h = Math.min(920, Math.max(680, screen.getHeight() - 80));

        topStatusPanel = new TopStatusPanel();
        playerPanel = new PlayerPanel(this);
        controlPanel = new ControlPanel(this);
        propertyPanel = new PropertyAreaPanel(this);
        recentLogPanel = new RecentLogPanel();

        topStatusPanel.setCardDropHandler(this::handleCenterDrop);
        topStatusPanel.setCardDropValidator(this::canDropCardInCenter);
        playerPanel.setBankDropHandler(this::handleBankDrop);
        playerPanel.setPropertyDropHandler(this::handlePropertyDrop);
        playerPanel.setEndTurnHandler(this::handleEndTurn);

        board = new GameBoardPane(this, topStatusPanel, propertyPanel, playerPanel, controlPanel, recentLogPanel);
        Scene scene = new Scene(board, w, h);
        stage.setScene(scene);
        mirrorLogic.getActionHandler().setDialogOwner(stage);
        topStatusPanel.showWaitingMessage("Connecting to server...");
    }

    private void wireClient() {
        client.setDecisionListener(this::handleDecisionOnFxThread);

        client.setMessageListener(new GameClient.MessageListener() {
            @Override
            public void onConnected(int myIndex, String playerName, String roomCode) {
                myPlayerIndex = myIndex;
                Platform.runLater(() ->
                        topStatusPanel.showWaitingMessage("Connected as " + playerName
                                + " | Room " + roomCode + " — waiting for players..."));
            }

            @Override
            public void onJoinRejected(String reason) {
            }

            @Override
            public void onLobbyState(String json) {
                Platform.runLater(() -> recentLogPanel.logEvent("Lobby update received."));
            }

            @Override
            public void onGameStart(String message) {
                Platform.runLater(() -> {
                    recentLogPanel.logEvent(message);
                    hideHomeIfNeeded();
                    if (!stage.isShowing()) {
                        stage.show();
                    }
                });
            }

            @Override
            public void onGameState(String stateJson) {
                GameStateParser.Snapshot snap = GameStateParser.parse(stateJson, myPlayerIndex);
                snapshot = snap;
                if (snap != null) {
                    if (!mirrorReady && snap.players != null && !snap.players.isEmpty()) {
                        initMirrorFromSnapshot(snap);
                    }
                    myTurn = !snap.gameOver && snap.turn == myPlayerIndex;
                    syncMirror(snap);
                }
                Platform.runLater(() -> {
                    hideHomeIfNeeded();
                    if (!stage.isShowing()) {
                        stage.show();
                    }
                    refreshFromSnapshot(snap);
                });
            }

            @Override
            public void onYourTurn(String message) {
                myTurn = true;
                discardMode = false;
                Platform.runLater(() -> {
                    recentLogPanel.logEvent("** YOUR TURN — " + message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onWait(String message) {
                myTurn = false;
                Platform.runLater(() -> {
                    recentLogPanel.logEvent(message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onEvent(String event) {
                Platform.runLater(() -> {
                    recentLogPanel.logEvent(event);
                    if (event != null && event.contains("must discard") && myTurn) {
                        discardMode = true;
                        try {
                            discardRemaining = Integer.parseInt(
                                    event.replaceAll(".*must discard (\\d+).*", "$1"));
                        } catch (NumberFormatException ignored) {
                            discardRemaining = 1;
                        }
                        refreshTurnIndicator();
                    }
                });
            }

            @Override
            public void onGameOver(String winner) {
                Platform.runLater(() -> {
                    if (!gameOverShown) {
                        gameOverShown = true;
                        board.setWinnerBanner(normalizeWinnerBannerText(winner));
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {
                Platform.runLater(() -> {
                    if (!gameOverShown && !exiting) {
                        ThemedDialog.showWarning(stage, "Connection Lost", "Disconnected: " + reason);
                        returnHome();
                    }
                });
            }
        });
    }

    private int handleDecisionOnFxThread(DecisionPayload payload) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger choice = new AtomicInteger(-1);
        Platform.runLater(() -> {
            try {
                int picked = ThemedDialog.showChoice(
                        stage,
                        payload.title,
                        payload.prompt,
                        payload.options,
                        payload.allowCancel
                );
                choice.set(picked);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(120, TimeUnit.SECONDS)) {
                return -1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
        return choice.get();
    }

    private void initMirrorFromSnapshot(GameStateParser.Snapshot snap) {
        if (snap == null || snap.players == null) {
            return;
        }
        List<String> names = new java.util.ArrayList<>();
        for (GameStateParser.PlayerInfo p : snap.players) {
            names.add(p.name);
        }
        GameManager.reset();
        mirrorManager = GameManager.getInstance();
        mirrorLogic = new GameLogic(mirrorManager);
        mirrorLogic.getActionHandler().setDialogOwner(stage);
        mirrorManager.initGame(names.size(), names);
        mirrorReady = true;
        ClientGameMirror.applySnapshot(mirrorManager, snap, myPlayerIndex);
    }

    private void syncMirror(GameStateParser.Snapshot snap) {
        if (!mirrorReady || snap == null) {
            return;
        }
        ClientGameMirror.applySnapshot(mirrorManager, snap, myPlayerIndex);
    }

    private void refreshFromSnapshot(GameStateParser.Snapshot snap) {
        if (snap == null) {
            return;
        }

        if (!mirrorReady) {
        topStatusPanel.updateFromSnapshot(snap, imageResolver, myTurn, discardMode, discardRemaining);
            board.updateFromSnapshot(snap, myPlayerIndex, imageResolver, this);
            return;
        }

        Player current = mirrorManager.getCurrentPlayer();
        Player viewPlayer = getViewPlayer();
        if (current == null || viewPlayer == null) {
            return;
        }

        GameStateParser.PlayerInfo meInfo = snap.getMyInfo(myPlayerIndex);
        if (meInfo != null) {
            viewPlayer.setActions(meInfo.actions);
        }
        GameStateParser.PlayerInfo turnInfo = findPlayerInfo(snap, snap.turn);
        if (turnInfo != null && current.getActions() != turnInfo.actions) {
            current.setActions(turnInfo.actions);
        }

        topStatusPanel.updateTableCenter(current, imageResolver, snap.gameOver, discardMode, discardRemaining);
        playerPanel.updatePlayerView(viewPlayer, snap.gameOver, discardMode, discardRemaining, myTurn);
        refreshPropertyPanelOnly(viewPlayer);
        controlPanel.updateSelfAssets(viewPlayer);
        board.updateOpponents(mirrorManager.getPlayers(), viewPlayer, imageResolver);
        if (!snap.gameOver) {
            board.setWinnerBanner(null);
        }
    }

    private void refreshTurnIndicator() {
        if (snapshot == null || !mirrorReady) {
            return;
        }
        Player viewPlayer = getViewPlayer();
        if (viewPlayer != null) {
            playerPanel.updatePlayerView(viewPlayer, snapshot.gameOver, discardMode, discardRemaining, myTurn);
        }
    }

    public void handleCenterDrop(int cardId) {
        if (!myTurn) {
            return;
        }
        if (discardMode) {
            client.sendDiscard(cardId);
            return;
        }

        Card card = findMirrorCard(cardId);
        if (card == null) {
            reportProblem("Play Failed", "This card is no longer in your hand.");
            return;
        }
        if (requiresTargetedDrop(card)) {
            reportProblem("Play Failed", "Drag this card onto an opponent.");
            return;
        }
        String reason = mirrorLogic.getRuleValidator().explainPlayCardFailure(getViewPlayer(), card);
        if (reason != null) {
            reportProblem("Play Failed", reason);
            return;
        }

        if (card instanceof PropertyCard) {
            PropertyCard propertyCard = (PropertyCard) card;
            if (propertyCard.needsColorChoiceOnPlacement()) {
                PropertyType chosen = PropertyColorChooser.prompt(stage, propertyCard);
                if (chosen == null) {
                    return;
                }
                    client.sendPlaceProperty(cardId, chosen.name());
                } else {
                    client.sendPlaceProperty(cardId, null);
                }
            } else {
                client.sendPlayCard(cardId);
        }
    }

    public void handleBankDrop(int cardId) {
        if (!myTurn || discardMode || !canBankCard(cardId)) {
            return;
        }
        client.sendBankCard(cardId);
    }

    public void handlePropertyDrop(int cardId) {
        if (!myTurn || discardMode) {
            return;
        }
        Card card = findMirrorCard(cardId);
        if (!(card instanceof PropertyCard)) {
            return;
        }
        PropertyCard propertyCard = (PropertyCard) card;
        if (mirrorLogic.getRuleValidator().explainPlayCardFailure(getViewPlayer(), propertyCard) != null) {
            return;
        }
        if (propertyCard.needsColorChoiceOnPlacement()) {
            PropertyType chosen = PropertyColorChooser.prompt(stage, propertyCard);
            if (chosen == null) {
                return;
            }
            client.sendPlaceProperty(cardId, chosen.name());
        } else {
            client.sendPlaceProperty(cardId, null);
        }
    }

    public void handleEndTurn() {
        if (!myTurn || discardMode) {
            return;
        }
        client.sendEndTurn();
        myTurn = false;
        refreshTurnIndicator();
    }

    public void playCardOnTarget(int targetIndex, int cardId) {
        if (!myTurn || discardMode || !canTargetOpponentWithCard(cardId, targetIndex)) {
            return;
        }
        client.sendPlayOnTarget(cardId, targetIndex);
    }

    public boolean canDropCardInCenter(int cardId) {
        if (!myTurn || snapshot == null || snapshot.gameOver) {
            return false;
        }
        if (discardMode) {
            return findCardInHand(cardId) != null;
        }
        Card card = findMirrorCard(cardId);
        if (card == null || requiresTargetedDrop(card)) {
            return false;
        }
        return mirrorLogic.getRuleValidator().canPlayCard(getViewPlayer(), card);
    }

    @Override
    public GameLogic getGameLogic() {
        return mirrorLogic;
    }

    @Override
    public GameManager getGameManager() {
        return mirrorManager;
    }

    @Override
    public CardImageResolver getImageResolver() {
        return imageResolver;
    }

    @Override
    public Player getViewPlayer() {
        return findMirrorPlayer(myPlayerIndex);
    }

    @Override
    public void bankCardById(int cardId) {
        handleBankDrop(cardId);
    }

    @Override
    public boolean canPlacePropertyInColor(int cardId, PropertyType color) {
        if (!myTurn || discardMode || snapshot == null || snapshot.gameOver || color == null) {
            return false;
        }
        Player current = getMirrorCurrentPlayer();
        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof PropertyCard)) {
            return false;
        }
        PropertyCard propertyCard = (PropertyCard) card;
        if (mirrorLogic.getRuleValidator().explainPlayCardFailure(current, propertyCard) != null) {
            return false;
        }
        return propertyCard.getAssignableColors().contains(color);
    }

    @Override
    public void placePropertyByIdToColor(int cardId, PropertyType color) {
        if (color == null) {
            handlePropertyDrop(cardId);
            return;
        }
        client.sendPlaceProperty(cardId, color.name());
    }

    public boolean canTargetOpponentWithCard(int cardId, Player target) {
        if (target == null || mirrorManager == null || mirrorManager.getPlayers() == null) {
            return false;
        }
        return canTargetOpponentWithCard(cardId, mirrorManager.getPlayers().indexOf(target));
    }

    public void playCardOnTarget(Player target, int cardId) {
        if (target == null || mirrorManager == null || mirrorManager.getPlayers() == null) {
            return;
        }
        playCardOnTarget(mirrorManager.getPlayers().indexOf(target), cardId);
    }

    @Override
    public boolean canBankCard(int cardId) {
        if (!myTurn || discardMode || snapshot == null || snapshot.gameOver) {
            return false;
        }
        Player current = getMirrorCurrentPlayer();
        Card card = current.getHand().findCard(cardId);
        if (card == null || current.getActions() <= 0) {
            return false;
        }
        if (card instanceof PropertyCard) {
            return ((PropertyCard) card).canBankAsMoney();
        }
        return true;
    }

    public boolean canTargetOpponentWithCard(int cardId, int targetIndex) {
        if (!myTurn || discardMode || snapshot == null || snapshot.gameOver || targetIndex == myPlayerIndex) {
            return false;
        }
        Player current = getMirrorCurrentPlayer();
        Player target = findMirrorPlayer(targetIndex);
        if (target == null) {
            return false;
        }
        Card card = current.getHand().findCard(cardId);
        if (!(card instanceof ActionCard)) {
            return false;
        }
        ActionCard actionCard = (ActionCard) card;
        if (mirrorLogic.getRuleValidator().explainPlayCardFailure(current, actionCard) != null) {
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

    public void showPropertyPreview(Player player) {
        if (player == null || player == getViewPlayer()) {
            return;
        }
        propertyPreviewPlayer = player;
        refreshPropertyPanelOnly(getViewPlayer());
    }

    public void clearPropertyPreview() {
        if (propertyPreviewPlayer == null) {
            return;
        }
        propertyPreviewPlayer = null;
        refreshPropertyPanelOnly(getViewPlayer());
    }

    private void refreshPropertyPanelOnly(Player viewPlayer) {
        if (viewPlayer == null) {
            return;
        }
        Player propertyOwner = resolvePropertyOwner(viewPlayer);
        boolean canEdit = myTurn && propertyOwner == viewPlayer && !snapshot.gameOver;
        propertyPanel.updatePropertyArea(propertyOwner, canEdit, 1);
        board.setPropertyPreviewName(propertyOwner == viewPlayer ? null : propertyOwner.getName());
    }

    private Player resolvePropertyOwner(Player viewPlayer) {
        if (propertyPreviewPlayer == null || mirrorManager == null) {
            return viewPlayer;
        }
        for (Player player : mirrorManager.getPlayers()) {
            if (player == propertyPreviewPlayer) {
                return player;
            }
        }
        propertyPreviewPlayer = null;
        return viewPlayer;
    }

    public void requestExitToHome() {
        board.showConfirmDialog(
                "Exit Game",
                "Leave this game and return to the main menu?\nYour current progress will be lost.",
                "Leave",
                "Stay",
                () -> {
                    if (exiting) {
                        return;
                    }
                    exiting = true;
                    client.disconnect();
                    returnHome();
                }
        );
    }

    private Player getMirrorCurrentPlayer() {
        if (!mirrorReady) {
            return mirrorManager.getCurrentPlayer();
        }
        return mirrorManager.getCurrentPlayer();
    }

    private static GameStateParser.PlayerInfo findPlayerInfo(GameStateParser.Snapshot snap, int index) {
        if (snap == null || snap.players == null) {
            return null;
        }
        for (GameStateParser.PlayerInfo info : snap.players) {
            if (info.index == index) {
                return info;
            }
        }
        return null;
    }

    private Player findMirrorPlayer(int index) {
        if (index < 0 || index >= mirrorManager.getPlayers().size()) {
            return null;
        }
        return mirrorManager.getPlayers().get(index);
    }

    private Card findMirrorCard(int cardId) {
        Player me = getViewPlayer();
        if (me == null) {
            return null;
        }
        return me.getHand().findCard(cardId);
    }

    private GameStateParser.CardInfo findCardInHand(int cardId) {
        if (snapshot == null || snapshot.myHand == null) {
            return null;
        }
        for (GameStateParser.CardInfo c : snapshot.myHand) {
            if (c.id == cardId) {
                return c;
            }
        }
        return null;
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

    private void reportProblem(String title, String message) {
        ThemedDialog.showWarning(stage, title, message);
        recentLogPanel.logEvent(message);
    }

    private String normalizeWinnerBannerText(String winner) {
        if (winner == null) {
            return null;
        }
        String normalized = winner.trim();
        normalized = normalized.replaceFirst("\\s+wins the game!?$", "");
        normalized = normalized.replaceFirst("\\s+wins!?$", "");
        return normalized.isEmpty() ? null : normalized + " win";
    }

    private void hideHomeIfNeeded() {
        if (homeHiddenForGame || homeStage == null) {
            return;
        }
        homeStage.hide();
        homeHiddenForGame = true;
    }

    private void returnHome() {
        stage.close();
        returnToHomeScreen();
    }

    private void returnToHomeScreen() {
        if (homeCallback != null) {
            homeCallback.run();
        } else if (homeStage != null) {
            homeStage.show();
            homeStage.toFront();
        }
        homeHiddenForGame = false;
    }

    public GameClient getClient() {
        return client;
    }

    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public boolean isDiscardMode() {
        return discardMode;
    }

    public GameStateParser.Snapshot getSnapshot() {
        return snapshot;
    }

    public Stage getStage() {
        return stage;
    }
}
