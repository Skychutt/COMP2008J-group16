package com.monopolydeal.logic;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates all game rules to prevent illegal operations.
 */
public class RuleValidator {
    private final GameManager gameManager;

    public RuleValidator(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public RuleValidator() {
        this(null);
    }

    /**
     * Checks if a player can play a card during their turn.
     * @param player the player playing the card
     * @param card the card to play
     * @return true if the play is allowed
     */
    public boolean canPlayCard(Player player, Card card) {
        return explainPlayCardFailure(player, card) == null;
    }

    /**
     * Returns a human-readable reason when a card cannot be played.
     * @return null if the play is legal
     */
    public String explainPlayCardFailure(Player player, Card card) {
        if (player == null) {
            return "There is no active player, so no card can be played.";
        }
        if (card == null) {
            return "No card was selected.";
        }
        if (player.getActions() <= 0) {
            return "Cannot play [" + card.getName() + "] because " + player.getName() + " has no actions left.";
        }
        if (player.getHand().findCard(card.getId()) == null) {
            return "Cannot play [" + card.getName() + "] because it is no longer in " + player.getName() + "'s hand.";
        }

        if (card instanceof ActionCard) {
            ActionCard actionCard = (ActionCard) card;
            switch (actionCard.getType()) {
                case HOUSE:
                    return canAddHouseToAnySet(player)
                            ? null
                            : "Cannot play [House] because there is no complete property set that can take a house.";
                case HOTEL:
                    return canAddHotelToAnySet(player)
                            ? null
                            : "Cannot play [Hotel] because there is no complete property set that can take a hotel.";
                case JUST_SAY_NO:
                    // Just Say No cannot be proactively played as a normal turn action.
                    return "Cannot play [Just Say No] proactively; it only works as a response.";
                case SLY_DEAL:
                    return hasStealablePropertyOnOpponents(player)
                            ? null
                            : "Cannot play [Sly Deal] because opponents have no stealable property outside complete sets.";
                case FORCED_DEAL:
                    return hasForcedDealCandidates(player)
                            ? null
                            : "Cannot play [Forced Deal] because there is no legal property swap target on either side.";
                case DEAL_BREAKER:
                    return hasOpponentCompleteSet(player)
                            ? null
                            : "Cannot play [Deal Breaker] because no opponent has a complete property set.";
                case BIRTHDAY:
                    return hasAtLeastOneOpponent(player)
                            ? null
                            : "Cannot play [It's My Birthday] because there is no opponent to charge.";
                case DEBT_DEAL:
                    return hasAtLeastOneOpponent(player)
                            ? null
                            : "Cannot play [Debt Collector] because there is no opponent to charge.";
                case DOUBLE_RENT:
                    if (isDoubleRentBoostCard(actionCard)) {
                        return canUseDoubleRentNow(player)
                                ? null
                                : "Cannot play [Double The Rent] because there is no rent card and matching property set to boost this turn.";
                    }
                    if (isRentCard(actionCard)) {
                        return hasRentableSetForCard(player, actionCard)
                                ? null
                                : "Cannot play [" + actionCard.getName() + "] because there is no matching property set to charge rent on.";
                    }
                    return null;
                case GO_PASS:
                default:
                    return null;
            }
        }
        return null;
    }

    private boolean isDoubleRentBoostCard(ActionCard card) {
        return card != null
                && card.getType() == ActionType.DOUBLE_RENT
                && "Double The Rent".equals(card.getName());
    }

    private boolean isRentCard(ActionCard card) {
        return card != null
                && card.getType() == ActionType.DOUBLE_RENT
                && !"Double The Rent".equals(card.getName());
    }

    private boolean canUseDoubleRentNow(Player player) {
        if (player == null) {
            return false;
        }
        for (Card c : player.getHand().getCards()) {
            if (!(c instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) c;
            if (isRentCard(action) && hasRentableSetForCard(player, action)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRentableSetForCard(Player player, ActionCard rentCard) {
        if (player == null || rentCard == null) {
            return false;
        }
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (isSetAllowedByRentCard(set, rentCard)) {
                return true;
            }
        }
        return canRentByMovingFullWildThisTurn(player, rentCard);
    }

    private boolean canRentByMovingFullWildThisTurn(Player player, ActionCard rentCard) {
        boolean hasMovableFullWild = false;
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (set.getColor() != PropertyType.RAINBOW) {
                continue;
            }
            for (PropertyCard card : set.getCards()) {
                if (card.isFullColorWild()) {
                    hasMovableFullWild = true;
                    break;
                }
            }
            if (hasMovableFullWild) {
                break;
            }
        }
        if (!hasMovableFullWild) {
            return false;
        }

        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (set.getColor() == PropertyType.RAINBOW || set.getCards().isEmpty()) {
                continue;
            }
            if (isSetColorAllowedByRentCard(set.getColor(), rentCard)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSetAllowedByRentCard(PropertySet set, ActionCard rentCard) {
        if (set == null || rentCard == null) {
            return false;
        }
        if (set.getCards().isEmpty() || set.getRent() <= 0) {
            return false;
        }
        if (set.getColor() == PropertyType.RAINBOW) {
            return false;
        }

        String name = rentCard.getName();
        if (name == null) {
            return false;
        }
        return isSetColorAllowedByRentCard(set.getColor(), rentCard);
    }

    private boolean isSetColorAllowedByRentCard(PropertyType setColor, ActionCard rentCard) {
        if (setColor == null || rentCard == null || setColor == PropertyType.RAINBOW) {
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

        return allowed.contains(setColor);
    }

    private boolean hasAtLeastOneOpponent(Player current) {
        if (current == null) {
            return false;
        }
        return !getOpponents(current).isEmpty();
    }

    private boolean hasStealablePropertyOnOpponents(Player current) {
        for (Player opponent : getOpponents(current)) {
            for (PropertySet set : opponent.getPropertyArea().getSets()) {
                if (!set.isComplete() && !set.getCards().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasForcedDealCandidates(Player current) {
        boolean mine = hasSwappableProperty(current);
        if (!mine) {
            return false;
        }
        for (Player opponent : getOpponents(current)) {
            if (hasSwappableProperty(opponent)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSwappableProperty(Player player) {
        if (player == null) {
            return false;
        }
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (!set.isComplete() && !set.getCards().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOpponentCompleteSet(Player current) {
        for (Player opponent : getOpponents(current)) {
            for (PropertySet set : opponent.getPropertyArea().getSets()) {
                if (set.isComplete()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Player> getOpponents(Player current) {
        List<Player> opponents = new ArrayList<>();
        if (current == null || gameManager == null || gameManager.getPlayers() == null) {
            return opponents;
        }
        for (Player p : gameManager.getPlayers()) {
            if (p != null && p != current) {
                opponents.add(p);
            }
        }
        return opponents;
    }

    private boolean canAddHouseToAnySet(Player player) {
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (canAddHouse(set)) {
                return true;
            }
        }
        return false;
    }

    private boolean canAddHotelToAnySet(Player player) {
        for (PropertySet set : player.getPropertyArea().getSets()) {
            if (canAddHotel(set)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a House card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHouse(PropertySet set) {
        if (set == null || !set.isComplete()) {
            return false;
        }
        if (set.getColor() == PropertyType.BLACK || set.getColor() == PropertyType.LIGHTGREEN) {
            return false;
        }
        return countUpgrade(set, ActionType.HOUSE) == 0
                && countUpgrade(set, ActionType.HOTEL) == 0;
    }

    /**
     * Checks if a Hotel card can be added to a property set.
     * @param set the target property set
     * @return true if allowed
     */
    public boolean canAddHotel(PropertySet set) {
        if (set == null || !set.isComplete()) {
            return false;
        }
        if (set.getColor() == PropertyType.BLACK || set.getColor() == PropertyType.LIGHTGREEN) {
            return false;
        }
        return countUpgrade(set, ActionType.HOUSE) >= 1
                && countUpgrade(set, ActionType.HOTEL) == 0;
    }

    private static int countUpgrade(PropertySet set, ActionType type) {
        int n = 0;
        for (PropertyCard pc : set.getCards()) {
            for (Card u : pc.getUpgrades()) {
                if (u instanceof ActionCard && ((ActionCard) u).getType() == type) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Checks if a player's hand exceeds the maximum allowed limit (7 cards).
     * @param player the player to check
     * @return true if over the limit
     */
    public boolean isHandOverLimit(Player player) {
        return player != null && player.getHand().size() > Player.MAX_HAND_SIZE;
    }
}

