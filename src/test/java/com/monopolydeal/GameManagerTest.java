package com.monopolydeal;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameManager – singleton, initialization, turn management, and win detection.
 * Covers Sequence Diagrams 1 (Start Game), 9 (End Turn), and 10 (Win Condition).
 */
class GameManagerTest {

    @BeforeEach
    void setUp() {
        GameManager.reset();
        Deck.reset();
    }

    @Test
    void testSingletonPattern() {
        GameManager gm1 = GameManager.getInstance();
        GameManager gm2 = GameManager.getInstance();
        assertSame(gm1, gm2, "GameManager.getInstance() must return the same instance");
    }

    @Test
    void testResetCreatesFreshInstance() {
        GameManager original = GameManager.getInstance();
        GameManager.reset();
        GameManager fresh = GameManager.getInstance();
        assertNotSame(original, fresh);
    }

    @Test
    void testInitGameCreatesCorrectPlayerCount() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(3);
        assertEquals(3, gm.getPlayers().size(), "initGame(3) must create exactly 3 players");
    }

    @Test
    void testInitGameDealsCardsToEachPlayer() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        for (Player p : gm.getPlayers()) {
            assertEquals(5, p.getHand().size(), "Each player must start with 5 cards");
        }
    }

    @Test
    void testInitGameSetsTurnToZero() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        assertEquals(0, gm.getTurn());
    }

    @Test
    void testGetCurrentPlayerReturnsTurnPlayer() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(3);
        Player current = gm.getCurrentPlayer();
        assertNotNull(current);
        assertSame(gm.getPlayers().get(0), current, "First player must be current on turn 0");
    }

    @Test
    void testNextTurnAdvancesToNextPlayer() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(3);
        gm.beginCurrentPlayerTurn();
        Player first = gm.getCurrentPlayer();
        gm.nextTurn();
        Player second = gm.getCurrentPlayer();
        assertNotSame(first, second, "nextTurn must switch to a different player");
    }

    @Test
    void testNextTurnWrapsAroundToFirstPlayer() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();
        Player player0 = gm.getCurrentPlayer();
        gm.nextTurn();
        gm.nextTurn();
        Player backToFirst = gm.getCurrentPlayer();
        assertSame(player0, backToFirst, "After all players take a turn, it must wrap back to the first");
    }

    @Test
    void testGameNotOverInitially() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        assertFalse(gm.isGameOver());
    }

    @Test
    void testMarkWinnerEndsGame() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        Player winner = gm.getPlayers().get(0);
        gm.markWinner(winner);
        assertTrue(gm.isGameOver(), "isGameOver must be true after markWinner()");
    }

    @Test
    void testMarkWinnerCalledTwiceDoesNothing() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.markWinner(gm.getPlayers().get(0));
        // Calling again must not throw
        assertDoesNotThrow(() -> gm.markWinner(gm.getPlayers().get(1)));
    }

    @Test
    void testCheckWinReturnsFalseWithNoCompleteSets() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        assertFalse(gm.checkWin(), "checkWin must be false when no player has 3 complete sets");
    }

    @Test
    void testBeginCurrentPlayerTurnGivesThreeActions() {
        GameManager gm = GameManager.getInstance();
        gm.initGame(2);
        gm.beginCurrentPlayerTurn();
        assertEquals(Player.ACTIONS_PER_TURN, gm.getCurrentPlayer().getActions());
    }

    @Test
    void testObserverReceivesNotification() {
        GameManager gm = GameManager.getInstance();
        StringBuilder log = new StringBuilder();
        gm.attach(event -> log.append(event));
        gm.notifyAllObservers("test-event");
        assertTrue(log.toString().contains("test-event"), "Observer must receive the broadcasted event");
    }

    @Test
    void testDetachedObserverReceivesNoNotification() {
        GameManager gm = GameManager.getInstance();
        StringBuilder log = new StringBuilder();
        com.monopolydeal.interfaces.IGameObserver obs = event -> log.append(event);
        gm.attach(obs);
        gm.detach(obs);
        gm.notifyAllObservers("should-not-arrive");
        assertTrue(log.length() == 0, "Detached observer must not receive events");
    }
}
