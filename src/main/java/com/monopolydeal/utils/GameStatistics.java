package com.monopolydeal.utils;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.RentCard;
import com.monopolydeal.enums.PropertyType;

import java.util.*;
import java.util.stream.Collectors;

public class GameStatistics {
    
    private Player player;
    private List<Player> opponents;
    
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
        stats.setBankBalance(player.getBankBalance());
        stats.setTotalProperties(player.getProperties().size());
        stats.setCompletedSets(player.getCompletedSets().size());
        stats.setTotalWealth(calculateTotalWealth());
        stats.setActionCardsCount(countActionCards());
        stats.setMoneyCardsValue(calculateMoneyCardsValue());
        stats.setPropertyDistribution(getPropertyDistribution());
        stats.setWildcardsCount(countWildcards());
        
        return stats;
    }
    
    private int calculateTotalWealth() {
        int handMoney = player.getHand().stream()
                .filter(c -> c instanceof MoneyCard)
                .mapToInt(c -> ((MoneyCard) c).getValue())
                .sum();
        return player.getBankBalance() + handMoney;
    }
    
    private int countActionCards() {
        return (int) player.getHand().stream()
                .filter(c -> c instanceof ActionCard)
                .count();
    }
    
    private int calculateMoneyCardsValue() {
        return player.getHand().stream()
                .filter(c -> c instanceof MoneyCard)
                .mapToInt(c -> ((MoneyCard) c).getValue())
                .sum();
    }
    
    private Map<PropertyType, Integer> getPropertyDistribution() {
        Map<PropertyType, Integer> distribution = new EnumMap<>(PropertyType.class);
        
        for (PropertyCard pc : player.getProperties()) {
            PropertyType color = pc.getColor();
            distribution.merge(color, 1, Integer::sum);
        }
        
        return distribution;
    }
    
    private int countWildcards() {
        return (int) player.getProperties().stream()
                .filter(PropertyCard::isWildcard)
                .count();
    }
    
    public List<OpponentStats> getOpponentsStats() {
        List<OpponentStats> statsList = new ArrayList<>();
        
        for (Player opp : opponents) {
            OpponentStats stats = new OpponentStats();
            stats.setPlayerName(opp.getName());
            stats.setHandSize(opp.getHand().size());
            stats.setBankBalance(opp.getBankBalance());
            stats.setTotalProperties(opp.getProperties().size());
            stats.setCompletedSets(opp.getCompletedSets().size());
            stats.setThreatLevel(calculateThreatLevel(opp));
            statsList.add(stats);
        }
        
        statsList.sort((a, b) -> Double.compare(b.getThreatLevel(), a.getThreatLevel()));
        return statsList;
    }
    
    private double calculateThreatLevel(Player opp) {
        double threat = 0.0;
        threat += opp.getCompletedSets().size() * 0.35;
        threat += Math.min(1.0, (double) opp.getBankBalance() / 20);
        threat += Math.min(1.0, (double) opp.getProperties().size() / 12);
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
        int maxSets = player.getCompletedSets().size();
        String leader = player.getName();
        
        for (Player opp : opponents) {
            if (opp.getCompletedSets().size() > maxSets) {
                maxSets = opp.getCompletedSets().size();
                leader = opp.getName();
            }
        }
        
        return leader;
    }
    
    private int calculateAverageWealth() {
        int total = calculateTotalWealth();
        for (Player opp : opponents) {
            total += opp.getBankBalance();
            total += opp.getHand().stream()
                    .filter(c -> c instanceof MoneyCard)
                    .mapToInt(c -> ((MoneyCard) c).getValue())
                    .sum();
        }
        return total / (opponents.size() + 1);
    }
    
    private int calculateTotalCompletedSets() {
        int total = player.getCompletedSets().size();
        for (Player opp : opponents) {
            total += opp.getCompletedSets().size();
        }
        return total;
    }
    
    public HandAnalysis analyzeHand() {
        HandAnalysis analysis = new HandAnalysis();
        
        List<Card> hand = player.getHand();
        
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
                String type = pc.isWildcard() ? "Wildcard" : pc.getColor().toString();
                cardTypes.add("Property(" + type + ")");
            } else if (card instanceof ActionCard) {
                actionCount++;
                ActionCard ac = (ActionCard) card;
                cardTypes.add("Action(" + ac.getActionType() + ")");
                if (ac.getActionType().name().contains("RENT")) {
                    rentCount++;
                }
            } else if (card instanceof RentCard) {
                rentCount++;
                cardTypes.add("RentCard");
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
        if (total == 0) return true;
        
        double moneyRatio = (double) money / total;
        double propertyRatio = (double) property / total;
        double actionRatio = (double) action / total;
        
        return moneyRatio >= 0.15 && moneyRatio <= 0.6 &&
               propertyRatio >= 0.15 && propertyRatio <= 0.6 &&
               actionRatio >= 0.1 && actionRatio <= 0.5;
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
            if (color == PropertyType.NONE) continue;
            
            int count = player.getPropertyCount(color);
            int needed = color.getCardsNeeded();
            
            if (count > 0) {
                SetDetail detail = new SetDetail();
                detail.setColor(color);
                detail.setCurrentCount(count);
                detail.setNeeded(needed);
                detail.setIsComplete(count >= needed);
                detail.setProgressPercent((count * 100) / needed);
                
                if (count >= needed) {
                    completedSets++;
                    totalPotentialRent += calculateSetRent(color);
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
    
    private int calculateSetRent(PropertyType color) {
        return player.getPropertySet(color) != null ? 
               player.getPropertySet(color).getTotalRent() : 0;
    }
    
    private PropertyType findNextSet(List<SetDetail> details) {
        for (SetDetail detail : details) {
            if (!detail.isComplete()) {
                return detail.getColor();
            }
        }
        return null;
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