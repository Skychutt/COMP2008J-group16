package com.monopolydeal;

import com.monopolydeal.interfaces.IGameObserver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

import java.util.List;
import java.util.Scanner;

/**
 * Text-mode Monopoly Deal: Scanner input and console output, turn rotation until the game ends.
 */
public class MonopolyDealConsole {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int playerCount = readPlayerCount(scanner);

        Deck.reset();
        GameManager.reset();

        final GameManager gameManager = GameManager.getInstance();
        gameManager.attach(new IGameObserver() {
            @Override
            public void onGameUpdate(String event) {
                System.out.println("[Game] " + event);
            }
        });

        gameManager.initGame(playerCount);
        GameLogic logic = new GameLogic(gameManager);
        logic.startGame();

        System.out.println();
        System.out.println("=== Monopoly Deal: " + playerCount + " players ===");
        System.out.println("On your turn: enter a card ID to play it, or 0 to end your turn (max 3 plays per turn).");
        System.out.println("Game ends when someone has 3 complete property sets.");
        System.out.println();

        while (!gameManager.isGameOver()) {
            Player current = gameManager.getCurrentPlayer();

            System.out.println("----------------------------------------");
            System.out.println("Current player: " + current.getName());
            System.out.println("Bank: " + current.getBankArea().total() + "M | Complete sets: "
                    + current.getPropertyArea().countCompleteSets());
            printPublicSummary(gameManager, current);

            while (current.getActions() > 0 && !gameManager.isGameOver()) {
                System.out.println();
                System.out.println("Actions left this turn: " + current.getActions());
                printHand(current);
                System.out.print("Enter card ID to play, or 0 to end turn: ");

                String line = scanner.nextLine();
                if (line == null) {
                    line = "";
                }
                line = line.trim();
                if (line.length() == 0) {
                    System.out.println("Please enter a number.");
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(line);
                } catch (NumberFormatException ex) {
                    System.out.println("That is not a valid number.");
                    continue;
                }

                if (choice == 0) {
                    System.out.println(current.getName() + " ends the play phase.");
                    break;
                }

                Card toPlay = current.getHand().findCard(choice);
                if (toPlay == null) {
                    System.out.println("No card in your hand with ID " + choice + ".");
                    continue;
                }

                System.out.println("Playing: " + toPlay);
                logic.playCard(current, toPlay);

                if (gameManager.isGameOver()) {
                    break;
                }
            }

            if (gameManager.isGameOver()) {
                break;
            }

            System.out.println();
            System.out.println("Ending turn and moving to next player...");
            logic.endTurn();
            gameManager.printGameState();
        }

        System.out.println();
        System.out.println("=== Game over ===");
        scanner.close();
    }

    private static int readPlayerCount(Scanner scanner) {
        int playerCount = 0;
        while (true) {
            System.out.print("Enter number of players (2-5): ");
            String line = scanner.nextLine();
            if (line == null) {
                line = "";
            }
            line = line.trim();
            if (line.length() == 0) {
                System.out.println("Please enter a whole number between 2 and 5.");
                continue;
            }
            try {
                playerCount = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a whole number between 2 and 5.");
                continue;
            }
            if (playerCount >= 2 && playerCount <= 5) {
                break;
            }
            System.out.println("Invalid input. Player count must be between 2 and 5.");
        }
        return playerCount;
    }

    private static void printHand(Player player) {
        System.out.println("Your hand:");
        List<Card> cards = player.getHand().getCards();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            System.out.println("  id=" + c.getId() + "  " + c);
        }
        if (cards.size() == 0) {
            System.out.println("  (empty)");
        }
    }

    /** Prints other players' bank and set counts (not their hands). */
    private static void printPublicSummary(GameManager gameManager, Player current) {
        System.out.println("Other players:");
        List<Player> all = gameManager.getPlayers();
        for (int i = 0; i < all.size(); i++) {
            Player p = all.get(i);
            if (p == current) {
                continue;
            }
            System.out.println("  " + p.getName() + " | Bank: " + p.getBankArea().total()
                    + "M | Complete sets: " + p.getPropertyArea().countCompleteSets()
                    + " | Hand size: " + p.getHand().size());
        }
    }
}
