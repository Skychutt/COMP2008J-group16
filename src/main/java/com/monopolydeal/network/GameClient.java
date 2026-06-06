package com.monopolydeal.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * LAN gaming client
 *
 *   Connect to GameServer
 *   Notify the UI layer to update the display
 *   Provide a method for sending operation instructions to the server
 *
 * Does not contain any game logic, only performs network I/O
 * The display of game status is handled by NetworkGameFrame (GUI)
 */
public class GameClient {

    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;

    private volatile int myPlayerIndex = -1;

    // Receive messages pushed by the server
    private MessageListener messageListener;

    /**
     * Message monitoring interface
     */
    public interface MessageListener {
        /** Successfully connected, myIndex is the player's ID number */
        void onConnected(int myIndex);
        /** Received game status update */
        void onGameState(String stateJson);
        /** It's our player's turn to take action */
        void onYourTurn(String message);
        /** Waiting for other players */
        void onWait(String message);
        /** In-game event notifications */
        void onEvent(String event);
        /** Game start */
        void onGameStart(String message);
        /** Game over */
        void onGameOver(String winner);
        /** Connection disconnected or error occurred */
        void onDisconnected(String reason);
    }

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Connect to the server and start the receiving thread
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        // start the receiving thread
        Thread receiver = new Thread(this::receiveLoop, "NetworkReceiver");
        receiver.setDaemon(true);
        receiver.start();
    }

    /** Disconnect */
    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void receiveLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.fromJson(line.trim());
                dispatch(msg);
            }
        } catch (IOException e) {
            if (running) {
                notify_disconnected("Connection lost: " + e.getMessage());
            }
        }
        running = false;
    }

    /** Call the corresponding method */
    private void dispatch(NetworkMessage msg) {
        if (messageListener == null) return;

        switch (msg.getType()) {

            case NetworkMessage.WELCOME:
                int idx = extractInt(msg.getData(), "playerIndex");
                myPlayerIndex = idx;
                messageListener.onConnected(idx);
                break;

            case NetworkMessage.GAME_START:
                messageListener.onGameStart(msg.getData());
                break;

            case NetworkMessage.GAME_STATE:
                messageListener.onGameState(msg.getData());
                break;

            case NetworkMessage.YOUR_TURN:
                messageListener.onYourTurn(msg.getData());
                break;

            case NetworkMessage.WAIT:
                messageListener.onWait(msg.getData());
                break;

            case NetworkMessage.EVENT:
                messageListener.onEvent(msg.getData());
                break;

            case NetworkMessage.GAME_OVER:
                messageListener.onGameOver(msg.getData());
                break;

            default:
                break;
        }
    }

    private void notify_disconnected(String reason) {
        if (messageListener != null) {
            messageListener.onDisconnected(reason);
        }
    }

    /** Play a card  */
    public void sendPlayCard(int cardId) {
        send(new NetworkMessage(NetworkMessage.PLAY_CARD, String.valueOf(cardId)));
    }

    /** Deposit a card into the bank */
    public void sendBankCard(int cardId) {
        send(new NetworkMessage(NetworkMessage.BANK_CARD, String.valueOf(cardId)));
    }

    /**
     * Place attribute card
     */
    public void sendPlaceProperty(int cardId, String colorName) {
        String data = (colorName == null || colorName.isEmpty())
                ? String.valueOf(cardId)
                : cardId + "," + colorName;
        send(new NetworkMessage(NetworkMessage.PLACE_PROP, data));
    }

    /** End this round */
    public void sendEndTurn() {
        send(new NetworkMessage(NetworkMessage.END_TURN, ""));
    }

    /** Abandoned cards */
    public void sendDiscard(int cardId) {
        send(new NetworkMessage(NetworkMessage.DISCARD, String.valueOf(cardId)));
    }

    private synchronized void send(NetworkMessage msg) {
        if (out != null && running) {
            out.println(msg.toJson());
        }
    }

    private static int extractInt(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return -1;
        start += marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ── Getters ──────────────────────────────────────────────────
    public int getMyPlayerIndex() { return myPlayerIndex; }
    public boolean isConnected() { return running && socket != null && !socket.isClosed(); }
}
