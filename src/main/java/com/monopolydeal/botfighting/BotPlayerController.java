package com.monopolydeal.botfighting;

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
 * Heuristic bot controller that prioritizes attacks and rent, then builds property sets.
 */
//First make trouble and collect money (attack card → rent collection),
//and then build (land → house → store → save money). Each step is greedy to choose the current best.

public class BotPlayerController implements BotDecisionPolicy, DecisionResolver {

    @Override
    public BotTurnChoice pickTurnChoice(Player bot, GameLogic gameLogic) {
        int actions = bot.getActions();
        RuleValidator rules = gameLogic.getRuleValidator();

        if (actions <= 0) {
            if (gameLogic.getRequiredDiscardCount(bot) > 0) {
                return buildDiscardChoice(bot);
            }
            return new BotTurnChoice(BotTurnChoice.Kind.END_TURN);
        }

        BotTurnChoice offensive = attemptOffensivePlays(bot, gameLogic, rules, actions);
        if (offensive != null) {
            return offensive;
        }

        BotTurnChoice build = attemptPropertyOrBanking(bot, gameLogic, rules);
        if (build != null) {
            return build;
        }

        if (gameLogic.getRequiredDiscardCount(bot) > 0) {
            return buildDiscardChoice(bot);
        }
        return new BotTurnChoice(BotTurnChoice.Kind.END_TURN);
    }

