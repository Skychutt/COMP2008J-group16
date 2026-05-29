package com.monopolydeal.logic;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Handles the execution of all action card effects.
 */
public class ActionHandler {

    private static final int BIRTHDAY_PAYMENT = 2;
    private static final int DEBT_COLLECTOR_AMOUNT = 5;

    private final GameLogic gameLogic;
    private Scanner scanner;

    public ActionHandler(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        this.scanner = new Scanner(System.in);
    }
    /** Refresh the scanner to pick up changes to System.in (useful for testing). */
    public void refreshScanner() {
        this.scanner = new Scanner(System.in);

    }

    /**
     * Executes the effect of a given action card.
     * @param player the player playing the card
     * @param card the action card to execute
     */
    public void executeAction(Player player, ActionCard card) {
        if (player == null || card == null) {
            return;
        }
        switch (card.getType()) {
            case GO_PASS:
                handlePassGo(player);
                break;
            case BIRTHDAY:
                handleBirthday(player);
                break;
            case DEBT_DEAL:
                handleDebtCollector(player);
                break;
            case SLY_DEAL:
                handleSlyDealAuto(player, card);
                break;
            case FORCED_DEAL:
                handleForcedDealStub(player, card);
                break;
            case DEAL_BREAKER:
                handleDealBreakerAuto(player, card);
                break;
            case DOUBLE_RENT:
                handleDoubleRentOrRentStub(player, card);
                break;
            case JUST_SAY_NO:
                break;
            default:
                break;
        }
    }
    private PropertySet firstCompleteSet(Player player) {
        for (PropertySet s : player.getPropertyArea().getSets()) {
            if (RentCalculator.canCollectRent(s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Handles the "Just Say No" defense mechanism.
     * @param defender the player being attacked
     * @param attacker the player using the action
     * @param attackCard the card being defended against
     * @return true if the attack was successfully defended
     */
    public boolean handleJustSayNo(Player defender, Player attacker, ActionCard attackCard) {
        if (defender == null || attackCard == null || !attackCard.isCanDefend()) {
            return false;
        }
        Card js = findJustSayNo(defender);
        if (js == null) {
            return false;
        }
        defender.getHand().removeCard(js.getId());
        Deck.getInstance().addToDiscard(js);
        String attackerName = attacker != null ? attacker.getName() : "an opponent";
        gameLogic.getGameManager().notifyAllObservers(defender.getName() + " blocked " + attackerName
                + "'s [" + attackCard.getName() + "] with Just Say No!");
        return true;
    }

    private Card findJustSayNo(Player defender) {
        for (Card c : defender.getHand().getCards()) {
            if (c instanceof ActionCard && ((ActionCard) c).getType() == ActionType.JUST_SAY_NO) {
                return c;
            }
        }
        return null;
    }

    public void handlePassGo(Player player) {
        List<Card> extra = Deck.getInstance().draw(2);
        for (Card c : extra) {
            player.getHand().add(c);
        }
        gameLogic.getGameManager().notifyAllObservers(player.getName() + " drew " + extra.size() + " extra card(s) from Pass Go.");
    }

    public void handleRent(Player collector, Player target, int amount) {
        AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                .processPayment(target, collector, amount, AssetTransferManager.PaymentMode.USE_MIXED);
        gameLogic.getGameManager().notifyAllObservers(r.getMessage());
    }

    public void handleBirthday(Player initiator) {
        GameManager gm = gameLogic.getGameManager();
        for (Player other : gm.getPlayers()) {
            if (other == initiator) {
                continue;
            }
            AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                    .processPayment(other, initiator, BIRTHDAY_PAYMENT, AssetTransferManager.PaymentMode.USE_MIXED);
            gm.notifyAllObservers(r.getMessage());
        }
    }

    private void handleDebtCollector(Player collector) {
        GameManager gm = gameLogic.getGameManager();
        List<Player> others = new ArrayList<>();
        for (Player p : gm.getPlayers()) {
            if (p != collector) others.add(p);
        }

        if (others.isEmpty()) {
            gm.notifyAllObservers("Debt Collector: no other players.");
            return;
        }

        // Let the player choose who to charge
        System.out.println();
        System.out.println("=== Debt Collector: Choose a player to charge 5M ===");
        for (int i = 0; i < others.size(); i++) {
            Player p = others.get(i);
            System.out.println("  " + (i + 1) + ". " + p.getName() + " (Bank: " + p.getBankArea().total() + "M)");
        }

        int choice = readChoice(1, others.size());
        Player victim = others.get(choice - 1);

        AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                .processPayment(victim, collector, DEBT_COLLECTOR_AMOUNT, AssetTransferManager.PaymentMode.USE_MIXED);
        gm.notifyAllObservers(r.getMessage());
    }

    /**
     * Sly Deal: Let the player choose which opponent's property to steal.
     * Cannot steal from a complete set.
     */
    private void handleSlyDealAuto(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        // Collect all stealable properties (not in complete sets) from all opponents
        List<Player> candidateOwners = new ArrayList<>();
        List<PropertyCard> candidateCards = new ArrayList<>();

        for (Player victim : gm.getPlayers()) {
            if (victim == thief) continue;
            for (PropertySet set : victim.getPropertyArea().getSets()) {
                if (set.isComplete()) continue; // Cannot steal from a complete set
                for (PropertyCard pc : set.getCards()) {
                    candidateOwners.add(victim);
                    candidateCards.add(pc);
                }
            }
        }

        if (candidateCards.isEmpty()) {
            gm.notifyAllObservers("Sly Deal: no stealable property available (all sets are complete or empty).");
            return;
        }

        // Display options to the player
        System.out.println();
        System.out.println("=== Sly Deal: Choose a property to steal ===");
        for (int i = 0; i < candidateCards.size(); i++) {
            PropertyCard pc = candidateCards.get(i);
            Player owner = candidateOwners.get(i);
            System.out.println("  " + (i + 1) + ". " + owner.getName() + "'s [" + pc.getName() + "] (" + pc.getColor() + ")");
        }
        System.out.println("  0. Cancel (waste this action)");

        int choice = readChoice(0, candidateCards.size());
        if (choice == 0) {
            gm.notifyAllObservers(thief.getName() + " cancelled Sly Deal.");
            return;
        }

        Player victim = candidateOwners.get(choice - 1);
        PropertyCard stolen = candidateCards.get(choice - 1);

        // Handle Just Say No defense
        if (handleJustSayNo(victim, thief, played)) {
            return;
        }

        victim.getPropertyArea().remove(stolen);
        thief.getPropertyArea().add(stolen);
        gm.notifyAllObservers(thief.getName() + " stole [" + stolen.getName() + "] from " + victim.getName() + " with Sly Deal.");
    }

    /**
     * Forced Deal: Player chooses one of their own properties (not from a complete set)
     * and one opponent's property (not from a complete set) to swap.
     */
    private void handleForcedDealStub(Player player, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        // Step 1: Collect player's own swappable properties (not in complete sets)
        List<PropertyCard> myCards = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (set.isComplete()) continue;
            myCards.addAll(set.getCards());
        }

        if (myCards.isEmpty()) {
            gm.notifyAllObservers("Forced Deal: " + player.getName() + " has no swappable property (all sets complete or empty).");
            return;
        }

        // Step 2: Collect opponent's swappable properties (not in complete sets)
        List<Player> opponentOwners = new ArrayList<>();
        List<PropertyCard> opponentCards = new ArrayList<>();
        for (Player other : gm.getPlayers()) {
            if (other == player) continue;
            for (PropertySet set : other.getPropertyArea().getSets()) {
                if (set.isComplete()) continue;
                for (PropertyCard pc : set.getCards()) {
                    opponentOwners.add(other);
                    opponentCards.add(pc);
                }
            }
        }

        if (opponentCards.isEmpty()) {
            gm.notifyAllObservers("Forced Deal: no opponent has swappable property.");
            return;
        }

        // Step 3: Let player choose their own property to give
        System.out.println();
        System.out.println("=== Forced Deal: Choose YOUR property to give away ===");
        for (int i = 0; i < myCards.size(); i++) {
            PropertyCard pc = myCards.get(i);
            System.out.println("  " + (i + 1) + ". [" + pc.getName() + "] (" + pc.getColor() + ")");
        }
        System.out.println("  0. Cancel (waste this action)");

        int myChoice = readChoice(0, myCards.size());
        if (myChoice == 0) {
            gm.notifyAllObservers(player.getName() + " cancelled Forced Deal.");
            return;
        }
        PropertyCard myCard = myCards.get(myChoice - 1);

        // Step 4: Let player choose opponent's property to take
        System.out.println();
        System.out.println("=== Forced Deal: Choose an OPPONENT's property to take ===");
        for (int i = 0; i < opponentCards.size(); i++) {
            PropertyCard pc = opponentCards.get(i);
            Player owner = opponentOwners.get(i);
            System.out.println("  " + (i + 1) + ". " + owner.getName() + "'s [" + pc.getName() + "] (" + pc.getColor() + ")");
        }
        System.out.println("  0. Cancel (waste this action)");

        int theirChoice = readChoice(0, opponentCards.size());
        if (theirChoice == 0) {
            gm.notifyAllObservers(player.getName() + " cancelled Forced Deal.");
            return;
        }
        Player victim = opponentOwners.get(theirChoice - 1);
        PropertyCard theirCard = opponentCards.get(theirChoice - 1);

        // Step 5: Handle Just Say No defense
        if (handleJustSayNo(victim, player, played)) {
            return;
        }

        // Step 6: Perform the swap
        player.getPropertyArea().remove(myCard);
        victim.getPropertyArea().remove(theirCard);
        player.getPropertyArea().add(theirCard);
        victim.getPropertyArea().add(myCard);

        gm.notifyAllObservers(player.getName() + " swapped [" + myCard.getName() + "] for "
                + victim.getName() + "'s [" + theirCard.getName() + "] with Forced Deal.");
    }

    /**
     * DealBreaker: Let the player choose which opponent's complete set to steal.
     */
    private void handleDealBreakerAuto(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        // Collect all complete sets from opponents
        List<Player> candidateOwners = new ArrayList<>();
        List<PropertySet> candidateSets = new ArrayList<>();

        for (Player victim : gm.getPlayers()) {
            if (victim == thief) continue;
            for (PropertySet set : victim.getPropertyArea().getSets()) {
                if (set.isComplete()) {
                    candidateOwners.add(victim);
                    candidateSets.add(set);
                }
            }
        }

        if (candidateSets.isEmpty()) {
            gm.notifyAllObservers("Deal Breaker: no opponent has a complete set to steal.");
            return;
        }

        // Display options
        System.out.println();
        System.out.println("=== Deal Breaker: Choose a complete set to steal ===");
        for (int i = 0; i < candidateSets.size(); i++) {
            PropertySet set = candidateSets.get(i);
            Player owner = candidateOwners.get(i);
            System.out.println("  " + (i + 1) + ". " + owner.getName() + "'s " + set.getColor()
                    + " set (" + set.getCards().size() + " cards)");
        }
        System.out.println("  0. Cancel (waste this action)");

        int choice = readChoice(0, candidateSets.size());
        if (choice == 0) {
            gm.notifyAllObservers(thief.getName() + " cancelled Deal Breaker.");
            return;
        }

        Player victim = candidateOwners.get(choice - 1);
        PropertySet targetSet = candidateSets.get(choice - 1);

        // Handle Just Say No defense
        if (handleJustSayNo(victim, thief, played)) {
            return;
        }

        // Steal the entire set
        List<PropertyCard> cards = new ArrayList<>(targetSet.getCards());
        for (PropertyCard pc : cards) {
            victim.getPropertyArea().remove(pc);
            thief.getPropertyArea().add(pc);
        }
        gm.notifyAllObservers(thief.getName() + " stole a complete " + targetSet.getColor()
                + " set from " + victim.getName() + " with Deal Breaker.");
    }


    /**
     * Handles Double Rent and Rent cards.
     * - Double The Rent: marks the current turn so the next rent is doubled.
     * - Rent card: player chooses a set to collect rent on, and opponent(s) to charge.
     */
    private void handleDoubleRentOrRentStub(Player player, ActionCard card) {
        GameManager gm = gameLogic.getGameManager();

        // If it's a "Double The Rent" card, mark double rent active for this turn
        if ("Double The Rent".equals(card.getName())) {
            gameLogic.setDoubleRentActive(true);
            gm.notifyAllObservers(player.getName() + " played Double The Rent! Next rent this turn will be doubled.");
            return;
        }

        // It's a Rent card — player chooses which set to collect rent on
        List<PropertySet> rentableSets = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (!set.getCards().isEmpty()) {
                rentableSets.add(set);
            }
        }

        if (rentableSets.isEmpty()) {
            gm.notifyAllObservers(player.getName() + " played " + card.getName() + " but has no properties to collect rent on.");
            return;
        }

        // Let player choose which set to charge rent for
        System.out.println();
        System.out.println("=== Rent: Choose which property set to charge rent for ===");
        for (int i = 0; i < rentableSets.size(); i++) {
            PropertySet set = rentableSets.get(i);
            int rentValue = set.getRent();
            System.out.println("  " + (i + 1) + ". " + set.getColor() + " (" + set.getCards().size()
                    + "/" + set.getNeed() + ") - Rent: " + rentValue + "M");
        }
        System.out.println("  0. Cancel (waste this action)");

        int setChoice = readChoice(0, rentableSets.size());
        if (setChoice == 0) {
            gm.notifyAllObservers(player.getName() + " cancelled Rent.");
            return;
        }
        PropertySet chosenSet = rentableSets.get(setChoice - 1);

        // Determine if this is an "Any Rent" (charge one player) or standard rent (charge all)
        boolean isAnyRent = card.getName() != null && card.getName().contains("Any");

        // Calculate rent amount (with building bonus and possible double rent)
        int baseRent = chosenSet.getRent() + getBuildingBonusForSet(chosenSet);
        boolean isDoubleRent = gameLogic.isDoubleRentActive();
        int finalRent = isDoubleRent ? baseRent * 2 : baseRent;

        // Reset double rent flag after use
        if (isDoubleRent) {
            gameLogic.setDoubleRentActive(false);
        }

        if (isAnyRent) {
            // Any Rent: choose one player to charge
            List<Player> others = new ArrayList<>();
            for (Player p : gm.getPlayers()) {
                if (p != player) others.add(p);
            }
            if (others.isEmpty()) {
                gm.notifyAllObservers("Rent: no other players.");
                return;
            }

            System.out.println();
            System.out.println("=== Rent (" + finalRent + "M): Choose a player to charge ===");
            for (int i = 0; i < others.size(); i++) {
                Player p = others.get(i);
                System.out.println("  " + (i + 1) + ". " + p.getName() + " (Bank: " + p.getBankArea().total() + "M)");
            }

            int targetChoice = readChoice(1, others.size());
            Player target = others.get(targetChoice - 1);

            // Handle Just Say No
            if (handleJustSayNo(target, player, card)) {
                return;
            }

            gm.notifyAllObservers(player.getName() + " charges " + target.getName() + " rent of " + finalRent
                    + "M on " + chosenSet.getColor() + " set." + (isDoubleRent ? " (DOUBLED!)" : ""));
            AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                    .processPayment(target, player, finalRent, AssetTransferManager.PaymentMode.USE_MIXED);
            gm.notifyAllObservers(r.getMessage());
        } else {
            // Standard Rent: charge ALL other players
            gm.notifyAllObservers(player.getName() + " charges ALL players rent of " + finalRent
                    + "M on " + chosenSet.getColor() + " set." + (isDoubleRent ? " (DOUBLED!)" : ""));

            for (Player target : gm.getPlayers()) {
                if (target == player) continue;

                // Each target can use Just Say No
                if (handleJustSayNo(target, player, card)) {
                    continue;
                }

                AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                        .processPayment(target, player, finalRent, AssetTransferManager.PaymentMode.USE_MIXED);
                gm.notifyAllObservers(r.getMessage());
            }
        }
    }

    private int getBuildingBonusForSet(PropertySet set) {
        int bonus = 0;
        for (PropertyCard pc : set.getCards()) {
            for (Card u : pc.getUpgrades()) {
                if (u instanceof ActionCard) {
                    ActionType t = ((ActionCard) u).getType();
                    if (t == ActionType.HOUSE) bonus += 3;
                    else if (t == ActionType.HOTEL) bonus += 4;
                }
            }
        }
        return bonus;
    }
    /**
     * Attaches a House or Hotel from the player's hand to the first legal property set.
     * @return true if the building was attached
     */
    public boolean tryAttachBuilding(Player player, ActionCard building) {
        RuleValidator rules = gameLogic.getRuleValidator();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            boolean ok = building.getType() == ActionType.HOUSE ? rules.canAddHouse(set) : rules.canAddHotel(set);
            if (ok && !set.getCards().isEmpty()) {
                PropertyCard host = set.getCards().get(0);
                host.attachUpgrade(building);
                gameLogic.getGameManager().notifyAllObservers(player.getName() + " placed "
                        + building.getName() + " on " + set.getColor() + " set.");
                return true;
            }
        }
        gameLogic.getGameManager().notifyAllObservers(player.getName() + " could not place " + building.getName() + " (no valid set).");
        return false;
    }
    // ============================= INPUT HELPER =============================
    /**
     * Reads a valid integer choice from the player within [min, max] range.
     */
    private int readChoice(int min, int max) {
        while (true) {
            System.out.print("Enter choice (" + min + "-" + max + "): ");
            String line = scanner.nextLine();
            if (line == null) line = "";
            line = line.trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) {
                    return val;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
        }
    }
}

