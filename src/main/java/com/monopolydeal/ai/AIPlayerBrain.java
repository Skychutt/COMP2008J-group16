package com.monopolydeal.ai;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.DecisionResolver;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.logic.RuleValidator;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Heuristic AI that prioritizes attacks and rent, then builds property sets.
 */
public class AIPlayerBrain implements AIActionStrategy, DecisionResolver {

    @Override
    public AIAction decideNextAction(Player ai, GameLogic gameLogic) {
        int actions = ai.getActions();
        RuleValidator rules = gameLogic.getRuleValidator();

        if (actions <= 0) {
            if (gameLogic.getRequiredDiscardCount(ai) > 0) {
                return discardWorst(ai);
            }
            return new AIAction(AIAction.Type.END_TURN);
        }

        AIAction priority = tryPriorityActions(ai, gameLogic, rules, actions);
        if (priority != null) {
            return priority;
        }

        AIAction build = tryBuildOrBank(ai, gameLogic, rules);
        if (build != null) {
            return build;
        }

        if (gameLogic.getRequiredDiscardCount(ai) > 0) {
            return discardWorst(ai);
        }
        return new AIAction(AIAction.Type.END_TURN);
    }

    @Override
    public boolean shouldUseJustSayNo(Player responder) {
        if (responder == null) {
            return false;
        }
        for (Card card : responder.getHand().getCards()) {
            if (card instanceof ActionCard
                    && ((ActionCard) card).getType() == ActionType.JUST_SAY_NO) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int chooseOption(Player player, String title, String prompt,
                            List<String> options, boolean allowCancel) {
        if (options == null || options.isEmpty()) {
            return -1;
        }

        String normalizedTitle = title == null ? "" : title.toLowerCase();
        if (normalizedTitle.contains("just say no")) {
            return shouldUseJustSayNo(player) ? 0 : 1;
        }
        if (normalizedTitle.contains("discard")) {
            return pickWorstDiscardIndex(player, options);
        }
        if (normalizedTitle.contains("debt collector")) {
            return pickRichestOptionIndex(options);
        }
        if (normalizedTitle.contains("rent")) {
            if (prompt != null && prompt.toLowerCase().contains("choose a player")) {
                return pickRichestOptionIndex(options);
            }
            return pickBestRentSetIndex(player, options);
        }
        if (normalizedTitle.contains("sly deal")) {
            return pickBestStealIndex(player, options);
        }
        if (normalizedTitle.contains("forced deal")) {
            if (prompt != null && prompt.toLowerCase().contains("your property")) {
                return pickWorstOwnPropertyIndex(player, options);
            }
            return pickBestStealIndex(player, options);
        }
        if (normalizedTitle.contains("deal breaker")) {
            return pickBestCompleteSetIndex(options);
        }
        if (normalizedTitle.contains("house") || normalizedTitle.contains("hotel")) {
            return pickBestBuildingSetIndex(player, options);
        }
        if (normalizedTitle.contains("payment")) {
            return 0;
        }

        return 0;
    }

    @Override
    public PropertyType choosePropertyColor(Player player, PropertyCard card) {
        if (card == null) {
            return null;
        }
        if (card.isColorCommitted()) {
            return card.getColor();
        }
        if (!card.needsColorChoiceOnPlacement()) {
            return null;
        }

        List<PropertyType> options = new ArrayList<>();
        Set<PropertyType> assignable = card.getAssignableColors();
        if (assignable != null) {
            options.addAll(assignable);
        }
        if (options.isEmpty()) {
            return null;
        }
        if (options.size() == 1) {
            return options.get(0);
        }

        PropertyType best = null;
        double bestProgress = -1;
        for (PropertyType color : options) {
            double progress = getSetProgress(player, color);
            if (progress > bestProgress) {
                bestProgress = progress;
                best = color;
            }
        }
        return best == null ? options.get(0) : best;
    }

    private AIAction tryPriorityActions(Player ai, GameLogic gameLogic,
                                        RuleValidator rules, int actions) {
        if (actions >= 2) {
            AIAction doubleRent = tryDoubleRent(ai, rules);
            if (doubleRent != null) {
                return doubleRent;
            }
        }

        AIAction dealBreaker = tryFirstPlayable(ai, rules, ActionType.DEAL_BREAKER);
        if (dealBreaker != null) return dealBreaker;

        AIAction passGo = tryFirstPlayable(ai, rules, ActionType.GO_PASS);
        if (passGo != null) return passGo;

        AIAction slyDeal = tryFirstPlayable(ai, rules, ActionType.SLY_DEAL);
        if (slyDeal != null) return slyDeal;

        AIAction forcedDeal = tryFirstPlayable(ai, rules, ActionType.FORCED_DEAL);
        if (forcedDeal != null) return forcedDeal;

        AIAction debt = tryFirstPlayable(ai, rules, ActionType.DEBT_DEAL);
        if (debt != null) return debt;

        AIAction anyRent = tryRentCard(ai, rules, true);
        if (anyRent != null) return anyRent;

        AIAction rent = tryRentCard(ai, rules, false);
        if (rent != null) return rent;

        return tryFirstPlayable(ai, rules, ActionType.BIRTHDAY);
    }

    private AIAction tryBuildOrBank(Player ai, GameLogic gameLogic, RuleValidator rules) {
        for (Card card : ai.getHand().getCards()) {
            if (!(card instanceof PropertyCard)) {
                continue;
            }
            PropertyCard property = (PropertyCard) card;
            if (rules.explainPlayCardFailure(ai, property) != null) {
                continue;
            }
            PropertyType color = property.needsColorChoiceOnPlacement()
                    ? choosePropertyColor(ai, property)
                    : null;
            return new AIAction(AIAction.Type.PLAY_CARD, property.getId(), color);
        }

        AIAction house = tryFirstPlayable(ai, rules, ActionType.HOUSE);
        if (house != null) return house;

        AIAction hotel = tryFirstPlayable(ai, rules, ActionType.HOTEL);
        if (hotel != null) return hotel;

        return tryBankDeposit(ai);
    }

    private AIAction tryDoubleRent(Player ai, RuleValidator rules) {
        for (Card card : ai.getHand().getCards()) {
            if (!(card instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) card;
            if (!"Double The Rent".equals(action.getName())) {
                continue;
            }
            if (rules.explainPlayCardFailure(ai, action) == null) {
                return new AIAction(AIAction.Type.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private AIAction tryRentCard(Player ai, RuleValidator rules, boolean anyRent) {
        for (Card card : ai.getHand().getCards()) {
            if (!(card instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) card;
            if (action.getType() != ActionType.DOUBLE_RENT) {
                continue;
            }
            if ("Double The Rent".equals(action.getName())) {
                continue;
            }
            String name = action.getName();
            boolean isAny = name != null && name.contains("Any");
            if (anyRent != isAny) {
                continue;
            }
            if (rules.explainPlayCardFailure(ai, action) == null) {
                return new AIAction(AIAction.Type.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private AIAction tryFirstPlayable(Player ai, RuleValidator rules, ActionType type) {
        for (Card card : ai.getHand().getCards()) {
            if (!(card instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) card;
            if (action.getType() != type) {
                continue;
            }
            if (rules.explainPlayCardFailure(ai, action) == null) {
                return new AIAction(AIAction.Type.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private AIAction tryBankDeposit(Player ai) {
        Card best = null;
        for (Card card : ai.getHand().getCards()) {
            if (!(card instanceof MoneyCard) && !(card instanceof ActionCard)) {
                continue;
            }
            if (card instanceof PropertyCard) {
                continue;
            }
            if (best == null || card.getValue() < best.getValue()) {
                best = card;
            }
        }
        if (best == null) {
            return null;
        }
        return new AIAction(AIAction.Type.DEPOSIT_TO_BANK, best.getId());
    }

    private AIAction discardWorst(Player ai) {
        Card worst = null;
        int worstScore = Integer.MAX_VALUE;
        for (Card card : ai.getHand().getCards()) {
            int score = (card instanceof MoneyCard ? 0 : 100) + card.getValue();
            if (score < worstScore) {
                worstScore = score;
                worst = card;
            }
        }
        if (worst == null) {
            return new AIAction(AIAction.Type.END_TURN);
        }
        return new AIAction(AIAction.Type.DISCARD, worst.getId());
    }

    private int pickRichestOptionIndex(List<String> options) {
        int best = 0;
        int bestBank = -1;
        for (int i = 0; i < options.size(); i++) {
            int bank = extractBankValue(options.get(i));
            if (bank > bestBank) {
                bestBank = bank;
                best = i;
            }
        }
        return best;
    }

    private int pickBestRentSetIndex(Player player, List<String> options) {
        int best = 0;
        int bestRent = -1;
        for (int i = 0; i < options.size(); i++) {
            int rent = extractRentValue(options.get(i));
            if (rent > bestRent) {
                bestRent = rent;
                best = i;
            }
        }
        if (bestRent >= 0) {
            return best;
        }
        return 0;
    }

    private int pickBestStealIndex(Player player, List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = extractColorFromOption(options.get(i));
            if (color != null && getSetProgress(player, color) < 1.0) {
                return i;
            }
        }
        return 0;
    }

    private int pickWorstOwnPropertyIndex(Player player, List<String> options) {
        int best = 0;
        double lowestProgress = Double.MAX_VALUE;
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = extractColorFromOption(options.get(i));
            double progress = color == null ? 0 : getSetProgress(player, color);
            if (progress < lowestProgress) {
                lowestProgress = progress;
                best = i;
            }
        }
        return best;
    }

    private int pickBestCompleteSetIndex(List<String> options) {
        int best = 0;
        int bestSize = -1;
        for (int i = 0; i < options.size(); i++) {
            int size = extractSetSize(options.get(i));
            if (size > bestSize) {
                bestSize = size;
                best = i;
            }
        }
        return best;
    }

    private int pickBestBuildingSetIndex(Player player, List<String> options) {
        int best = 0;
        int bestRent = -1;
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = extractColorFromOption(options.get(i));
            PropertySet set = color == null ? null : player.getPropertyArea().getSet(color);
            int rent = set == null ? 0 : set.getRent();
            if (rent > bestRent) {
                bestRent = rent;
                best = i;
            }
        }
        return best;
    }

    private int pickWorstDiscardIndex(Player player, List<String> options) {
        List<Card> hand = player.getHand().getCards();
        int best = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < options.size(); i++) {
            int cardId = extractCardId(options.get(i));
            Card card = findById(hand, cardId);
            if (card == null) {
                continue;
            }
            int score = (card instanceof MoneyCard ? 0 : 100) + card.getValue();
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private double getSetProgress(Player player, PropertyType color) {
        if (player == null || color == null) {
            return 0;
        }
        PropertySet set = player.getPropertyArea().getSet(color);
        if (set == null || set.getNeed() <= 0) {
            return 0;
        }
        return Math.min(1.0, (double) set.getCards().size() / set.getNeed());
    }

    private Card findById(List<Card> cards, int id) {
        for (Card card : cards) {
            if (card.getId() == id) {
                return card;
            }
        }
        return null;
    }

    private int extractBankValue(String option) {
        int idx = option == null ? -1 : option.indexOf("Bank:");
        if (idx < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(option.substring(idx + 5).replace("M)", "").replace("M", "").trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int extractRentValue(String option) {
        int idx = option == null ? -1 : option.indexOf("Rent:");
        if (idx < 0) {
            return -1;
        }
        try {
            String tail = option.substring(idx + 5).trim();
            int end = tail.indexOf('M');
            return Integer.parseInt(end >= 0 ? tail.substring(0, end).trim() : tail);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private PropertyType extractColorFromOption(String option) {
        if (option == null) {
            return null;
        }
        for (PropertyType type : PropertyType.values()) {
            if (type == PropertyType.RAINBOW) {
                continue;
            }
            if (option.contains(type.name()) || option.contains(formatColorLabel(type))) {
                return type;
            }
        }
        return null;
    }

    private String formatColorLabel(PropertyType type) {
        switch (type) {
            case BROWN: return "Brown";
            case LIGHTBLUE: return "Light Blue";
            case PURPLE: return "Pink";
            case ORANGE: return "Orange";
            case RED: return "Red";
            case YELLOW: return "Yellow";
            case GREEN: return "Green";
            case BLUE: return "Dark Blue";
            case BLACK: return "Railroad";
            case LIGHTGREEN: return "Utility";
            default: return type.name();
        }
    }

    private int extractSetSize(String option) {
        int start = option == null ? -1 : option.indexOf('(');
        int slash = option == null ? -1 : option.indexOf('/');
        if (start < 0 || slash < 0 || slash <= start) {
            return 0;
        }
        try {
            return Integer.parseInt(option.substring(start + 1, slash).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int extractCardId(String option) {
        int idx = option == null ? -1 : option.indexOf("#ID ");
        if (idx < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(option.substring(idx + 4).trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

}