    @Override
    public boolean wantsJustSayNoCounter(Player responder) {
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
            return wantsJustSayNoCounter(player) ? 0 : 1;
        }
        if (normalizedTitle.contains("discard")) {
            return selectWorstDiscardIndex(player, options);
        }
        if (normalizedTitle.contains("debt collector")) {
            return selectRichestTargetIndex(options);
        }
        if (normalizedTitle.contains("rent")) {
            if (prompt != null && prompt.toLowerCase().contains("choose a player")) {
                return selectRichestTargetIndex(options);
            }
            return selectBestRentSetIndex(player, options);
        }
        if (normalizedTitle.contains("sly deal")) {
            return selectBestStealIndex(player, options);
        }
        if (normalizedTitle.contains("forced deal")) {
            if (prompt != null && prompt.toLowerCase().contains("your property")) {
                return selectWeakestOwnPropertyIndex(player, options);
            }
            return selectBestStealIndex(player, options);
        }
        if (normalizedTitle.contains("deal breaker")) {
            return selectBestCompleteSetIndex(options);
        }
        if (normalizedTitle.contains("house") || normalizedTitle.contains("hotel")) {
            return selectBestBuildingSetIndex(player, options);
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
            double progress = measureSetProgress(player, color);
            if (progress > bestProgress) {
                bestProgress = progress;
                best = color;
            }
        }
        return best == null ? options.get(0) : best;
    }

    private BotTurnChoice attemptOffensivePlays(Player bot, GameLogic gameLogic,
                                                RuleValidator rules, int actions) {
        if (actions >= 2) {
            BotTurnChoice doubleRent = findDoubleRentCard(bot, rules);
            if (doubleRent != null) {
                return doubleRent;
            }
        }

        BotTurnChoice dealBreaker = findPlayableAction(bot, rules, ActionType.DEAL_BREAKER);
        if (dealBreaker != null) return dealBreaker;

        BotTurnChoice passGo = findPlayableAction(bot, rules, ActionType.GO_PASS);
        if (passGo != null) return passGo;

        BotTurnChoice slyDeal = findPlayableAction(bot, rules, ActionType.SLY_DEAL);
        if (slyDeal != null) return slyDeal;

        BotTurnChoice forcedDeal = findPlayableAction(bot, rules, ActionType.FORCED_DEAL);
        if (forcedDeal != null) return forcedDeal;

        BotTurnChoice debt = findPlayableAction(bot, rules, ActionType.DEBT_DEAL);
        if (debt != null) return debt;

        BotTurnChoice anyRent = findRentCard(bot, rules, true);
        if (anyRent != null) return anyRent;

        BotTurnChoice rent = findRentCard(bot, rules, false);
        if (rent != null) return rent;

        return findPlayableAction(bot, rules, ActionType.BIRTHDAY);
    }

    private BotTurnChoice attemptPropertyOrBanking(Player bot, GameLogic gameLogic, RuleValidator rules) {
        for (Card card : bot.getHand().getCards()) {
            if (!(card instanceof PropertyCard)) {
                continue;
            }
            PropertyCard property = (PropertyCard) card;
            if (rules.explainPlayCardFailure(bot, property) != null) {
                continue;
            }
            PropertyType color = property.needsColorChoiceOnPlacement()
                    ? choosePropertyColor(bot, property)
                    : null;
            return new BotTurnChoice(BotTurnChoice.Kind.PLAY_CARD, property.getId(), color);
        }

        BotTurnChoice house = findPlayableAction(bot, rules, ActionType.HOUSE);
        if (house != null) return house;

        BotTurnChoice hotel = findPlayableAction(bot, rules, ActionType.HOTEL);
        if (hotel != null) return hotel;

        return findBankDeposit(bot);
    }

    private BotTurnChoice findDoubleRentCard(Player bot, RuleValidator rules) {
        for (Card card : bot.getHand().getCards()) {
            if (!(card instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) card;
            if (!"Double The Rent".equals(action.getName())) {
                continue;
            }
            if (rules.explainPlayCardFailure(bot, action) == null) {
                return new BotTurnChoice(BotTurnChoice.Kind.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private BotTurnChoice findRentCard(Player bot, RuleValidator rules, boolean anyRent) {
        for (Card card : bot.getHand().getCards()) {
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
            if (rules.explainPlayCardFailure(bot, action) == null) {
                return new BotTurnChoice(BotTurnChoice.Kind.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private BotTurnChoice findPlayableAction(Player bot, RuleValidator rules, ActionType type) {
        for (Card card : bot.getHand().getCards()) {
            if (!(card instanceof ActionCard)) {
                continue;
            }
            ActionCard action = (ActionCard) card;
            if (action.getType() != type) {
                continue;
            }
            if (rules.explainPlayCardFailure(bot, action) == null) {
                return new BotTurnChoice(BotTurnChoice.Kind.PLAY_CARD, action.getId());
            }
        }
        return null;
    }

    private BotTurnChoice findBankDeposit(Player bot) {
        Card best = null;
        for (Card card : bot.getHand().getCards()) {
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
        return new BotTurnChoice(BotTurnChoice.Kind.DEPOSIT_TO_BANK, best.getId());
    }

    private BotTurnChoice buildDiscardChoice(Player bot) {
        Card worst = null;
        int worstScore = Integer.MAX_VALUE;
        for (Card card : bot.getHand().getCards()) {
            int score = (card instanceof MoneyCard ? 0 : 100) + card.getValue();
            if (score < worstScore) {
                worstScore = score;
                worst = card;
            }
        }
        if (worst == null) {
            return new BotTurnChoice(BotTurnChoice.Kind.END_TURN);
        }
        return new BotTurnChoice(BotTurnChoice.Kind.DISCARD, worst.getId());
    }

    private int selectRichestTargetIndex(List<String> options) {
        int best = 0;
        int bestBank = -1;
        for (int i = 0; i < options.size(); i++) {
            int bank = parseBankValue(options.get(i));
            if (bank > bestBank) {
                bestBank = bank;
                best = i;
            }
        }
        return best;
    }

    private int selectBestRentSetIndex(Player player, List<String> options) {
        int best = 0;
        int bestRent = -1;
        for (int i = 0; i < options.size(); i++) {
            int rent = parseRentValue(options.get(i));
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

    private int selectBestStealIndex(Player player, List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = parseColorFromLabel(options.get(i));
            if (color != null && measureSetProgress(player, color) < 1.0) {
                return i;
            }
        }
        return 0;
    }

    private int selectWeakestOwnPropertyIndex(Player player, List<String> options) {
        int best = 0;
        double lowestProgress = Double.MAX_VALUE;
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = parseColorFromLabel(options.get(i));
            double progress = color == null ? 0 : measureSetProgress(player, color);
            if (progress < lowestProgress) {
                lowestProgress = progress;
                best = i;
            }
        }
        return best;
    }

    private int selectBestCompleteSetIndex(List<String> options) {
        int best = 0;
        int bestSize = -1;
        for (int i = 0; i < options.size(); i++) {
            int size = parseSetSize(options.get(i));
            if (size > bestSize) {
                bestSize = size;
                best = i;
            }
        }
        return best;
    }

    private int selectBestBuildingSetIndex(Player player, List<String> options) {
        int best = 0;
        int bestRent = -1;
        for (int i = 0; i < options.size(); i++) {
            PropertyType color = parseColorFromLabel(options.get(i));
            PropertySet set = color == null ? null : player.getPropertyArea().getSet(color);
            int rent = set == null ? 0 : set.getRent();
            if (rent > bestRent) {
                bestRent = rent;
                best = i;
            }
        }
        return best;
    }

    private int selectWorstDiscardIndex(Player player, List<String> options) {
        List<Card> hand = player.getHand().getCards();
        int best = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < options.size(); i++) {
            int cardId = parseCardId(options.get(i));
            Card card = locateCardById(hand, cardId);
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

    private double measureSetProgress(Player player, PropertyType color) {
        if (player == null || color == null) {
            return 0;
        }
        PropertySet set = player.getPropertyArea().getSet(color);
        if (set == null || set.getNeed() <= 0) {
            return 0;
        }
        return Math.min(1.0, (double) set.getCards().size() / set.getNeed());
    }

    private Card locateCardById(List<Card> cards, int id) {
        for (Card card : cards) {
            if (card.getId() == id) {
                return card;
            }
        }
        return null;
    }

    private int parseBankValue(String option) {
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

    private int parseRentValue(String option) {
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

    private PropertyType parseColorFromLabel(String option) {
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

    private int parseSetSize(String option) {
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

    private int parseCardId(String option) {
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
