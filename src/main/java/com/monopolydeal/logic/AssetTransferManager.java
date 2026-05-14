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
        if (payer == null || receiver == null || requiredAmount <= 0) {
            return new PaymentResult(false, 0, Collections.emptyList(), "Invalid payment request.");
        }
        if (!canAfford(payer, requiredAmount)) {
            return new PaymentResult(false, 0, Collections.emptyList(), payer.getName() + " cannot afford " + requiredAmount + "M.");
        }

        List<Card> eligible = getEligibleAssets(payer, paymentMode);
        List<Card> selected = selectOptimalPaymentAssets(eligible, requiredAmount);
        if (selected.isEmpty()) {
            return new PaymentResult(false, 0, Collections.emptyList(), "No eligible assets for this payment mode.");
        }
        int totalPaid = selected.stream().mapToInt(Card::getValue).sum();
        executeTransfer(payer, receiver, selected);
        return new PaymentResult(true, totalPaid, selected,
                payer.getName() + " paid " + totalPaid + "M to " + receiver.getName() + " (owed " + requiredAmount + "M, no change).");
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
        if (sum < requiredAmount) {
            return Collections.emptyList();
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
            props.addAll(new ArrayList<>(set.getCards()));
        }
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
