package com.monopolydeal.logic;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import com.monopolydeal.gui.theme.ThemedDialog;

import javafx.stage.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Handles the execution of all action card effects.
 */
public class ActionHandler {

    private static final int BIRTHDAY_PAYMENT = 2;
    private static final int DEBT_COLLECTOR_AMOUNT = 5;

    private final GameLogic gameLogic;
    private Scanner scanner;
    private boolean useDialogInput;
    private boolean useRemoteDecisions;
    private Window dialogOwner;
    private DecisionResolver decisionResolver;
    private Player activeDecisionPlayer;
    private Player preferredTargetPlayer;

    public ActionHandler(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        this.scanner = new Scanner(System.in);
        this.useDialogInput = false;
    }

    /** Refresh the scanner to pick up changes to System.in (useful for testing). */
    public void refreshScanner() {
        this.scanner = new Scanner(System.in);
    }

    /** Switch between console input and dialog input (GUI mode). */
    public void setUseDialogInput(boolean useDialogInput) {
        this.useDialogInput = useDialogInput;
    }

    /** @return true when dialogs are used for player decisions */
    public boolean isUseDialogInput() {
        return useDialogInput;
    }

    /** LAN server: route human decisions to remote clients instead of stdin/dialogs. */
    public void setUseRemoteDecisions(boolean useRemoteDecisions) {
        this.useRemoteDecisions = useRemoteDecisions;
    }

    /** Parent window for themed in-game choice dialogs. */
    public void setDialogOwner(Window dialogOwner) {
        this.dialogOwner = dialogOwner;
    }

    /** Registers the AI decision resolver used during automated choices. */
    public void setDecisionResolver(DecisionResolver decisionResolver) {
        this.decisionResolver = decisionResolver;
    }

    /** Marks which player is currently making an interactive choice. */
    public void setActiveDecisionPlayer(Player player) {
        this.activeDecisionPlayer = player;
    }

    /** Clears the active decision player after an AI action completes. */
    public void clearActiveDecisionPlayer() {
        this.activeDecisionPlayer = null;
    }

    /** Prefers one explicit opponent target for UI drag-and-drop plays. */
    public void setPreferredTargetPlayer(Player player) {
        this.preferredTargetPlayer = player;
    }

    /** Clears the explicit UI target after the card resolves or is cancelled. */
    public void clearPreferredTargetPlayer() {
        this.preferredTargetPlayer = null;
    }

    /**
     * Executes the effect of a given action card.
     * @param player the player playing the card
     * @param card the action card to execute
     */
    public boolean executeAction(Player player, ActionCard card) {
        if (player == null || card == null) {
            return true;
        }
        switch (card.getType()) {
            case GO_PASS:
                handlePassGo(player);
                return true;
            case BIRTHDAY:
                handleBirthday(player, card);
                return true;
            case DEBT_DEAL:
                return handleDebtCollector(player, card);
            case SLY_DEAL:
                return handleSlyDeal(player, card);
            case FORCED_DEAL:
                return handleForcedDeal(player, card);
            case DEAL_BREAKER:
                return handleDealBreaker(player, card);
            case RENT:
                return handleDoubleRentOrRent(player, card);
            case DOUBLE_RENT:
                return handleDoubleRentOrRent(player, card);
            case JUST_SAY_NO:
                return true;
            default:
                return true;
        }
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

        GameManager gm = gameLogic.getGameManager();
        Player responder = defender;
        Player challenger = attacker;
        boolean blocked = false;

        while (responder != null) {
            Card js = findJustSayNo(responder);
            if (js == null) {
                break;
            }

            boolean useCounter;
            if (responder.isAI() && decisionResolver instanceof com.monopolydeal.ai.BotDecisionPolicy) {
                useCounter = ((com.monopolydeal.ai.BotDecisionPolicy) decisionResolver)
                        .wantsJustSayNoCounter(responder);
            } else {
                useCounter = askUseJustSayNo(responder, attackCard, blocked, challenger);
            }
            if (!useCounter) {
                break;
            }

            responder.getHand().removeCard(js.getId());
            Deck.getInstance().addToDiscard(js);

            if (!blocked) {
                String challengerName = challenger != null ? challenger.getName() : "an opponent";
                gm.notifyAllObservers(responder.getName() + " blocked " + challengerName
                        + "'s [" + attackCard.getName() + "] with Just Say No!");
            } else {
                String challengerName = challenger != null ? challenger.getName() : "the previous player";
                gm.notifyAllObservers(responder.getName() + " countered " + challengerName
                        + "'s Just Say No with another Just Say No!");
            }

            blocked = !blocked;
            Player temp = responder;
            responder = challenger;
            challenger = temp;
        }

        return blocked;
    }

