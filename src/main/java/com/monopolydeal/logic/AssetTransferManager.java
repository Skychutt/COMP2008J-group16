package com.monopolydeal.logic;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the logic for moving assets (money and properties) between players.
 * Follows the "no change" rule for payments in Monopoly Deal.
 */
public class AssetTransferManager {

    /**
     * Processes a full payment flow from one player to another.
     * Validates payment, selects assets, and performs the transfer.
     * @param payer the player making the payment
     * @param receiver the player receiving the payment
     * @param requiredAmount the total amount of money value required
     * @param paymentMode specifies if the payer uses money only, property only, or mixed
     * @return a result object indicating success, amount paid, and assets used
     */
    public PaymentResult processPayment(Player payer, Player receiver, int requiredAmount, PaymentMode paymentMode) {
        return processPayment(payer, receiver, requiredAmount, paymentMode, null);
    }

    /**
     * Processes payment and allows an optional payer-driven card selection strategy.
     * When the chooser returns an invalid/insufficient selection, fallback selection is used
     * so payment still follows game rules.
     */
    public PaymentResult processPayment(
            Player payer,
            Player receiver,
            int requiredAmount,
            PaymentMode paymentMode,
            PaymentChooser chooser
    ) {
        if (payer == null || receiver == null || requiredAmount <= 0) {
            return new PaymentResult(false, 0, Collections.emptyList(), "Invalid payment request.");
        }

        List<Card> eligible = getEligibleAssets(payer, paymentMode);
        List<Card> selected = choosePaymentAssets(payer, receiver, eligible, requiredAmount, chooser);
        if (selected.isEmpty()) {
            return new PaymentResult(true, 0, Collections.emptyList(),
                    payer.getName() + " has no payable assets and pays 0M to " + receiver.getName()
                            + " (owed " + requiredAmount + "M).");
        }
        int totalPaid = selected.stream().mapToInt(Card::getValue).sum();
        executeTransfer(payer, receiver, selected);
        if (totalPaid < requiredAmount) {
            return new PaymentResult(true, totalPaid, selected,
                    payer.getName() + " could only pay " + totalPaid + "M to " + receiver.getName()
                            + " (owed " + requiredAmount + "M).");
        }
        return new PaymentResult(true, totalPaid, selected,
                payer.getName() + " paid " + totalPaid + "M to " + receiver.getName()
                        + " (owed " + requiredAmount + "M, no change).");
    }

    private List<Card> choosePaymentAssets(
            Player payer,
            Player receiver,
            List<Card> eligible,
            int requiredAmount,
            PaymentChooser chooser
    ) {
        if (eligible == null || eligible.isEmpty()) {
            return Collections.emptyList();
        }

        List<Card> bankCards = new ArrayList<>();
        List<Card> propertyCards = new ArrayList<>();
        for (Card card : eligible) {
            if (card instanceof PropertyCard) {
                propertyCards.add(card);
            } else {
                bankCards.add(card);
            }
        }

        int bankTotal = totalValue(bankCards);
        if (bankTotal >= requiredAmount) {
            return chooseFromSubset(payer, receiver, bankCards, requiredAmount, chooser);
        }

        List<Card> selected = new ArrayList<>(bankCards);
        int remaining = requiredAmount - bankTotal;
        if (remaining <= 0 || propertyCards.isEmpty()) {
            return selected;
        }

        selected.addAll(chooseFromSubset(payer, receiver, propertyCards, remaining, chooser));
        return selected;
    }

    private List<Card> chooseFromSubset(
            Player payer,
            Player receiver,
            List<Card> subset,
            int requiredAmount,
            PaymentChooser chooser
    ) {
        if (subset == null || subset.isEmpty()) {
            return Collections.emptyList();
        }

        List<Card> chosen = null;
        if (chooser != null) {
            chosen = chooser.choose(
                    payer,
                    receiver,
                    Collections.unmodifiableList(new ArrayList<>(subset)),
                    requiredAmount
            );
        }

        List<Card> normalized = normalizeSelection(subset, chosen);
        int availableTotal = totalValue(subset);
        int chosenTotal = totalValue(normalized);

        if (normalized.isEmpty()) {
            return selectOptimalPaymentAssets(subset, requiredAmount);
        }
        if (availableTotal < requiredAmount) {
            return new ArrayList<>(subset);
        }
        if (chosenTotal < requiredAmount) {
            return selectOptimalPaymentAssets(subset, requiredAmount);
        }
        return normalized;
    }

