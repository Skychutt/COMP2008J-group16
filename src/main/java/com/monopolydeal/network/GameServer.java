package com.monopolydeal.network;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LAN game server: room code lobby, authoritative game logic, remote player decisions.
 */
public class GameServer {

    public static final int DEFAULT_PORT = 12345;

    private final int port;
    private final int playerCount;
    private final String roomCode;
    private final List<String> playerNames;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService gameExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GameServerLogic");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<String, DecisionWaiter> pendingDecisions = new ConcurrentHashMap<>();

    private GameManager gameManager;
    private GameLogic gameLogic;
    private NetworkDecisionResolver decisionResolver;

    private volatile boolean gameStarted = false;
    private ServerStatusListener statusListener;

    public interface ServerStatusListener {
        void onPlayerJoined(int count, int total, String playerName);
        void onGameStarted();
        void onPlayerDisconnected(int playerIndex, String playerName);
    }

    public GameServer(int port, int playerCount, String hostName) {
        this.port = port;
        this.playerCount = playerCount;
        this.roomCode = generateRoomCode();
        this.playerNames = new ArrayList<>();
        String safeHost = hostName == null || hostName.isBlank() ? "Host" : hostName.trim();
        this.playerNames.add(safeHost);
        for (int i = 1; i < playerCount; i++) {
            this.playerNames.add("Waiting...");
        }
    }

    public void setStatusListener(ServerStatusListener listener) {
        this.statusListener = listener;
    }

    public void bind() throws IOException {
        serverSocket = new ServerSocket(port);
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("[Server] Room " + roomCode + " on " + ip + ":" + port
                + " | waiting for " + playerCount + " players");
    }

