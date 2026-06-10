package com.monopolydeal;

import com.monopolydeal.logic.TurnManager;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TurnManager – turn lifecycle, action tracking, and end-phase enforcement.
 * Covers Sequence Diagram 9 (End Turn Sequence).
 */
class TurnManagerTest {

    @BeforeEach
    void setUp() {
        Deck.reset();
    }

    @Test
    void testStartTurnActivatesTurn() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        tm.startTurn(player);
        assertTrue(tm.isTurnActive(), "Turn must be active after startTurn()");
        assertSame(player, tm.getCurrentPlayer());
    }

    @Test
    void testCanPerformActionWhenActionsRemain() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        player.setActions(3);
        tm.startTurn(player);
        assertTrue(tm.canPerformAction());
    }

    @Test
    void testCannotPerformActionWhenNoActionsLeft() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        player.setActions(0);
        tm.startTurn(player);
        assertFalse(tm.canPerformAction());
    }

    @Test
    void testConsumeActionDecrementsCount() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        player.setActions(3);
        tm.startTurn(player);

        tm.consumeAction();
        assertEquals(2, player.getActions());
        assertEquals(2, tm.getRemainingActions());
    }

    @Test
    void testConsumeActionDoesNothingAtZero() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        player.setActions(0);
        tm.startTurn(player);
        tm.consumeAction(); // must not go negative
        assertEquals(0, player.getActions());
    }

    @Test
    void testEndTurnDeactivatesTurn() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        tm.startTurn(player);
        tm.endTurn();
        assertFalse(tm.isTurnActive(), "Turn must be inactive after endTurn()");
    }

    @Test
    void testExecuteDrawPhaseAddsCardsToHand() {
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        // Add one card so draw picks 2 (not 5)
        player.getHand().add(Deck.getInstance().draw(1).get(0));
        int before = player.getHand().size();
        tm.startTurn(player);
        tm.executeDrawPhase();
        assertEquals(before + 2, player.getHand().size(),
                "executeDrawPhase must draw 2 cards for non-empty hand");
    }

    @Test
    void testExecuteEndPhaseCompletesWithoutCrash() {
        // Note: hand-limit enforcement moved to GUI layer (GameLogic.discardCard).
        // executeEndPhase now only notifies observers that the turn ended.
        TurnManager tm = new TurnManager();
        Player player = new Player("1", "Alice");
        player.setActions(3);
        for (int i = 0; i < 9; i++) {
            player.getHand().add(Deck.getInstance().draw(1).get(0));
        }
        tm.startTurn(player);
        assertDoesNotThrow(() -> tm.executeEndPhase(),
                "executeEndPhase must complete without throwing even with oversized hand");
    }

    @Test
    void testGetRemainingActionsWithNoPlayer() {
        TurnManager tm = new TurnManager();
        assertEquals(0, tm.getRemainingActions(), "No player set: remaining actions must be 0");
    }
}
