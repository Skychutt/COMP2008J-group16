package com.monopolydeal.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * LAN game client: connects with room code + player name, relays UI actions, handles remote decisions.
 */
public class GameClient {

    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;
    private ScheduledExecutorService heartbeat;

    private volatile int myPlayerIndex = -1;
    private volatile String myPlayerName = "";
    private volatile String roomCode = "";

    private MessageListener messageListener;
    private DecisionListener decisionListener;

    public interface MessageListener {
        void onConnected(int myIndex, String playerName, String roomCode);
        void onJoinRejected(String reason);
        void onLobbyState(String json);
        void onGameState(String stateJson);
        void onYourTurn(String message);
        void onWait(String message);
        void onEvent(String event);
        void onGameStart(String message);
        void onGameOver(String winner);
        void onDisconnected(String reason);
    }

    public interface DecisionListener {
        int onDecisionRequested(DecisionPayload payload);
    }

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void setDecisionListener(DecisionListener listener) {
        this.decisionListener = listener;
    }

    public void connect(String joinRoomCode, String playerName) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(120_000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String joinJson = "{"
                + "\"roomCode\":\"" + NetworkJson.esc(joinRoomCode) + "\","
                + "\"playerName\":\"" + NetworkJson.esc(playerName) + "\""
                + "}";
        out.println(new NetworkMessage(NetworkMessage.JOIN, joinJson).toJson());

        String firstLine = in.readLine();
        if (firstLine == null) {
            throw new IOException("Server closed connection during join.");
        }
        NetworkMessage first = NetworkMessage.fromJson(firstLine.trim());
        if (NetworkMessage.JOIN_REJECTED.equals(first.getType())) {
            throw new IOException(first.getData());
        }
        if (!NetworkMessage.WELCOME.equals(first.getType())) {
            throw new IOException("Unexpected server response: " + first.getType());
        }

        myPlayerIndex = NetworkJson.num(first.getData(), "playerIndex");
        myPlayerName = NetworkJson.str(first.getData(), "playerName");
        roomCode = NetworkJson.str(first.getData(), "roomCode");
        running = true;

        if (messageListener != null) {
            messageListener.onConnected(myPlayerIndex, myPlayerName, roomCode);
        }

        Thread receiver = new Thread(this::receiveLoop, "NetworkReceiver");
        receiver.setDaemon(true);
        receiver.start();

        heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NetworkHeartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeat.scheduleAtFixedRate(() -> {
            if (running) {
                send(new NetworkMessage(NetworkMessage.PING, ""));
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    public void disconnect() {
        running = false;
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void receiveLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                dispatch(NetworkMessage.fromJson(line.trim()));
            }
        } catch (IOException e) {
            if (running) {
                notifyDisconnected("Connection lost: " + e.getMessage());
            }
        }
        running = false;
    }

    private void dispatch(NetworkMessage msg) {
        if (messageListener == null && !NetworkMessage.DECISION_REQUEST.equals(msg.getType())) {
            return;
        }

        switch (msg.getType()) {
            case NetworkMessage.LOBBY_STATE:
                if (messageListener != null) {
                    messageListener.onLobbyState(msg.getData());
                }
                break;
            case NetworkMessage.GAME_START:
                if (messageListener != null) {
                    messageListener.onGameStart(msg.getData());
                }
                break;
            case NetworkMessage.GAME_STATE:
                if (messageListener != null) {
                    messageListener.onGameState(msg.getData());
                }
                break;
            case NetworkMessage.YOUR_TURN:
                if (messageListener != null) {
                    messageListener.onYourTurn(msg.getData());
                }
                break;
            case NetworkMessage.WAIT:
                if (messageListener != null) {
                    messageListener.onWait(msg.getData());
                }
                break;
            case NetworkMessage.EVENT:
                if (messageListener != null) {
                    messageListener.onEvent(msg.getData());
                }
                break;
            case NetworkMessage.GAME_OVER:
                if (messageListener != null) {
                    messageListener.onGameOver(msg.getData());
                }
                break;
            case NetworkMessage.DECISION_REQUEST:
                handleDecisionRequest(msg.getData());
                break;
            case NetworkMessage.PONG:
                break;
            default:
                break;
        }
    }

    private void handleDecisionRequest(String json) {
        DecisionPayload payload = DecisionPayload.fromJson(json);
        int choice = -1;
        if (decisionListener != null) {
            choice = decisionListener.onDecisionRequested(payload);
        }
        send(new NetworkMessage(NetworkMessage.DECISION_RESPONSE,
                DecisionPayload.responseJson(payload.requestId, choice)));
    }

    private void notifyDisconnected(String reason) {
        if (messageListener != null) {
            messageListener.onDisconnected(reason);
        }
    }

    public void sendPlayCard(int cardId) {
        send(new NetworkMessage(NetworkMessage.PLAY_CARD, String.valueOf(cardId)));
    }

    public void sendPlayOnTarget(int cardId, int targetPlayerIndex) {
        send(new NetworkMessage(NetworkMessage.PLAY_ON_TARGET, cardId + "," + targetPlayerIndex));
    }

    public void sendBankCard(int cardId) {
        send(new NetworkMessage(NetworkMessage.BANK_CARD, String.valueOf(cardId)));
    }

    public void sendPlaceProperty(int cardId, String colorName) {
        String data = (colorName == null || colorName.isEmpty())
                ? String.valueOf(cardId)
                : cardId + "," + colorName;
        send(new NetworkMessage(NetworkMessage.PLACE_PROP, data));
    }

    public void sendEndTurn() {
        send(new NetworkMessage(NetworkMessage.END_TURN, ""));
    }

    public void sendDiscard(int cardId) {
        send(new NetworkMessage(NetworkMessage.DISCARD, String.valueOf(cardId)));
    }

    private synchronized void send(NetworkMessage msg) {
        if (out != null && running) {
            out.println(msg.toJson());
        }
    }

    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }

    public String getMyPlayerName() {
        return myPlayerName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}