    private List<Card> normalizeSelection(List<Card> eligible, List<Card> chosen) {
        if (chosen == null || chosen.isEmpty()) {
            return Collections.emptyList();
        }
        List<Card> remaining = new ArrayList<>(eligible);
        List<Card> normalized = new ArrayList<>();
        for (Card c : chosen) {
            if (c == null) {
                continue;
            }
            if (remaining.remove(c)) {
                normalized.add(c);
            }
        }
        return normalized;
    }

    private int totalValue(List<Card> cards) {
        int total = 0;
        if (cards == null) {
            return 0;
        }
        for (Card card : cards) {
            if (card != null) {
                total += card.getValue();
            }
        }
        return total;
    }

    /**
     * Checks if a player has enough total assets to cover the required payment amount.
     * @param player the player to check
     * @param requiredAmount the amount to cover
     * @return true if the player can afford the payment
     */
    public boolean canAfford(Player player, int requiredAmount) {
        if (player == null || requiredAmount <= 0) {
            return true;
        }
        int total = 0;
        for (Card c : player.getBankArea().getMoney()) {
            total += c.getValue();
        }
        for (PropertySet set : player.getPropertyArea().getSets()) {
            for (PropertyCard pc : set.getCards()) {
                total += pc.getValue();
            }
        }
        return total >= requiredAmount;
    }

    /**
     * Selects assets to cover the required amount (greedy ascending value to limit overpay).
     * @param availableAssets list of all assets the player can use for payment
     * @param requiredAmount the minimum total value needed
     * @return a list of selected assets to pay with
     */
    public List<Card> selectOptimalPaymentAssets(List<Card> availableAssets, int requiredAmount) {
        if (availableAssets == null || availableAssets.isEmpty()) {
            return Collections.emptyList();
        }
        List<Card> sorted = new ArrayList<>(availableAssets);
        sorted.sort(Comparator.comparingInt(Card::getValue));
        List<Card> chosen = new ArrayList<>();
        int sum = 0;
        for (Card c : sorted) {
            if (sum >= requiredAmount) {
                break;
            }
            chosen.add(c);
            sum += c.getValue();
        }
        return chosen;
    }

    /**
     * Executes the transfer of selected assets from the payer to the receiver.
     * @param from the payer
     * @param to the receiver
     * @param assetsToTransfer the list of assets to move
     */
    private void executeTransfer(Player from, Player to, List<Card> assetsToTransfer) {
        for (Card c : assetsToTransfer) {
            from.getBankArea().remove(c);
            from.getPropertyArea().remove(c);
        }
        List<Card> copy = new ArrayList<>(assetsToTransfer);
        to.receivePayment(copy);
    }

    /**
     * Filters the player's assets based on the chosen payment mode (money, property, or mixed).
     * @param player the payer
     * @param mode the selected payment mode
     * @return a filtered list of cards eligible for payment
     */
    private List<Card> getEligibleAssets(Player player, PaymentMode mode) {
        List<Card> bank = new ArrayList<>(player.getBankArea().getMoney());
        List<Card> props = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            for (PropertyCard pc : set.getCards()) {
                if (pc.canBeUsedAsPayment()) {
                    props.add(pc);
                }
            }
        }
        bank.removeIf(c -> c == null || c.getValue() <= 0);
        props.removeIf(c -> c == null || c.getValue() <= 0);
        switch (mode) {
            case USE_MONEY_ONLY:
                return bank;
            case USE_PROPERTY_ONLY:
                return props;
            case USE_MIXED:
            default:
                List<Card> all = new ArrayList<>(bank);
                all.addAll(props);
                return all;
        }
    }

    /**
     * Inner enum representing the payment selection mode.
     */
    public enum PaymentMode {
        USE_MONEY_ONLY,
        USE_PROPERTY_ONLY,
        USE_MIXED
    }

    /** Callback used by UI/input layers when payer chooses which cards to give. */
    @FunctionalInterface
    public interface PaymentChooser {
        List<Card> choose(Player payer, Player receiver, List<Card> eligibleAssets, int requiredAmount);
    }

    /**
     * Inner class representing the result of a payment operation.
     */
    public static class PaymentResult {
        private final boolean success;
        private final int totalPaid;
        private final List<Card> assetsUsed;
        private final String message;

        public PaymentResult(boolean success, int totalPaid, List<Card> assetsUsed, String message) {
            this.success = success;
            this.totalPaid = totalPaid;
            this.assetsUsed = assetsUsed;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getTotalPaid() { return totalPaid; }
        public List<Card> getAssetsUsed() { return assetsUsed; }
        public String getMessage() { return message; }
    }
}
