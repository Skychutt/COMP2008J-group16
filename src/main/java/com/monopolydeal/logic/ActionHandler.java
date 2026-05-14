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

/**
 * Handles the execution of all action card effects.
 */
public class ActionHandler {

    private static final int BIRTHDAY_PAYMENT = 2;
    private static final int DEBT_COLLECTOR_AMOUNT = 5;

    private final GameLogic gameLogic;

    public ActionHandler(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
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
                handleForcedDealStub(player);
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

    private void handleDoubleRentOrRentStub(Player player, ActionCard card) {
        GameManager gm = gameLogic.getGameManager();
        if ("Double The Rent".equals(card.getName())) {
            gm.notifyAllObservers(player.getName() + " played Double The Rent (pair with a rent card on the same turn — UI hook).");
            return;
        }
        if (card.getName() != null && card.getName().contains("Rent")) {
            PropertySet set = firstCompleteSet(player);
            Player target = firstOtherPlayerWithAssets(player);
            if (set != null && target != null) {
                boolean doubleRent = card.getName().toLowerCase().contains("double");
                gameLogic.collectRent(player, target, set, doubleRent);
            } else {
                gm.notifyAllObservers(player.getName() + " played " + card.getName() + " but no valid rent target/set.");
            }
        } else {
            gm.notifyAllObservers(player.getName() + " played " + card.getName() + ".");
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
        Player victim = firstOtherPlayerWithAssets(collector);
        GameManager gm = gameLogic.getGameManager();
        if (victim == null) {
            gm.notifyAllObservers("Debt Collector: no other player with assets.");
            return;
        }
        AssetTransferManager.PaymentResult r = gameLogic.getAssetTransferManager()
                .processPayment(victim, collector, DEBT_COLLECTOR_AMOUNT, AssetTransferManager.PaymentMode.USE_MIXED);
        gm.notifyAllObservers(r.getMessage());
    }

    private void handleSlyDealAuto(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();
        for (Player victim : gm.getPlayers()) {
            if (victim == thief) {
                continue;
            }
            PropertyCard stolen = firstPropertyCard(victim);
            if (stolen != null) {
                if (handleJustSayNo(victim, thief, played)) {
                    return;
                }
                victim.getPropertyArea().remove(stolen);
                thief.getPropertyArea().add(stolen);
                gm.notifyAllObservers(thief.getName() + " stole [" + stolen.getName() + "] from " + victim.getName() + ".");
                return;
            }
        }
        gm.notifyAllObservers("Sly Deal: no property to steal.");
    }

    private void handleForcedDealStub(Player player) {
        gameLogic.getGameManager().notifyAllObservers("Forced Deal: requires UI to pick swap — stub skipped for " + player.getName() + ".");
    }

    private void handleDealBreakerAuto(Player thief, ActionCard played) {
        GameManager gm = gameLogic.getGameManager();
        for (Player victim : gm.getPlayers()) {
            if (victim == thief) {
                continue;
            }
            PropertySet complete = firstCompletedSet(victim);
            if (complete != null) {
                if (handleJustSayNo(victim, thief, played)) {
                    return;
                }
                List<PropertyCard> cards = new ArrayList<>(complete.getCards());
                for (PropertyCard pc : cards) {
                    victim.getPropertyArea().remove(pc);
                    thief.getPropertyArea().add(pc);
                }
                gm.notifyAllObservers(thief.getName() + " stole a complete " + complete.getColor()
                        + " set from " + victim.getName() + " with Deal Breaker.");
                return;
            }
        }
        gm.notifyAllObservers("Deal Breaker: no complete set to steal.");
    }

    private PropertySet firstCompletedSet(Player victim) {
        for (PropertySet s : victim.getPropertyArea().getSets()) {
            if (s.isComplete()) {
                return s;
            }
        }
        return null;
    }

    private PropertyCard firstPropertyCard(Player victim) {
        for (PropertySet s : victim.getPropertyArea().getSets()) {
            if (!s.getCards().isEmpty()) {
                return s.getCards().get(0);
            }
        }
        return null;
    }

    private Player firstOtherPlayerWithAssets(Player self) {
        for (Player p : gameLogic.getGameManager().getPlayers()) {
            if (p != self && gameLogic.getAssetTransferManager().canAfford(p, 1)) {
                return p;
            }
        }
        for (Player p : gameLogic.getGameManager().getPlayers()) {
            if (p != self) {
                return p;
            }
        }
        return null;
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
}
