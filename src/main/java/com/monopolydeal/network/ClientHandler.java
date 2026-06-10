package com.monopolydeal.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Per-client connection thread on the LAN server.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;

    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;
    private volatile int playerIndex = -1;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void startReaderThread() {
        Thread t = new Thread(this, "ClientHandler-" + socket.getRemoteSocketAddress());
        t.setDaemon(true);
        t.start();
    }

    void assignPlayerIndex(int index) {
        this.playerIndex = index;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String firstLine = in.readLine();
            if (firstLine == null) {
                return;
            }
            NetworkMessage joinMsg = NetworkMessage.fromJson(firstLine.trim());
            if (!NetworkMessage.JOIN.equals(joinMsg.getType())) {
                send(new NetworkMessage(NetworkMessage.JOIN_REJECTED, "First message must be JOIN."));
                return;
            }

            GameServer.JoinResult result = server.handleJoin(this, joinMsg.getData());
            if (!result.accepted) {
                send(new NetworkMessage(NetworkMessage.JOIN_REJECTED, result.reason));
                return;
            }

            send(new NetworkMessage(NetworkMessage.WELCOME, result.welcomeJson()));
            server.onClientWelcomed(this);

            String line;
            while (running && (line = in.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.fromJson(line.trim());
                handleMessage(msg);
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[Server] Client disconnected: " + e.getMessage());
            }
        } finally {
            close();
            if (playerIndex >= 0) {
                server.onClientDisconnected(playerIndex);
            }
        }
    }

    private void handleMessage(NetworkMessage msg) {
        switch (msg.getType()) {
            case NetworkMessage.PLAY_CARD:
                handleInt(msg.getData(), id -> server.submitPlayCard(playerIndex, id));
                break;
            case NetworkMessage.PLAY_ON_TARGET:
                handlePlayOnTarget(msg.getData());
                break;
            case NetworkMessage.BANK_CARD:
                handleInt(msg.getData(), id -> server.submitBankCard(playerIndex, id));
                break;
            case NetworkMessage.PLACE_PROP:
                server.submitPlaceProperty(playerIndex, msg.getData());
                break;
            case NetworkMessage.END_TURN:
                server.submitEndTurn(playerIndex);
                break;
            case NetworkMessage.DISCARD:
                handleInt(msg.getData(), id -> server.submitDiscard(playerIndex, id));
                break;
            case NetworkMessage.DECISION_RESPONSE:
                server.receiveDecisionResponse(playerIndex, msg.getData());
                break;
            case NetworkMessage.PING:
                send(new NetworkMessage(NetworkMessage.PONG, ""));
                break;
            default:
                send(new NetworkMessage(NetworkMessage.EVENT, "Unknown command: " + msg.getType()));
                break;
        }
    }

    private void handlePlayOnTarget(String data) {
        String[] parts = data.split(",", 2);
        if (parts.length < 2) {
            send(new NetworkMessage(NetworkMessage.EVENT, "Invalid PLAY_ON_TARGET payload."));
            return;
        }
        try {
            int cardId = Integer.parseInt(parts[0].trim());
            int targetIdx = Integer.parseInt(parts[1].trim());
            server.submitPlayOnTarget(playerIndex, cardId, targetIdx);
        } catch (NumberFormatException e) {
            send(new NetworkMessage(NetworkMessage.EVENT, "Invalid PLAY_ON_TARGET numbers."));
        }
    }

    private void handleInt(String data, java.util.function.IntConsumer action) {
        try {
            action.accept(Integer.parseInt(data.trim()));
        } catch (NumberFormatException e) {
            send(new NetworkMessage(NetworkMessage.EVENT, "Invalid number: \"" + data + "\""));
        }
    }

    public synchronized void send(NetworkMessage msg) {
        if (out != null && !socket.isClosed()) {
            out.println(msg.toJson());
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}