    private Card findJustSayNo(Player defender) {
        for (Card c : defender.getHand().getCards()) {
            if (c instanceof ActionCard && ((ActionCard) c).getType() == ActionType.JUST_SAY_NO) {
                return c;
            }
        }
        return null;
    }

    /**
     * Ask whether a player wants to use Just Say No.
     * First response blocks the attack, next response counter-blocks, and so on.
     */
    private boolean askUseJustSayNo(Player responder, ActionCard attackCard, boolean attackCurrentlyBlocked, Player otherSide) {
        String attackName = attackCard.getName() == null ? "this action" : attackCard.getName();
        String prompt;
        if (!attackCurrentlyBlocked) {
            String attackerName = otherSide != null ? otherSide.getName() : "the opponent";
            prompt = responder.getName() + ": " + attackerName + " played [" + attackName + "]. Use Just Say No?";
        } else {
            String previousName = otherSide != null ? otherSide.getName() : "the other player";
            prompt = responder.getName() + ": " + previousName
                    + " just used Just Say No. Use yours to counter?";
        }

        List<String> options = new ArrayList<>();
        options.add("Use Just Say No");
        options.add("Do Not Use");

        int choice = chooseOptionFor(
                responder,
                "Just Say No",
                prompt,
                options,
                false
        );

        return choice == 0;
    }

    public void handlePassGo(Player player) {
        List<Card> extra = Deck.getInstance().draw(2);
        for (Card c : extra) {
            player.getHand().add(c);
        }
        gameLogic.getGameManager().notifyAllObservers(player.getName() + " drew " + extra.size() + " extra card(s) from Pass Go.");
    }

    /**
     * Enforce end-turn hand limit by letting the player choose which cards to discard.
     * Rule reference: if hand has more than 7 cards at end of turn, discard extras.
     */
    public void enforceEndTurnDiscard(Player player) {
        if (player == null) {
            return;
        }
        GameManager gm = gameLogic.getGameManager();

        while (player.getHand().size() > Player.MAX_HAND_SIZE) {
            List<Card> handCards = new ArrayList<>(player.getHand().getCards());
            List<String> options = new ArrayList<>();
            for (Card card : handCards) {
                options.add("[" + card.getName() + "] (" + card.getValue() + "M) #ID " + card.getId());
            }

            int needDiscard = player.getHand().size() - Player.MAX_HAND_SIZE;
            int index = chooseOptionFor(
                    player,
                    "Discard To 7 Cards",
                    player.getName() + " must discard " + needDiscard + " more card(s):",
                    options,
                    false
            );

            if (index < 0 || index >= handCards.size()) {
                index = 0;
            }

            Card discard = handCards.get(index);
            player.getHand().removeCard(discard.getId());
            Deck.getInstance().addToDiscard(discard);
            gm.notifyAllObservers(player.getName() + " discarded [" + discard.getName() + "] to discard pile (hand limit).");
        }
    }

