package com.monopolydeal.utils;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameStatistics {

    private final Player player;
    private final List<Player> opponents;

    public GameStatistics(Player player) {
        this(player, new ArrayList<>());
    }

    public GameStatistics(Player player, List<Player> opponents) {
        this.player = player;
        this.opponents = opponents != null ? opponents : new ArrayList<>();
    }

    public PlayerStats getPlayerStats() {
        PlayerStats stats = new PlayerStats();

        stats.setPlayerName(player.getName());
        stats.setHandSize(player.getHand().size());
        stats.setBankBalance(bankBalance(player));
        stats.setTotalProperties(allProperties(player).size());
        stats.setCompletedSets(completedSetCount(player));
        stats.setTotalWealth(calculateTotalWealth(player));
        stats.setActionCardsCount(countActionCards(player));
        stats.setMoneyCardsValue(calculateMoneyCardsValue(player));
        stats.setPropertyDistribution(getPropertyDistribution(player));
        stats.setWildcardsCount(countWildcards(player));

        return stats;
    }

    private int calculateTotalWealth(Player target) {
        int handMoney = target.getHand().getCards().stream()
                .filter(c -> c instanceof MoneyCard)
                .mapToInt(c -> ((MoneyCard) c).getValue())
                .sum();
        return bankBalance(target) + handMoney;
    }

    private int countActionCards(Player target) {
        return (int) target.getHand().getCards().stream()
                .filter(c -> c instanceof ActionCard)
                .count();
    }

    private int calculateMoneyCardsValue(Player target) {
        return target.getHand().getCards().stream()
                .filter(c -> c instanceof MoneyCard)
                .mapToInt(c -> ((MoneyCard) c).getValue())
                .sum();
    }

    private Map<PropertyType, Integer> getPropertyDistribution(Player target) {
        Map<PropertyType, Integer> distribution = new EnumMap<>(PropertyType.class);

        for (PropertyCard pc : allProperties(target)) {
            PropertyType color = pc.getColor();
            if (color != null) {
                distribution.merge(color, 1, Integer::sum);
            }
        }

        return distribution;
    }

    private int countWildcards(Player target) {
        return (int) allProperties(target).stream()
                .filter(PropertyCard::isWild)
                .count();
    }

    public List<OpponentStats> getOpponentsStats() {
        List<OpponentStats> statsList = new ArrayList<>();

        for (Player opp : opponents) {
            OpponentStats stats = new OpponentStats();
            stats.setPlayerName(opp.getName());
            stats.setHandSize(opp.getHand().size());
            stats.setBankBalance(bankBalance(opp));
            stats.setTotalProperties(allProperties(opp).size());
            stats.setCompletedSets(completedSetCount(opp));
            stats.setThreatLevel(calculateThreatLevel(opp));
            statsList.add(stats);
        }

        statsList.sort((a, b) -> Double.compare(b.getThreatLevel(), a.getThreatLevel()));
        return statsList;
    }

    private double calculateThreatLevel(Player opp) {
        double threat = 0.0;
        threat += completedSetCount(opp) * 0.35;
        threat += Math.min(1.0, (double) bankBalance(opp) / 20);
        threat += Math.min(1.0, (double) allProperties(opp).size() / 12);
        return Math.min(1.0, threat);
    }

    public GameSummary getGameSummary() {
        GameSummary summary = new GameSummary();

        summary.setTotalPlayers(opponents.size() + 1);
        summary.setPlayerStats(getPlayerStats());
        summary.setOpponentsStats(getOpponentsStats());
        summary.setLeadingPlayer(findLeadingPlayer());
        summary.setAverageWealth(calculateAverageWealth());
        summary.setTotalCompletedSets(calculateTotalCompletedSets());

        return summary;
    }

    private String findLeadingPlayer() {
        int maxSets = completedSetCount(player);
        String leader = player.getName();

        for (Player opp : opponents) {
            int oppSets = completedSetCount(opp);
            if (oppSets > maxSets) {
                maxSets = oppSets;
                leader = opp.getName();
            }
        }

        return leader;
    }

    private int calculateAverageWealth() {
        int total = calculateTotalWealth(player);
        for (Player opp : opponents) {
            total += calculateTotalWealth(opp);
        }
        return total / (opponents.size() + 1);
    }

    private int calculateTotalCompletedSets() {
        int total = completedSetCount(player);
        for (Player opp : opponents) {
            total += completedSetCount(opp);
        }
        return total;
    }

    public HandAnalysis analyzeHand() {
        HandAnalysis analysis = new HandAnalysis();

        List<Card> hand = player.getHand().getCards();

        int moneyCount = 0;
        int moneyValue = 0;
        int propertyCount = 0;
        int actionCount = 0;
        int rentCount = 0;

        List<String> cardTypes = new ArrayList<>();

        for (Card card : hand) {
            if (card instanceof MoneyCard) {
                moneyCount++;
                moneyValue += ((MoneyCard) card).getValue();
                cardTypes.add("Money(" + ((MoneyCard) card).getValue() + "M)");
            } else if (card instanceof PropertyCard) {
                propertyCount++;
                PropertyCard pc = (PropertyCard) card;
                String type = pc.isWild() ? "Wildcard" : pc.getColor().toString();
                cardTypes.add("Property(" + type + ")");
            } else if (card instanceof ActionCard) {
                actionCount++;
                ActionCard ac = (ActionCard) card;
                cardTypes.add("Action(" + ac.getType() + ")");
                if (ac.getType() == ActionType.RENT || ac.getType() == ActionType.DOUBLE_RENT) {
                    rentCount++;
                }
            }
        }

        analysis.setTotalCards(hand.size());
        analysis.setMoneyCount(moneyCount);
        analysis.setMoneyValue(moneyValue);
        analysis.setPropertyCount(propertyCount);
        analysis.setActionCount(actionCount);
        analysis.setRentCount(rentCount);
        analysis.setCardTypes(cardTypes);
        analysis.setIsBalanced(isHandBalanced(moneyCount, propertyCount, actionCount));
        analysis.setRecommendation(getHandRecommendation(moneyCount, propertyCount, actionCount));

        return analysis;
    }

    private boolean isHandBalanced(int money, int property, int action) {
        int total = money + property + action;
        if (total == 0) {
            return true;
        }

        double moneyRatio = (double) money / total;
        double propertyRatio = (double) property / total;
        double actionRatio = (double) action / total;

        return moneyRatio >= 0.15 && moneyRatio <= 0.6
                && propertyRatio >= 0.15 && propertyRatio <= 0.6
                && actionRatio >= 0.1 && actionRatio <= 0.5;
    }

    private String getHandRecommendation(int money, int property, int action) {
        if (money == 0 && action > 0) {
            return "Consider using action cards that generate money";
        }
        if (property == 0 && money > 5) {
            return "Consider buying properties or trading";
        }
        if (action == 0 && property > 3) {
            return "Consider holding for defensive cards";
        }
        return "Hand composition looks balanced";
    }

    public SetAnalysis analyzeSets() {
        SetAnalysis analysis = new SetAnalysis();

        List<SetDetail> setDetails = new ArrayList<>();
        int incompleteSets = 0;
        int completedSets = 0;
        int totalPotentialRent = 0;

        for (PropertyType color : PropertyType.values()) {
            if (color == PropertyType.RAINBOW) {
                continue;
            }

            int count = propertyCount(player, color);
            int needed = cardsNeeded(color);

            if (count > 0) {
                SetDetail detail = new SetDetail();
                detail.setColor(color);
                detail.setCurrentCount(count);
                detail.setNeeded(needed);
                detail.setIsComplete(count >= needed);
                detail.setProgressPercent(needed > 0 ? (count * 100) / needed : 0);

                if (count >= needed) {
                    completedSets++;
                    totalPotentialRent += calculateSetRent(player, color);
                } else {
                    incompleteSets++;
                }

                setDetails.add(detail);
            }
        }

        setDetails.sort((a, b) -> Integer.compare(b.getProgressPercent(), a.getProgressPercent()));

        analysis.setSetDetails(setDetails);
        analysis.setCompletedSets(completedSets);
        analysis.setIncompleteSets(incompleteSets);
        analysis.setTotalPotentialRent(totalPotentialRent);
        analysis.setNextSetToComplete(findNextSet(setDetails));

        return analysis;
    }

    private int calculateSetRent(Player target, PropertyType color) {
        PropertySet set = target.getPropertyArea().getSet(color);
        return set != null ? set.getRent() : 0;
    }

    private PropertyType findNextSet(List<SetDetail> details) {
        for (SetDetail detail : details) {
            if (!detail.isComplete()) {
                return detail.getColor();
            }
        }
        return null;
    }

    private static int bankBalance(Player target) {
        return target.getBankArea().total();
    }

    private static int completedSetCount(Player target) {
        return target.getPropertyArea().countCompleteSets();
    }

    private static List<PropertyCard> allProperties(Player target) {
        List<PropertyCard> properties = new ArrayList<>();
        for (PropertySet set : target.getPropertyArea().getSets()) {
            properties.addAll(set.getCards());
        }
        return properties;
    }

    private static int propertyCount(Player target, PropertyType color) {
        PropertySet set = target.getPropertyArea().getSet(color);
        return set == null ? 0 : set.getCards().size();
    }

    private static int cardsNeeded(PropertyType color) {
        return new PropertySet(color).getNeed();
    }

    public static class PlayerStats {
        private String playerName;
        private int handSize;
        private int bankBalance;
        private int totalProperties;
        private int completedSets;
        private int totalWealth;
        private int actionCardsCount;
        private int moneyCardsValue;
        private Map<PropertyType, Integer> propertyDistribution;
        private int wildcardsCount;

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public int getHandSize() { return handSize; }
        public void setHandSize(int handSize) { this.handSize = handSize; }
        public int getBankBalance() { return bankBalance; }
        public void setBankBalance(int bankBalance) { this.bankBalance = bankBalance; }
        public int getTotalProperties() { return totalProperties; }
        public void setTotalProperties(int totalProperties) { this.totalProperties = totalProperties; }
        public int getCompletedSets() { return completedSets; }
        public void setCompletedSets(int completedSets) { this.completedSets = completedSets; }
        public int getTotalWealth() { return totalWealth; }
        public void setTotalWealth(int totalWealth) { this.totalWealth = totalWealth; }
        public int getActionCardsCount() { return actionCardsCount; }
        public void setActionCardsCount(int actionCardsCount) { this.actionCardsCount = actionCardsCount; }
        public int getMoneyCardsValue() { return moneyCardsValue; }
        public void setMoneyCardsValue(int moneyCardsValue) { this.moneyCardsValue = moneyCardsValue; }
        public Map<PropertyType, Integer> getPropertyDistribution() { return propertyDistribution; }
        public void setPropertyDistribution(Map<PropertyType, Integer> propertyDistribution) { this.propertyDistribution = propertyDistribution; }
        public int getWildcardsCount() { return wildcardsCount; }
        public void setWildcardsCount(int wildcardsCount) { this.wildcardsCount = wildcardsCount; }
    }

    public static class OpponentStats {
        private String playerName;
        private int handSize;
        private int bankBalance;
        private int totalProperties;
        private int completedSets;
        private double threatLevel;

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public int getHandSize() { return handSize; }
        public void setHandSize(int handSize) { this.handSize = handSize; }
        public int getBankBalance() { return bankBalance; }
        public void setBankBalance(int bankBalance) { this.bankBalance = bankBalance; }
        public int getTotalProperties() { return totalProperties; }
        public void setTotalProperties(int totalProperties) { this.totalProperties = totalProperties; }
        public int getCompletedSets() { return completedSets; }
        public void setCompletedSets(int completedSets) { this.completedSets = completedSets; }
        public double getThreatLevel() { return threatLevel; }
        public void setThreatLevel(double threatLevel) { this.threatLevel = threatLevel; }
    }

    public static class GameSummary {
        private int totalPlayers;
        private PlayerStats playerStats;
        private List<OpponentStats> opponentsStats;
        private String leadingPlayer;
        private int averageWealth;
        private int totalCompletedSets;

        public int getTotalPlayers() { return totalPlayers; }
        public void setTotalPlayers(int totalPlayers) { this.totalPlayers = totalPlayers; }
        public PlayerStats getPlayerStats() { return playerStats; }
        public void setPlayerStats(PlayerStats playerStats) { this.playerStats = playerStats; }
        public List<OpponentStats> getOpponentsStats() { return opponentsStats; }
        public void setOpponentsStats(List<OpponentStats> opponentsStats) { this.opponentsStats = opponentsStats; }
        public String getLeadingPlayer() { return leadingPlayer; }
        public void setLeadingPlayer(String leadingPlayer) { this.leadingPlayer = leadingPlayer; }
        public int getAverageWealth() { return averageWealth; }
        public void setAverageWealth(int averageWealth) { this.averageWealth = averageWealth; }
        public int getTotalCompletedSets() { return totalCompletedSets; }
        public void setTotalCompletedSets(int totalCompletedSets) { this.totalCompletedSets = totalCompletedSets; }
    }

    public static class HandAnalysis {
        private int totalCards;
        private int moneyCount;
        private int moneyValue;
        private int propertyCount;
        private int actionCount;
        private int rentCount;
        private List<String> cardTypes;
        private boolean isBalanced;
        private String recommendation;

        public int getTotalCards() { return totalCards; }
        public void setTotalCards(int totalCards) { this.totalCards = totalCards; }
        public int getMoneyCount() { return moneyCount; }
        public void setMoneyCount(int moneyCount) { this.moneyCount = moneyCount; }
        public int getMoneyValue() { return moneyValue; }
        public void setMoneyValue(int moneyValue) { this.moneyValue = moneyValue; }
        public int getPropertyCount() { return propertyCount; }
        public void setPropertyCount(int propertyCount) { this.propertyCount = propertyCount; }
        public int getActionCount() { return actionCount; }
        public void setActionCount(int actionCount) { this.actionCount = actionCount; }
        public int getRentCount() { return rentCount; }
        public void setRentCount(int rentCount) { this.rentCount = rentCount; }
        public List<String> getCardTypes() { return cardTypes; }
        public void setCardTypes(List<String> cardTypes) { this.cardTypes = cardTypes; }
        public boolean isBalanced() { return isBalanced; }
        public void setIsBalanced(boolean isBalanced) { this.isBalanced = isBalanced; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public static class SetAnalysis {
        private List<SetDetail> setDetails;
        private int completedSets;
        private int incompleteSets;
        private int totalPotentialRent;
        private PropertyType nextSetToComplete;

        public List<SetDetail> getSetDetails() { return setDetails; }
        public void setSetDetails(List<SetDetail> setDetails) { this.setDetails = setDetails; }
        public int getCompletedSets() { return completedSets; }
        public void setCompletedSets(int completedSets) { this.completedSets = completedSets; }
        public int getIncompleteSets() { return incompleteSets; }
        public void setIncompleteSets(int incompleteSets) { this.incompleteSets = incompleteSets; }
        public int getTotalPotentialRent() { return totalPotentialRent; }
        public void setTotalPotentialRent(int totalPotentialRent) { this.totalPotentialRent = totalPotentialRent; }
        public PropertyType getNextSetToComplete() { return nextSetToComplete; }
        public void setNextSetToComplete(PropertyType nextSetToComplete) { this.nextSetToComplete = nextSetToComplete; }
    }

    public static class SetDetail {
        private PropertyType color;
        private int currentCount;
        private int needed;
        private boolean isComplete;
        private int progressPercent;

        public PropertyType getColor() { return color; }
        public void setColor(PropertyType color) { this.color = color; }
        public int getCurrentCount() { return currentCount; }
        public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }
        public int getNeeded() { return needed; }
        public void setNeeded(int needed) { this.needed = needed; }
        public boolean isComplete() { return isComplete; }
        public void setIsComplete(boolean isComplete) { this.isComplete = isComplete; }
        public int getProgressPercent() { return progressPercent; }
        public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    }
}
