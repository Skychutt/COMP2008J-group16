package com.monopolydeal;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.network.ClientGameMirror;
import com.monopolydeal.network.GameStateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LAN client mirror must not shuffle/deal locally and must sync deck counts from snapshots.
 */
class ClientGameMirrorTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
        GameManager.reset();
    }

    @Test
    void initPlayersOnlyDoesNotTouchDeck() {
        Deck deck = Deck.getInstance();
        int initialDraw = deck.drawPileSize();

        GameManager gm = GameManager.getInstance();
        gm.initPlayersOnly(2, java.util.List.of("Host", "Guest"));

        assertEquals(initialDraw, deck.drawPileSize(), "initPlayersOnly must not draw from the deck");
        assertEquals(2, gm.getPlayers().size());
        assertTrue(gm.getPlayers().get(0).getHand().isEmpty());
    }

    @Test
    void applySnapshotDoesNotMutateSharedDeck() {
        Deck deck = Deck.getInstance();
        GameManager serverGm = GameManager.getInstance();
        serverGm.initGame(2, java.util.List.of("A", "B"));
        int drawAfterDeal = deck.drawPileSize();
        int discardAfterDeal = deck.discardSize();

        GameManager.reset();
        GameManager gm = GameManager.getInstance();
        gm.initPlayersOnly(2, java.util.List.of("A", "B"));

        GameStateParser.Snapshot snap = new GameStateParser.Snapshot();
        snap.turn = 0;
        snap.currentPlayer = "A";
        snap.deckSize = 91;
        snap.discardSize = 2;
        snap.gameOver = false;

        GameStateParser.CardInfo discardTop = new GameStateParser.CardInfo();
        discardTop.id = 42;
        discardTop.name = "Pass Go";
        discardTop.value = 1;
        discardTop.cardType = "ACTION";
        discardTop.actionType = "GO_PASS";
        snap.discardTop = discardTop;

        GameStateParser.PlayerInfo p0 = new GameStateParser.PlayerInfo();
        p0.index = 0;
        p0.name = "A";
        p0.handSize = 1;
        p0.actions = 3;
        GameStateParser.CardInfo handCard = new GameStateParser.CardInfo();
        handCard.id = 7;
        handCard.name = "2M";
        handCard.value = 2;
        handCard.cardType = "MONEY";
        p0.hand = new java.util.ArrayList<>(java.util.List.of(handCard));

        GameStateParser.PlayerInfo p1 = new GameStateParser.PlayerInfo();
        p1.index = 1;
        p1.name = "B";
        p1.handSize = 5;
        snap.players = java.util.List.of(p0, p1);
        snap.myHand = p0.hand;

        ClientGameMirror.applySnapshot(gm, snap, 0);

        assertEquals(drawAfterDeal, deck.drawPileSize(),
                "LAN client mirror must not rewrite the shared Deck singleton");
        assertEquals(discardAfterDeal, deck.discardSize());
        assertEquals(1, gm.getPlayers().get(0).getHand().size());
        assertEquals(5, gm.getPlayers().get(1).getVisibleHandSize());
    }

    @Test
    void mirrorInitMustNotCorruptAuthoritativeDeck() {
        // Simulate server start: reset deck, deal 5 cards to 2 players.
        Deck deck = Deck.getInstance();
        GameManager serverGm = GameManager.getInstance();
        serverGm.initGame(2, java.util.List.of("Host", "Guest"));
        int serverDrawAfterDeal = deck.drawPileSize();

        // Old bug: client mirror called initGame again on the same Deck singleton.
        GameManager clientMirror = GameManager.getInstance();
        GameManager.reset();
        clientMirror = GameManager.getInstance();
        clientMirror.initPlayersOnly(2, java.util.List.of("Host", "Guest"));

        assertEquals(serverDrawAfterDeal, deck.drawPileSize(),
                "Client mirror setup must not reshuffle or deal from the shared Deck singleton");
    }
}