    public void handleRent(Player collector, Player target, int amount) {
        AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                target,
                collector,
                amount,
                "Rent"
        );
        gameLogic.getGameManager().notifyAllObservers(r.getMessage());
    }

    public void handleBirthday(Player initiator, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();
        for (Player other : getOpponents(initiator)) {
            if (played != null && handleJustSayNo(other, initiator, played)) {
                continue;
            }
            AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                    other,
                    initiator,
                    BIRTHDAY_PAYMENT,
                    "Birthday"
            );
            gm.notifyAllObservers(r.getMessage());
        }
        checkVictoryAfterStateChange();
    }

    private AssetTransferManager.PaymentResult processPaymentWithChoice(
            Player payer,
            Player receiver,
            int requiredAmount,
            String reason
    ) {
        return gameLogic.getAssetTransferManager().processPayment(
                payer,
                receiver,
                requiredAmount,
                AssetTransferManager.PaymentMode.USE_MIXED,
                (from, to, eligibleAssets, amount) -> choosePaymentAssets(from, to, eligibleAssets, amount, reason)
        );
    }

    private List<Card> choosePaymentAssets(
            Player payer,
            Player receiver,
            List<Card> eligibleAssets,
            int requiredAmount,
            String reason
    ) {
        if (eligibleAssets == null || eligibleAssets.isEmpty()) {
            return new ArrayList<>();
        }
        if (payer != null && payer.isAI()) {
            return gameLogic.getAssetTransferManager()
                    .selectOptimalPaymentAssets(eligibleAssets, requiredAmount);
        }

        List<Card> remaining = new ArrayList<>(eligibleAssets);
        List<Card> chosen = new ArrayList<>();
        int paid = 0;
        int affordable = 0;
        for (Card c : eligibleAssets) {
            affordable += c.getValue();
        }

        while (!remaining.isEmpty()) {
            if (paid >= requiredAmount) {
                break;
            }

            List<String> options = new ArrayList<>();
            for (Card c : remaining) {
                options.add(formatPaymentOption(c));
            }

            String prompt = payer.getName() + " pays " + receiver.getName()
                    + " for " + reason + ". Owed: " + requiredAmount + "M, selected: " + paid + "M.";

            int index = chooseOptionFor(payer, "Choose Payment Card", prompt, options, true);
            if (index < 0) {
                break;
            }
            if (index >= remaining.size()) {
                continue;
            }

            Card selected = remaining.remove(index);
            chosen.add(selected);
            paid += selected.getValue();
        }

        // If the payer cancels early but still cannot cover the debt, fall back to the full legal set.
        if (affordable < requiredAmount && paid < affordable) {
            chosen.clear();
            chosen.addAll(eligibleAssets);
        } else if (affordable >= requiredAmount && paid < requiredAmount) {
            return gameLogic.getAssetTransferManager().selectOptimalPaymentAssets(eligibleAssets, requiredAmount);
        }

        return chosen;
    }

    private String formatPaymentOption(Card card) {
        if (card instanceof PropertyCard) {
            PropertyCard pc = (PropertyCard) card;
            return "[Property] " + card.getName() + " (" + pc.getColor() + ") - " + card.getValue() + "M";
        }
        return "[Bank] " + card.getName() + " - " + card.getValue() + "M";
    }

    private boolean handleDebtCollector(Player collector, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();
        List<Player> others = getOpponents(collector);

        if (others.isEmpty()) {
            return finish(gm, "Debt Collector: no other players.");
        }

        if (preferredTargetPlayer != null) {
            if (!others.contains(preferredTargetPlayer)) {
                return cancel(gm, collector.getName() + " cancelled Debt Collector.");
            }
            if (played != null && handleJustSayNo(preferredTargetPlayer, collector, played)) {
                return true;
            }
            AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                    preferredTargetPlayer,
                    collector,
                    DEBT_COLLECTOR_AMOUNT,
                    "Debt Collector"
            );
            gm.notifyAllObservers(r.getMessage());
            checkVictoryAfterStateChange();
            return true;
        }

        List<String> options = new ArrayList<>();
        for (Player p : others) {
            options.add(p.getName() + " (Bank: " + p.getBankArea().total() + "M)");
        }

        int index = chooseOption(
                "Debt Collector",
                "Choose one player to charge 5M:",
                options,
                true
        );

        if (index < 0) {
            return cancel(gm, collector.getName() + " cancelled Debt Collector.");
        }

        Player victim = others.get(index);
        if (played != null && handleJustSayNo(victim, collector, played)) {
            return true;
        }
        AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                victim,
                collector,
                DEBT_COLLECTOR_AMOUNT,
                "Debt Collector"
        );
        gm.notifyAllObservers(r.getMessage());
        checkVictoryAfterStateChange();
        return true;
    }

    /**
     * Sly Deal: Let the player choose which opponent's property to steal.
     * Cannot steal from a complete set.
     */
    private boolean handleSlyDeal(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        List<Player> candidateOwners = new ArrayList<>();
        List<PropertyCard> candidateCards = new ArrayList<>();
        for (Player victim : getOpponents(thief)) {
            for (PropertyCard pc : collectNonCompleteCards(victim)) {
                candidateOwners.add(victim);
                candidateCards.add(pc);
            }
        }

        if (candidateCards.isEmpty()) {
            return finish(gm, "Sly Deal: no stealable property available (all sets are complete or empty).");
        }

        if (preferredTargetPlayer != null) {
            List<PropertyCard> targetCards = new ArrayList<>();
            for (int i = 0; i < candidateCards.size(); i++) {
                if (candidateOwners.get(i) == preferredTargetPlayer) {
                    targetCards.add(candidateCards.get(i));
                }
            }
            if (targetCards.isEmpty()) {
                return cancel(gm, thief.getName() + " cancelled Sly Deal.");
            }

            PropertyCard stolen = targetCards.get(0);
            if (targetCards.size() > 1) {
                List<String> targetOptions = new ArrayList<>();
                for (PropertyCard propertyCard : targetCards) {
                    targetOptions.add("[" + propertyCard.getName() + "] (" + propertyCard.getColor() + ")");
                }
                int index = chooseOption(
                        "Sly Deal",
                        "Choose one property to steal from " + preferredTargetPlayer.getName() + ":",
                        targetOptions,
                        true
                );
                if (index < 0) {
                    return cancel(gm, thief.getName() + " cancelled Sly Deal.");
                }
                stolen = targetCards.get(index);
            }

            if (handleJustSayNo(preferredTargetPlayer, thief, played)) {
                return true;
            }

            preferredTargetPlayer.getPropertyArea().remove(stolen);
            thief.getPropertyArea().add(stolen);
            gm.notifyAllObservers(thief.getName() + " stole [" + stolen.getName() + "] from "
                    + preferredTargetPlayer.getName() + " with Sly Deal.");
            checkVictoryAfterStateChange();
            return true;
        }

        List<String> options = new ArrayList<>();
        for (int i = 0; i < candidateCards.size(); i++) {
            PropertyCard pc = candidateCards.get(i);
            Player owner = candidateOwners.get(i);
            options.add(owner.getName() + "'s [" + pc.getName() + "] (" + pc.getColor() + ")");
        }

        int index = chooseOption(
                "Sly Deal",
                "Choose one property to steal:",
                options,
                true
        );

        if (index < 0) {
            return cancel(gm, thief.getName() + " cancelled Sly Deal.");
        }

        Player victim = candidateOwners.get(index);
        PropertyCard stolen = candidateCards.get(index);

        if (handleJustSayNo(victim, thief, played)) {
            return true;
        }

        victim.getPropertyArea().remove(stolen);
        thief.getPropertyArea().add(stolen);
        gm.notifyAllObservers(thief.getName() + " stole [" + stolen.getName() + "] from " + victim.getName() + " with Sly Deal.");
        checkVictoryAfterStateChange();
        return true;
    }

    /**
     * Forced Deal: Player chooses one of their own properties (not from a complete set)
     * and one opponent's property (not from a complete set) to swap.
     */
    private boolean handleForcedDeal(Player player, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        List<PropertyCard> myCards = collectNonCompleteCards(player);

        if (myCards.isEmpty()) {
            return finish(gm, "Forced Deal: " + player.getName() + " has no swappable property (all sets complete or empty).");
        }

        List<Player> opponentOwners = new ArrayList<>();
        List<PropertyCard> opponentCards = new ArrayList<>();
        for (Player other : getOpponents(player)) {
            for (PropertyCard pc : collectNonCompleteCards(other)) {
                opponentOwners.add(other);
                opponentCards.add(pc);
            }
        }

        if (opponentCards.isEmpty()) {
            return finish(gm, "Forced Deal: no opponent has swappable property.");
        }

        if (preferredTargetPlayer != null) {
            List<PropertyCard> targetCards = new ArrayList<>();
            for (int i = 0; i < opponentCards.size(); i++) {
                if (opponentOwners.get(i) == preferredTargetPlayer) {
                    targetCards.add(opponentCards.get(i));
                }
            }
            if (targetCards.isEmpty()) {
                return cancel(gm, player.getName() + " cancelled Forced Deal.");
            }
        }

        List<String> myOptions = new ArrayList<>();
        for (PropertyCard card : myCards) {
            myOptions.add("[" + card.getName() + "] (" + card.getColor() + ")");
        }
        int myIndex = chooseOption(
                "Forced Deal",
                "Choose YOUR property to give away:",
                myOptions,
                true
        );
        if (myIndex < 0) {
            return cancel(gm, player.getName() + " cancelled Forced Deal.");
        }
        PropertyCard myCard = myCards.get(myIndex);

        List<Player> visibleOwners = new ArrayList<>();
        List<PropertyCard> visibleCards = new ArrayList<>();
        for (int i = 0; i < opponentCards.size(); i++) {
            Player owner = opponentOwners.get(i);
            if (preferredTargetPlayer != null && owner != preferredTargetPlayer) {
                continue;
            }
            visibleOwners.add(owner);
            visibleCards.add(opponentCards.get(i));
        }

        if (visibleCards.isEmpty()) {
            return cancel(gm, player.getName() + " cancelled Forced Deal.");
        }

        List<String> otherOptions = new ArrayList<>();
        for (int i = 0; i < visibleCards.size(); i++) {
            PropertyCard pc = visibleCards.get(i);
            Player owner = visibleOwners.get(i);
            otherOptions.add(owner.getName() + "'s [" + pc.getName() + "] (" + pc.getColor() + ")");
        }
        int theirIndex = chooseOption(
                "Forced Deal",
                "Choose an OPPONENT property to take:",
                otherOptions,
                true
        );
        if (theirIndex < 0) {
            return cancel(gm, player.getName() + " cancelled Forced Deal.");
        }

        Player victim = visibleOwners.get(theirIndex);
        PropertyCard theirCard = visibleCards.get(theirIndex);

        if (handleJustSayNo(victim, player, played)) {
            return true;
        }

        player.getPropertyArea().remove(myCard);
        victim.getPropertyArea().remove(theirCard);
        player.getPropertyArea().add(theirCard);
        victim.getPropertyArea().add(myCard);

        gm.notifyAllObservers(player.getName() + " swapped [" + myCard.getName() + "] for "
                + victim.getName() + "'s [" + theirCard.getName() + "] with Forced Deal.");
        checkVictoryAfterStateChange();
        return true;
    }

    /**
     * DealBreaker: Let the player choose which opponent's complete set to steal.
     */
    private boolean handleDealBreaker(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();

        List<Player> candidateOwners = new ArrayList<>();
        List<PropertySet> candidateSets = new ArrayList<>();
        for (Player victim : getOpponents(thief)) {
            for (PropertySet set : collectCompleteSets(victim)) {
                candidateOwners.add(victim);
                candidateSets.add(set);
            }
        }

        if (candidateSets.isEmpty()) {
            return finish(gm, "Deal Breaker: no opponent has a complete set to steal.");
        }

        if (preferredTargetPlayer != null) {
            List<PropertySet> targetSets = new ArrayList<>();
            for (int i = 0; i < candidateSets.size(); i++) {
                if (candidateOwners.get(i) == preferredTargetPlayer) {
                    targetSets.add(candidateSets.get(i));
                }
            }
            if (targetSets.isEmpty()) {
                return cancel(gm, thief.getName() + " cancelled Deal Breaker.");
            }

            PropertySet targetSet = targetSets.get(0);
            if (targetSets.size() > 1) {
                List<String> setOptions = new ArrayList<>();
                for (PropertySet set : targetSets) {
                    setOptions.add(set.getColor() + " set (" + set.getCards().size() + " cards)");
                }
                int index = chooseOption(
                        "Deal Breaker",
                        "Choose a complete set to steal from " + preferredTargetPlayer.getName() + ":",
                        setOptions,
                        true
                );
                if (index < 0) {
                    return cancel(gm, thief.getName() + " cancelled Deal Breaker.");
                }
                targetSet = targetSets.get(index);
            }

            if (handleJustSayNo(preferredTargetPlayer, thief, played)) {
                return true;
            }

            List<PropertyCard> cards = new ArrayList<>(targetSet.getCards());
            for (PropertyCard pc : cards) {
                preferredTargetPlayer.getPropertyArea().remove(pc);
                thief.getPropertyArea().add(pc);
            }
            gm.notifyAllObservers(thief.getName() + " stole a complete " + targetSet.getColor()
                    + " set from " + preferredTargetPlayer.getName() + " with Deal Breaker.");
            checkVictoryAfterStateChange();
            return true;
        }

        List<String> options = new ArrayList<>();
        for (int i = 0; i < candidateSets.size(); i++) {
            PropertySet set = candidateSets.get(i);
            Player owner = candidateOwners.get(i);
            options.add(owner.getName() + "'s " + set.getColor() + " set (" + set.getCards().size() + " cards)");
        }

        int index = chooseOption(
                "Deal Breaker",
                "Choose a complete set to steal:",
                options,
                true
        );

        if (index < 0) {
            return cancel(gm, thief.getName() + " cancelled Deal Breaker.");
        }

        Player victim = candidateOwners.get(index);
        PropertySet targetSet = candidateSets.get(index);

        if (handleJustSayNo(victim, thief, played)) {
            return true;
        }

        List<PropertyCard> cards = new ArrayList<>(targetSet.getCards());
        for (PropertyCard pc : cards) {
            victim.getPropertyArea().remove(pc);
            thief.getPropertyArea().add(pc);
        }
        gm.notifyAllObservers(thief.getName() + " stole a complete " + targetSet.getColor()
                + " set from " + victim.getName() + " with Deal Breaker.");
        checkVictoryAfterStateChange();
        return true;
    }


    /**
     * Handles Double Rent and Rent cards.
     * - Double The Rent: marks the current turn so the next rent is doubled.
     * - Rent card: player chooses a set to collect rent on, and opponent(s) to charge.
     */
    private boolean handleDoubleRentOrRent(Player player, ActionCard card) {
        GameManager gm = gameLogic.getGameManager();

        if ("Double The Rent".equals(card.getName())) {
            gameLogic.addPendingDoubleRent();
            gm.notifyAllObservers(player.getName() + " played Double The Rent! Pending doubles this turn: "
                    + gameLogic.getPendingDoubleRentCount() + ".");
            return true;
        }

        List<PropertySet> rentableSets = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (!set.getCards().isEmpty()
                    && set.getRent() > 0
                    && isSetAllowedByRentCard(set, card)) {
                rentableSets.add(set);
            }
        }

        if (rentableSets.isEmpty()) {
            return finish(gm, player.getName() + " played " + card.getName() + " but has no properties to collect rent on.");
        }

        List<String> setOptions = new ArrayList<>();
        for (PropertySet set : rentableSets) {
            int rentValue = set.getRent();
            setOptions.add(set.getColor() + " (" + set.getCards().size() + "/" + set.getNeed() + ") - Rent: " + rentValue + "M");
        }

        int setIndex = chooseOption(
                "Rent",
                "Choose which property set to charge rent for:",
                setOptions,
                true
        );

        if (setIndex < 0) {
            return cancel(gm, player.getName() + " cancelled Rent.");
        }

        PropertySet chosenSet = rentableSets.get(setIndex);
        mergeFullWildsIntoSetColor(player, chosenSet.getColor());
        PropertySet refreshed = player.getPropertyArea().getSet(chosenSet.getColor());
        if (refreshed != null) {
            chosenSet = refreshed;
        }

        // Determine if this is an "Any Rent" (charge one player) or standard rent (charge all)
        boolean isAnyRent = card.getName() != null && card.getName().contains("Any");

        // Calculate base rent first (multiplier will be consumed only when rent is actually charged).
        int baseRent = chosenSet.getRent() + getBuildingBonusForSet(chosenSet);

        if (isAnyRent) {
            List<Player> others = getOpponents(player);
            if (others.isEmpty()) {
                return finish(gm, "Rent: no other players.");
            }

             if (preferredTargetPlayer != null && !others.contains(preferredTargetPlayer)) {
                return cancel(gm, player.getName() + " cancelled Rent.");
            }

            List<String> targetOptions = new ArrayList<>();
            for (Player p : others) {
                targetOptions.add(p.getName() + " (Bank: " + p.getBankArea().total() + "M)");
            }
            int previewMultiplier = previewRentMultiplier();
            int previewRent = baseRent * previewMultiplier;

            Player target;
            if (preferredTargetPlayer != null) {
                target = preferredTargetPlayer;
            } else {
                int targetIndex = chooseOption(
                        "Rent",
                        "Rent amount: " + previewRent + "M. Choose a player to charge:",
                        targetOptions,
                        true
                );

                if (targetIndex < 0) {
                    return cancel(gm, player.getName() + " cancelled Rent.");
                }
                target = others.get(targetIndex);
            }
            int rentMultiplier = gameLogic.consumeRentMultiplier();
            int finalRent = baseRent * rentMultiplier;
            boolean doubled = rentMultiplier > 1;

            if (handleJustSayNo(target, player, card)) {
                return true;
            }

            gm.notifyAllObservers(player.getName() + " charges " + target.getName() + " rent of " + finalRent
                    + "M on " + chosenSet.getColor() + " set." + (doubled ? " (x" + rentMultiplier + ")" : ""));
            AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                    target,
                    player,
                    finalRent,
                    "Rent"
            );
            gm.notifyAllObservers(r.getMessage());
        } else {
            int rentMultiplier = gameLogic.consumeRentMultiplier();
            int finalRent = baseRent * rentMultiplier;
            boolean doubled = rentMultiplier > 1;

            gm.notifyAllObservers(player.getName() + " charges ALL players rent of " + finalRent
                    + "M on " + chosenSet.getColor() + " set." + (doubled ? " (x" + rentMultiplier + ")" : ""));

            for (Player target : getOpponents(player)) {
                if (handleJustSayNo(target, player, card)) {
                    continue;
                }

                AssetTransferManager.PaymentResult r = processPaymentWithChoice(
                        target,
                        player,
                        finalRent,
                        "Rent"
                );
                gm.notifyAllObservers(r.getMessage());
            }
        }
        checkVictoryAfterStateChange();
        return true;
    }

    private void checkVictoryAfterStateChange() {
        gameLogic.checkGameOver();
    }

    /** Return every opponent except the current player. */
    private List<Player> getOpponents(Player current) {
        List<Player> opponents = new ArrayList<>();
        GameManager gm = gameLogic.getGameManager();
        if (current == null || gm == null || gm.getPlayers() == null) {
            return opponents;
        }
        for (Player player : gm.getPlayers()) {
            if (player != null && player != current) {
                opponents.add(player);
            }
        }
        return opponents;
    }

    /** Collect cards that can be targeted because they are not inside complete sets. */
    private List<PropertyCard> collectNonCompleteCards(Player player) {
        List<PropertyCard> cards = new ArrayList<>();
        if (player == null) {
            return cards;
        }
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (!set.isComplete()) {
                cards.addAll(set.getCards());
            }
        }
        return cards;
    }

    /** Collect complete sets for Deal Breaker. */
    private List<PropertySet> collectCompleteSets(Player player) {
        List<PropertySet> sets = new ArrayList<>();
        if (player == null) {
            return sets;
        }
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (set.isComplete()) {
                sets.add(set);
            }
        }
        return sets;
    }

    /** Log a normal outcome and keep the action as resolved. */
    private boolean finish(GameManager gm, String message) {
        gm.notifyAllObservers(message);
        return true;
    }

    /** Log a cancelled action so the caller can restore the card to hand. */
    private boolean cancel(GameManager gm, String message) {
        gm.notifyAllObservers(message);
        return false;
    }

    /** Move full-color wilds into the chosen color before rent is calculated. */
    private void mergeFullWildsIntoSetColor(Player player, PropertyType targetColor) {
        if (player == null || targetColor == null || targetColor == PropertyType.RAINBOW) {
            return;
        }

        List<PropertyCard> movable = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (set.getColor() != PropertyType.RAINBOW) {
                continue;
            }
            for (PropertyCard card : set.getCards()) {
                if (card.isFullColorWild()) {
                    movable.add(card);
                }
            }
        }

        for (PropertyCard card : movable) {
            player.getPropertyArea().moveProperty(card, targetColor);
        }
    }

    private boolean isSetAllowedByRentCard(PropertySet set, ActionCard rentCard) {
        if (set == null || rentCard == null) {
            return false;
        }
        if (set.getColor() == PropertyType.RAINBOW) {
            return false;
        }

        String name = rentCard.getName();
        if (name == null) {
            return false;
        }
        if (name.contains("Any")) {
            return true;
        }

        Set<PropertyType> allowed = new HashSet<>();
        if ("Rent Blue/Green".equals(name)) {
            allowed.add(PropertyType.BLUE);
            allowed.add(PropertyType.GREEN);
        } else if ("Rent Red/Yellow".equals(name)) {
            allowed.add(PropertyType.RED);
            allowed.add(PropertyType.YELLOW);
        } else if ("Rent Purple/Orange".equals(name)) {
            allowed.add(PropertyType.PURPLE);
            allowed.add(PropertyType.ORANGE);
        } else if ("Rent Black/LightGreen".equals(name)) {
            allowed.add(PropertyType.BLACK);
            allowed.add(PropertyType.LIGHTGREEN);
        } else if ("Rent Brown/LightBlue".equals(name)) {
            allowed.add(PropertyType.BROWN);
            allowed.add(PropertyType.LIGHTBLUE);
        } else {
            return false;
        }

        return allowed.contains(set.getColor());
    }

    private int previewRentMultiplier() {
        int multiplier = 1;
        for (int i = 0; i < gameLogic.getPendingDoubleRentCount(); i++) {
            multiplier *= 2;
        }
        return multiplier;
    }

    /**
     * Sums the rent bonuses from any House/Hotel upgrades on the given set.
     * Delegates to each card's {@link PropertyCard#getTotalRent()}, which walks
     * the Decorator chain when available and falls back to the legacy upgrade list
     * otherwise.
     */
    private int getBuildingBonusForSet(PropertySet set) {
        int bonus = 0;
        for (PropertyCard pc : set.getCards()) {
            bonus += pc.getTotalRent();
        }
        return bonus;
    }

    /**
     * Attaches a House or Hotel from the player's hand to one legal property set.
     * @return true if the building was attached
     */
    public boolean tryAttachBuilding(Player player, ActionCard building) {
        RuleValidator rules = gameLogic.getRuleValidator();
        List<PropertySet> legalSets = new ArrayList<>();
        for (PropertySet set : player.getPropertyArea().getSets()) {
            boolean ok = building.getType() == ActionType.HOUSE ? rules.canAddHouse(set) : rules.canAddHotel(set);
            if (ok && !set.getCards().isEmpty()) {
                legalSets.add(set);
            }
        }

        if (legalSets.isEmpty()) {
            gameLogic.getGameManager().notifyAllObservers(
                    player.getName() + " could not place [" + building.getName()
                            + "] because there was no legal property set to attach it to.");
            return false;
        }

        PropertySet target = legalSets.get(0);
        if (legalSets.size() > 1) {
            List<String> options = new ArrayList<>();
            for (PropertySet set : legalSets) {
                options.add(set.getColor() + " (" + set.getCards().size() + "/" + set.getNeed() + ")");
            }

            int index = chooseOption(
                    building.getName(),
                    "Choose a property set for " + building.getName() + ":",
                    options,
                    true
            );
            if (index < 0 || index >= legalSets.size()) {
                gameLogic.getGameManager().notifyAllObservers(player.getName() + " cancelled " + building.getName() + ".");
                return false;
            }
            target = legalSets.get(index);
        }

        PropertyCard host = target.getCards().get(0);
        // Decorator chain + legacy upgrade list both maintained so UI rendering and rent
        // calculation stay in sync when rent reads via {@link PropertyCard#getTotalRent()}.
        if (building.getType() == ActionType.HOUSE) {
            host.attachHouse();
        } else {
            host.attachHotel();
        }
        host.attachUpgrade(building);
        gameLogic.getGameManager().notifyAllObservers(player.getName() + " placed "
                + building.getName() + " on " + target.getColor() + " set.");
        checkVictoryAfterStateChange();
        return true;
    }

    /**
     * Presents options and returns selected index (0-based), or -1 when cancelled.
     */
    private int chooseOption(String title, String prompt, List<String> options, boolean allowCancel) {
        return chooseOptionFor(activeDecisionPlayer, title, prompt, options, allowCancel);
    }

    /**
     * Presents options for a specific deciding player (e.g. JSN defender during an AI attack).
     */
    private int chooseOptionFor(Player decisionPlayer, String title, String prompt,
                              List<String> options, boolean allowCancel) {
        if (options == null || options.isEmpty()) {
            return -1;
        }

        if (decisionResolver != null && decisionPlayer != null
                && (decisionPlayer.isAI() || useRemoteDecisions)) {
            return decisionResolver.chooseOption(decisionPlayer, title, prompt, options, allowCancel);
        }

        if (!useDialogInput) {
            System.out.println();
            System.out.println("=== " + title + " ===");
            System.out.println(prompt);
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + options.get(i));
            }
            if (allowCancel) {
                System.out.println("  0. Cancel");
            }

            int choice = readChoice(allowCancel ? 0 : 1, options.size());
            if (allowCancel && choice == 0) {
                return -1;
            }
            return choice - 1;
        }

        return ThemedDialog.showChoice(dialogOwner, title, prompt, options, allowCancel);
    }

    // ============================= INPUT HELPER =============================
    /**
     * Reads a valid integer choice from the player within [min, max] range.
     */
    private int readChoice(int min, int max) {
        while (true) {
            System.out.print("Enter choice (" + min + "-" + max + "): ");
            String line = scanner.nextLine();
            if (line == null) {
                line = "";
            }
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