    public void acceptRemainingPlayers() throws IOException, InterruptedException {
        while (clients.size() < playerCount) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, this);
            int before = clients.size();
            handler.startReaderThread();
            long deadline = System.currentTimeMillis() + 30_000;
            while (clients.size() == before && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
        }
    }

    public void start() throws IOException, InterruptedException {
        bind();
        acceptRemainingPlayers();
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientHandler c : clients) {
            c.close();
        }
        gameExecutor.shutdownNow();
    }

    public synchronized JoinResult handleJoin(ClientHandler handler, String json) {
        String code = NetworkJson.str(json, "roomCode");
        String playerName = NetworkJson.str(json, "playerName").trim();
        if (playerName.isEmpty()) {
            return JoinResult.rejected("Player name is required.");
        }
        if (!roomCode.equals(code)) {
            return JoinResult.rejected("Invalid room code.");
        }
        if (gameStarted) {
            return JoinResult.rejected("Game already started.");
        }
        if (clients.size() >= playerCount) {
            return JoinResult.rejected("Room is full.");
        }

        int idx = clients.size();
        playerNames.set(idx, playerName);
        handler.assignPlayerIndex(idx);
        clients.add(handler);

        if (statusListener != null) {
            statusListener.onPlayerJoined(clients.size(), playerCount, playerName);
        }

        if (clients.size() >= playerCount) {
            gameExecutor.submit(this::initAndStartGame);
        }
        return JoinResult.accepted(idx, playerName, roomCode);
    }

    /**
     * Called after WELCOME is sent so the joining client never sees LOBBY_STATE before handshake completes.
     */
    public synchronized void onClientWelcomed(ClientHandler handler) {
        int idx = handler.getPlayerIndex();
        if (idx < 0) {
            return;
        }
        String playerName = playerNames.get(idx);
        broadcastLobbyState();
        broadcastEvent("Player " + (idx + 1) + " (" + playerName + ") joined ("
                + clients.size() + "/" + playerCount + ")");
    }

    private void initAndStartGame() {
        synchronized (this) {
            if (gameStarted || clients.size() < playerCount) {
                return;
            }
            gameStarted = true;
        }

        Deck.reset();
        GameManager.reset();
        gameManager = GameManager.getInstance();
        gameManager.attach(new IGameObserver() {
            @Override
            public void onGameUpdate(String event) {
                broadcastEvent(event);
            }
        });

        gameManager.initGame(playerCount, playerNames);
        gameLogic = new GameLogic(gameManager);
        decisionResolver = new NetworkDecisionResolver(this);
        gameLogic.getActionHandler().setUseDialogInput(false);
        gameLogic.getActionHandler().setUseRemoteDecisions(true);
        gameLogic.getActionHandler().setDecisionResolver(decisionResolver);

        broadcast(new NetworkMessage(NetworkMessage.GAME_START,
                "Game started with " + playerCount + " players!"));
        gameLogic.startGame();
        broadcastGameState();
        notifyWhoseTurn();

        if (statusListener != null) {
            statusListener.onGameStarted();
        }
    }

    public void submitPlayCard(int fromIdx, int cardId) {
        gameExecutor.submit(() -> requestPlayCard(fromIdx, cardId, -1));
    }

    public void submitPlayOnTarget(int fromIdx, int cardId, int targetIdx) {
        gameExecutor.submit(() -> requestPlayCard(fromIdx, cardId, targetIdx));
    }

    public void submitPlaceProperty(int fromIdx, String data) {
        gameExecutor.submit(() -> requestPlaceProperty(fromIdx, data));
    }

    public void submitBankCard(int fromIdx, int cardId) {
        gameExecutor.submit(() -> requestBankCard(fromIdx, cardId));
    }

    public void submitEndTurn(int fromIdx) {
        gameExecutor.submit(() -> requestEndTurn(fromIdx));
    }

    public void submitDiscard(int fromIdx, int cardId) {
        gameExecutor.submit(() -> requestDiscard(fromIdx, cardId));
    }

    private void requestPlayCard(int fromIdx, int cardId, int targetIdx) {
        if (!canAct(fromIdx)) {
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found in your hand (id=" + cardId + ")."));
            return;
        }

        if (targetIdx >= 0 && targetIdx < gameManager.getPlayers().size()) {
            gameLogic.getActionHandler().setPreferredTargetPlayer(gameManager.getPlayers().get(targetIdx));
        }
        gameLogic.getActionHandler().setActiveDecisionPlayer(player);
        try {
            if (card instanceof PropertyCard) {
                PropertyCard pc = (PropertyCard) card;
                gameLogic.placeProperty(player, pc, pc.getColor());
            } else {
                gameLogic.playCard(player, card);
            }
            afterAction();
        } finally {
            gameLogic.getActionHandler().clearPreferredTargetPlayer();
            gameLogic.getActionHandler().clearActiveDecisionPlayer();
        }
    }

    private void requestPlaceProperty(int fromIdx, String data) {
        if (!canAct(fromIdx)) {
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        String[] parts = data.split(",", 2);
        int cardId;
        try {
            cardId = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT, "Invalid card ID in PLACE_PROP."));
            return;
        }

        Card card = player.getHand().findCard(cardId);
        if (!(card instanceof PropertyCard)) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT, "Card is not a property card."));
            return;
        }

        PropertyCard pc = (PropertyCard) card;
        PropertyType color = pc.getColor();
        if (parts.length == 2 && !parts[1].trim().isEmpty()) {
            try {
                color = PropertyType.valueOf(parts[1].trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String reason = gameLogic.getRuleValidator().explainPlayCardFailure(player, pc);
        if (reason != null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT, reason));
            return;
        }

        gameLogic.placeProperty(player, pc, color);
        afterAction();
    }

    private void requestBankCard(int fromIdx, int cardId) {
        if (!canAct(fromIdx)) {
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found in hand (id=" + cardId + ")."));
            return;
        }

        boolean ok = player.putMoneyInBank(cardId);
        if (!ok) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Cannot bank [" + card.getName() + "]."));
            return;
        }
        gameManager.notifyAllObservers(player.getName() + " deposited [" + card.getName() + "] to bank.");
        afterAction();
    }

    private void requestEndTurn(int fromIdx) {
        if (!canAct(fromIdx)) {
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        int discardNeeded = gameLogic.getRequiredDiscardCount(player);
        if (discardNeeded > 0) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "You must discard " + discardNeeded + " card(s) first."));
            broadcastGameState();
            return;
        }

        gameLogic.endTurn();
        broadcastGameState();
        if (gameManager.isGameOver()) {
            broadcastGameOver();
        } else {
            notifyWhoseTurn();
        }
    }

    private void requestDiscard(int fromIdx, int cardId) {
        if (!gameStarted || gameManager.isGameOver()) {
            return;
        }
        if (gameManager.getTurn() != fromIdx) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT, "It's not your turn."));
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        if (!gameLogic.discardCard(player, cardId)) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Discard failed for card id=" + cardId + "."));
            return;
        }

        broadcastEvent(player.getName() + " discarded a card.");
        broadcastGameState();

        if (gameLogic.getRequiredDiscardCount(player) == 0) {
            gameLogic.endTurn();
            broadcastGameState();
            if (gameManager.isGameOver()) {
                broadcastGameOver();
            } else {
                notifyWhoseTurn();
            }
        }
    }

    public void receiveDecisionResponse(int fromIdx, String json) {
        String requestId = DecisionPayload.parseRequestId(json);
        int choice = DecisionPayload.parseChoice(json);
        DecisionWaiter waiter = pendingDecisions.get(requestId);
        if (waiter != null) {
            waiter.complete(choice);
        }
    }

    public int awaitDecision(int playerIndex, DecisionPayload payload) {
        DecisionWaiter waiter = new DecisionWaiter();
        pendingDecisions.put(payload.requestId, waiter);
        sendToPlayer(playerIndex, new NetworkMessage(NetworkMessage.DECISION_REQUEST, payload.toJson()));
        try {
            return waiter.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            pendingDecisions.remove(payload.requestId);
        }
    }

    public int getPlayerIndex(Player player) {
        if (gameManager == null || player == null) {
            return -1;
        }
        List<Player> players = gameManager.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) == player) {
                return i;
            }
        }
        return -1;
    }

    public void onClientDisconnected(int playerIndex) {
        String name = playerIndex >= 0 && playerIndex < playerNames.size()
                ? playerNames.get(playerIndex) : "Player " + (playerIndex + 1);
        broadcastEvent(name + " disconnected.");
        if (statusListener != null) {
            statusListener.onPlayerDisconnected(playerIndex, name);
        }
    }

    private void afterAction() {
        if (gameManager.isGameOver()) {
            broadcastGameState();
            broadcastGameOver();
            return;
        }

        Player current = gameManager.getCurrentPlayer();
        if (current.getActions() <= 0) {
            int discardNeeded = gameLogic.getRequiredDiscardCount(current);
            if (discardNeeded > 0) {
                broadcastGameState();
                broadcastEvent(current.getName() + " must discard " + discardNeeded + " card(s).");
                return;
            }
            gameLogic.endTurn();
            broadcastGameState();
            if (gameManager.isGameOver()) {
                broadcastGameOver();
            } else {
                notifyWhoseTurn();
            }
        } else {
            broadcastGameState();
        }
    }

    private boolean canAct(int fromIdx) {
        if (!gameStarted || gameManager.isGameOver()) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT, "Game is not running."));
            return false;
        }
        if (gameManager.getTurn() != fromIdx) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "It's not your turn! It's " + gameManager.getCurrentPlayer().getName() + "'s turn."));
            return false;
        }
        return true;
    }

    public void broadcastGameState() {
        if (gameManager == null) {
            return;
        }
        for (ClientHandler client : clients) {
            String json = GameStateSnapshot.toJson(gameManager, client.getPlayerIndex());
            client.send(new NetworkMessage(NetworkMessage.GAME_STATE, json));
        }
    }

    private void broadcastLobbyState() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"roomCode\":\"").append(NetworkJson.esc(roomCode)).append("\",");
        sb.append("\"count\":").append(clients.size()).append(",");
        sb.append("\"total\":").append(playerCount).append(",");
        sb.append("\"players\":[");
        for (int i = 0; i < playerNames.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"index\":").append(i)
                    .append(",\"name\":\"").append(NetworkJson.esc(playerNames.get(i))).append("\"")
                    .append(",\"connected\":").append(i < clients.size()).append('}');
        }
        sb.append("]}");
        broadcast(new NetworkMessage(NetworkMessage.LOBBY_STATE, sb.toString()));
    }

    public void broadcastEvent(String message) {
        broadcast(new NetworkMessage(NetworkMessage.EVENT, message));
    }

    public void broadcast(NetworkMessage msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    public void sendToPlayer(int playerIndex, NetworkMessage msg) {
        if (playerIndex >= 0 && playerIndex < clients.size()) {
            clients.get(playerIndex).send(msg);
        }
    }

    private void notifyWhoseTurn() {
        int currentIdx = gameManager.getTurn();
        Player current = gameManager.getCurrentPlayer();
        sendToPlayer(currentIdx, new NetworkMessage(NetworkMessage.YOUR_TURN,
                "It's your turn! Actions left: " + current.getActions()));
        for (int i = 0; i < clients.size(); i++) {
            if (i != currentIdx) {
                sendToPlayer(i, new NetworkMessage(NetworkMessage.WAIT,
                        "Waiting for " + current.getName() + " to play..."));
            }
        }
    }

    private void broadcastGameOver() {
        String winner = "Unknown";
        for (Player p : gameManager.getPlayers()) {
            if (p.getPropertyArea().countCompleteSets() >= 3) {
                winner = p.getName();
                break;
            }
        }
        broadcast(new NetworkMessage(NetworkMessage.GAME_OVER, winner + " wins!"));
        broadcastGameState();
    }

    private static String generateRoomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public int getPort() {
        return port;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerNameAt(int index) {
        if (index >= 0 && index < playerNames.size()) {
            return playerNames.get(index);
        }
        return "Player " + (index + 1);
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public static final class JoinResult {
        public final boolean accepted;
        public final int playerIndex;
        public final String playerName;
        public final String roomCode;
        public final String reason;

        private JoinResult(boolean accepted, int playerIndex, String playerName,
                           String roomCode, String reason) {
            this.accepted = accepted;
            this.playerIndex = playerIndex;
            this.playerName = playerName;
            this.roomCode = roomCode;
            this.reason = reason;
        }

        public static JoinResult accepted(int index, String name, String code) {
            return new JoinResult(true, index, name, code, null);
        }

        public static JoinResult rejected(String reason) {
            return new JoinResult(false, -1, null, null, reason);
        }

        public String welcomeJson() {
            return "{"
                    + "\"playerIndex\":" + playerIndex + ","
                    + "\"playerName\":\"" + NetworkJson.esc(playerName) + "\","
                    + "\"roomCode\":\"" + NetworkJson.esc(roomCode) + "\""
                    + "}";
        }
    }

    private static final class DecisionWaiter {
        private final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        private volatile int choice = -1;

        void complete(int value) {
            choice = value;
            latch.countDown();
        }

        int await(long timeout, TimeUnit unit) throws InterruptedException {
            if (!latch.await(timeout, unit)) {
                return -1;
            }
            return choice;
        }
    }
}
