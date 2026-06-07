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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LAN game server
 *
 *   Monitor the specified port and wait for all players to connect
 *   After the number of players is full, initialize the game and start the first round
 *   Receive operational requests from various clients, execute game logic
 */
public class GameServer {

    /** Default port number */
    public static final int DEFAULT_PORT = 1111;

    private final int port;
    private final int playerCount;
    private final List<String> playerNames;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private GameManager gameManager;
    private GameLogic gameLogic;

    private volatile boolean gameStarted = false;

    private ServerStatusListener statusListener;

    public interface ServerStatusListener {
        /** A new player has joined; count = current, total = needed, playerName = their name */
        void onPlayerJoined(int count, int total, String playerName);
        /** The game starts when all players arrive */
        void onGameStarted();
        /** A player disconnected from the connection */
        void onPlayerDisconnected(int playerIndex, String playerName);
    }

    public GameServer(int port, int playerCount, List<String> playerNames) {
        this.port = port;
        this.playerCount = playerCount;
        this.playerNames = new ArrayList<>(playerNames);
    }

    public void setStatusListener(ServerStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * Start the server and block it until all players are connected before starting the game
     */
    public void bind() throws IOException {
        serverSocket = new ServerSocket(port);
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("[Server] Bound on " + ip + ":" + port
                + " | Waiting for " + playerCount + " players...");
    }

    /**
     *   wait for all players to connect
     */
    public void acceptRemainingPlayers() throws IOException {
        while (clients.size() < playerCount) {
            Socket socket = serverSocket.accept();
            int idx = clients.size();
            System.out.println("[Server] Player " + (idx + 1) + " connected from "
                    + socket.getInetAddress().getHostAddress());

            ClientHandler handler = new ClientHandler(socket, this, idx);
            clients.add(handler);
            new Thread(handler, "Client-" + idx).start();

            broadcastEvent("Player " + (idx + 1) + " connected ("
                    + clients.size() + "/" + playerCount + ")");

            String joinedName = (idx < playerNames.size()) ? playerNames.get(idx) : "Player " + (idx + 1);
            if (statusListener != null) {
                statusListener.onPlayerJoined(clients.size(), playerCount, joinedName);
            }
        }

        // Full number of players
        initAndStartGame();
    }


    public void start() throws IOException {
        bind();
        acceptRemainingPlayers();
    }

    /** Close server and all client connections */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        for (ClientHandler c : clients) {
            c.close();
        }
    }


    private void initAndStartGame() {
        Deck.reset();
        GameManager.reset();

        gameManager = GameManager.getInstance();

        // Register Observer
        gameManager.attach(new IGameObserver() {
            @Override
            public void onGameUpdate(String event) {
                broadcastEvent(event);
            }
        });

        gameManager.initGame(playerCount, playerNames);

        gameLogic = new GameLogic(gameManager);
        gameLogic.getActionHandler().setUseDialogInput(false);

        gameStarted = true;

        broadcast(new NetworkMessage(NetworkMessage.GAME_START,
                "Game started with " + playerCount + " players!"));

        gameLogic.startGame();
        broadcastGameState();
        notifyWhoseTurn();

        if (statusListener != null) {
            statusListener.onGameStarted();
        }
    }

    /**
     * playing card
     */
    public synchronized void requestPlayCard(int fromIdx, int cardId) {
        if (!canAct(fromIdx)) return;

        Player player = gameManager.getCurrentPlayer();
        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found in your hand (id=" + cardId + ")."));
            return;
        }


        if (card instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) card;
            gameLogic.placeProperty(player, pc, pc.getColor());
        } else {
            gameLogic.playCard(player, card);
        }

        afterAction();
    }

    /**
     * property card placement
     */
    public synchronized void requestPlaceProperty(int fromIdx, String data) {
        if (!canAct(fromIdx)) return;

        Player player = gameManager.getCurrentPlayer();

        String[] parts = data.split(",", 2);
        int cardId;
        try {
            cardId = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Invalid card ID in PLACE_PROP."));
            return;
        }

        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found in hand (id=" + cardId + ")."));
            return;
        }
        if (!(card instanceof PropertyCard)) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card is not a property card."));
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

        gameLogic.placeProperty(player, pc, color);
        afterAction();
    }

    /**
     * deposit in bank
     */
    public synchronized void requestBankCard(int fromIdx, int cardId) {
        if (!canAct(fromIdx)) return;

        Player player = gameManager.getCurrentPlayer();
        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found in hand (id=" + cardId + ")."));
            return;
        }

        boolean ok = player.putMoneyInBank(cardId);
        if (ok) {
            gameManager.notifyAllObservers(
                    player.getName() + " deposited [" + card.getName() + "] to bank.");
        } else {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Cannot bank [" + card.getName() + "]."));
            return;
        }

        afterAction();
    }

    /**
     * End turn
     */
    public synchronized void requestEndTurn(int fromIdx) {
        if (!canAct(fromIdx)) return;

        Player player = gameManager.getCurrentPlayer();

        int discardNeeded = gameLogic.getRequiredDiscardCount(player);
        if (discardNeeded > 0) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "You must discard " + discardNeeded
                            + " card(s) first. Use DISCARD <cardId>."));
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

    /**
     * Fold
     */
    public synchronized void requestDiscard(int fromIdx, int cardId) {
        if (!gameStarted || gameManager.isGameOver()) return;

        if (gameManager.getTurn() != fromIdx) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "It's not your turn."));
            return;
        }

        Player player = gameManager.getCurrentPlayer();
        Card card = player.getHand().findCard(cardId);
        if (card == null) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Card not found (id=" + cardId + ")."));
            return;
        }

        boolean ok = gameLogic.discardCard(player, cardId);
        if (!ok) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Discard failed for card id=" + cardId + "."));
            return;
        }

        broadcastEvent(player.getName() + " discarded [" + card.getName() + "].");
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

    public void onClientDisconnected(int playerIndex) {
        String name = (playerIndex < playerNames.size()) ? playerNames.get(playerIndex) : "Player " + (playerIndex + 1);
        broadcastEvent(name + " disconnected.");
        if (statusListener != null) {
            statusListener.onPlayerDisconnected(playerIndex, name);
        }
    }

    /**
     * Check if the game has ended
     */
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
                broadcastEvent(current.getName() + " must discard "
                        + discardNeeded + " card(s). Use DISCARD <cardId>.");
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

    /**
     * check operation
     */
    private boolean canAct(int fromIdx) {
        if (!gameStarted || gameManager.isGameOver()) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "Game is not running."));
            return false;
        }
        if (gameManager.getTurn() != fromIdx) {
            sendToPlayer(fromIdx, new NetworkMessage(NetworkMessage.EVENT,
                    "It's not your turn! It's "
                            + gameManager.getCurrentPlayer().getName() + "'s turn."));
            return false;
        }
        return true;
    }

    public void broadcastGameState() {
        for (ClientHandler client : clients) {
            String json = GameStateSnapshot.toJson(gameManager, client.getPlayerIndex());
            client.send(new NetworkMessage(NetworkMessage.GAME_STATE, json));
        }
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

    public int getPort() { return port; }
    public int getPlayerCount() { return playerCount; }
    public boolean isGameStarted() { return gameStarted; }
}