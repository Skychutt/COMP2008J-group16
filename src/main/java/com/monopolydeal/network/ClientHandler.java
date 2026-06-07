package com.monopolydeal.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A processing thread for each player connection on the server.
 *
 *   Continuously read JSON messages sent by the client
 *   Forward the operation request to GameServer for processing
 *   Provide a method for sending messages to the client
 */
public class ClientHandler implements Runnable {

    private final Socket socket;          //TCP connection with the client
    private final GameServer server;      //Game server
    private final int playerIndex;        //Player ID corresponding to this client

    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, GameServer server, int playerIndex) {
        this.socket = socket;
        this.server = server;
        this.playerIndex = playerIndex;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send a welcome message to the client
            send(new NetworkMessage(NetworkMessage.WELCOME,
                    "{\"playerIndex\":" + playerIndex + "}"));

            //Read a welcome message to the client
            String line;
            while (running && (line = in.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.fromJson(line.trim());
                handleMessage(msg);
            }

        } catch (IOException e) {
            if (running) {
                System.out.println("[Server] Player " + (playerIndex + 1) + " disconnected: " + e.getMessage());
            }
        } finally {
            close();
            server.onClientDisconnected(playerIndex);
        }
    }

    /**
     *
     * Route client requests to the method corresponding to GameServer.
     */
    private void handleMessage(NetworkMessage msg) {
        switch (msg.getType()) {

            case NetworkMessage.PLAY_CARD:
                handleIntCommand(msg.getData(), cardId ->
                        server.requestPlayCard(playerIndex, cardId));
                break;

            case NetworkMessage.BANK_CARD:
                handleIntCommand(msg.getData(), cardId ->
                        server.requestBankCard(playerIndex, cardId));
                break;

            case NetworkMessage.PLACE_PROP:
                server.requestPlaceProperty(playerIndex, msg.getData());
                break;

            case NetworkMessage.END_TURN:
                server.requestEndTurn(playerIndex);
                break;

            case NetworkMessage.DISCARD:
                handleIntCommand(msg.getData(), cardId ->
                        server.requestDiscard(playerIndex, cardId));
                break;

            case NetworkMessage.PING:
                send(new NetworkMessage(NetworkMessage.PONG, ""));
                break;

            case NetworkMessage.PONG:
                break; // client echoed back, no action needed

            default:
                send(new NetworkMessage(NetworkMessage.EVENT,
                        "Unknown command: " + msg.getType()));
                break;
        }
    }

    private void handleIntCommand(String data, java.util.function.IntConsumer action) {
        try {
            action.accept(Integer.parseInt(data.trim()));
        } catch (NumberFormatException e) {
            send(new NetworkMessage(NetworkMessage.EVENT,
                    "Invalid card ID: \"" + data + "\""));
        }
    }

    /**
     *Prevent output disorder
     */
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
        } catch (IOException ignored) {}
    }

    public int getPlayerIndex() { return playerIndex; }
}
