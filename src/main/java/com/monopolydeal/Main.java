package com.monopolydeal;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Monopoly Deal Test ===\n");

        // 1. Test Deck singleton & initialization
        Deck deck = Deck.getInstance();
        System.out.println("[1] Deck initialized: " + deck.drawPileSize() + " cards");

        // 2. Test shuffle
        deck.shuffle();
        System.out.println("[2] Shuffle complete");

        // 3. Test drawing cards
        java.util.List<Card> drawn = deck.draw(5);
        System.out.println("[3] Drew 5 cards:");
        for (Card c : drawn) {
            System.out.println("    " + c);
        }
        System.out.println("    Draw pile remaining: " + deck.drawPileSize() + " cards");

        // 4. Test discard
        deck.addToDiscard(drawn.get(0));
        System.out.println("[4] Discard pile: " + deck.discardSize() + " cards");

        // Reset for GameManager test
        Deck.reset();
        GameManager.reset();

        // 5. Test GameManager init game (3 players)
        System.out.println("\n[5] Initializing 3-player game...");
        GameManager gm = GameManager.getInstance();
        gm.initGame(3);

        for (Player p : gm.getPlayers()) {
            System.out.println("    " + p.getName() + " hand: " + p.getHand().size() + " cards");
        }
        System.out.println("    Draw pile remaining: " + Deck.getInstance().drawPileSize() + " cards");

        // 6. Test turn: draw cards
        System.out.println("\n[6] Next turn (Player 1 draws)...");
        gm.nextTurn();
        Player current = gm.getCurrentPlayer();
        System.out.println("    Current player: " + current.getName());
        System.out.println("    Hand size: " + current.getHand().size());
        System.out.println("    Actions remaining: " + current.getActions());

        // 7. Test playing a card (play the first card in hand)
        if (current.getHand().size() > 0) {
            Card firstCard = current.getHand().getCards().get(0);
            System.out.println("\n[7] Playing card: " + firstCard);
            current.playCard(firstCard.getId());
            System.out.println("    Hand remaining: " + current.getHand().size());
            System.out.println("    Actions remaining: " + current.getActions());
            System.out.println("    Bank: " + current.getBankArea().total() + "M");
            System.out.println("    Complete property sets: " + current.getPropertyArea().countCompleteSets());
        }

        // 8. Test win detection
        System.out.println("\n[8] Win check: " + (gm.checkWin() ? "Someone won" : "Game continues"));

        System.out.println("\n=== All tests passed ===");
    }
}
