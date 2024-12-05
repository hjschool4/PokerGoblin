
package org.latinschool.server;

import org.latinschool.shared.Card;
import org.latinschool.shared.Rank;
import org.latinschool.shared.Suit;

import java.util.*;

public class HandEvaluator {
    public enum HandRank {
        HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT, FLUSH,
        FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH, ROYAL_FLUSH, ALL_FOLDED
    }

    public static class EvaluatedHand implements Comparable<EvaluatedHand> {
        private HandRank rank;
        private List<Rank> highCards;

        public EvaluatedHand(HandRank rank, List<Rank> highCards) {
            this.rank = rank;
            this.highCards = highCards;
        }

        public HandRank getRank() {
            return rank;
        }

        public List<Rank> getHighCards() {
            return highCards;
        }

        @Override
        public int compareTo(EvaluatedHand o) {
            if (this.rank.ordinal() != o.rank.ordinal()) {
                return Integer.compare(this.rank.ordinal(), o.rank.ordinal());
            }

            for (int i = 0; i < this.highCards.size(); i++) {
                if (i >= o.highCards.size()) return 1;
                int comparison = this.highCards.get(i).ordinal() - o.highCards.get(i).ordinal();
                if (comparison != 0) return comparison;
            }
            return 0;
        }
    }

    public static EvaluatedHand evaluateHand(List<Card> allCards) {

        if (allCards.size() != 7) {
            throw new IllegalArgumentException("Hand must contain exactly 7 cards.");
        }

        List<Card> sortedCards = new ArrayList<>(allCards);
        sortedCards.sort((a, b) -> b.getRank().ordinal() - a.getRank().ordinal());

        boolean isFlush = false;
        Suit flushSuit = null;
        Map<Suit, List<Card>> suitMap = new HashMap<>();
        for (Card card : sortedCards) {
            suitMap.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
            if (suitMap.get(card.getSuit()).size() >= 5) {
                isFlush = true;
                flushSuit = card.getSuit();
                break;
            }
        }

        List<Card> flushCards = new ArrayList<>();
        if (isFlush) {
            flushCards = suitMap.get(flushSuit);
            flushCards.sort((a, b) -> b.getRank().ordinal() - a.getRank().ordinal());
        }


        if (isFlush) {
            List<Card> potentialStraightFlush = new ArrayList<>(flushCards);
            EvaluatedHand straightFlush = findStraight(potentialStraightFlush);
            if (straightFlush != null) {
                if (straightFlush.getHighCards().get(0) == Rank.ACE) {
                    return new EvaluatedHand(HandRank.ROYAL_FLUSH, Collections.singletonList(Rank.ACE));
                }
                return new EvaluatedHand(HandRank.STRAIGHT_FLUSH, straightFlush.getHighCards());
            }
        }


        EvaluatedHand fourKind = findMultiple(sortedCards, 4);
        if (fourKind != null) {

            Rank kicker = getKickers(sortedCards, Arrays.asList(fourKind.getHighCards().get(0)), 1).get(0);
            List<Rank> highCards = new ArrayList<>(fourKind.getHighCards());
            highCards.add(kicker);
            return new EvaluatedHand(HandRank.FOUR_OF_A_KIND, highCards);
        }


        EvaluatedHand fullHouse = findFullHouse(sortedCards);
        if (fullHouse != null) {
            return fullHouse;
        }


        if (isFlush) {
            List<Rank> highCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                highCards.add(flushCards.get(i).getRank());
            }
            return new EvaluatedHand(HandRank.FLUSH, highCards);
        }


        EvaluatedHand straight = findStraight(sortedCards);
        if (straight != null) {
            return new EvaluatedHand(HandRank.STRAIGHT, straight.getHighCards());
        }


        EvaluatedHand threeKind = findMultiple(sortedCards, 3);
        if (threeKind != null) {
            List<Rank> highCards = new ArrayList<>(threeKind.getHighCards());
            highCards.addAll(getKickers(sortedCards, threeKind.getHighCards(), 2));
            return new EvaluatedHand(HandRank.THREE_OF_A_KIND, highCards);
        }


        List<Rank> twoPairs = findTwoPairs(sortedCards);
        if (twoPairs.size() >= 2) {
            List<Rank> highCards = new ArrayList<>(twoPairs.subList(0, 2));
            highCards.add(getKickers(sortedCards, twoPairs, 1).get(0));
            return new EvaluatedHand(HandRank.TWO_PAIR, highCards);
        }


        EvaluatedHand onePair = findMultiple(sortedCards, 2);
        if (onePair != null) {
            List<Rank> highCards = new ArrayList<>(onePair.getHighCards());
            highCards.addAll(getKickers(sortedCards, onePair.getHighCards(), 3));
            return new EvaluatedHand(HandRank.ONE_PAIR, highCards);
        }


        List<Rank> highCards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            highCards.add(sortedCards.get(i).getRank());
        }
        return new EvaluatedHand(HandRank.HIGH_CARD, highCards);
    }

    private static EvaluatedHand findMultiple(List<Card> cards, int count) {
        Map<Rank, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }

        for (Rank rank : Rank.values()) {
            if (rankCount.getOrDefault(rank, 0) >= count) {
                List<Rank> highCards = Collections.singletonList(rank);
                return new EvaluatedHand(null, highCards);
            }
        }
        return null;
    }

    private static EvaluatedHand findFullHouse(List<Card> cards) {
        Map<Rank, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }

        Rank threeKind = null;
        Rank pair = null;

        for (Rank rank : Rank.values()) {
            if (rankCount.getOrDefault(rank, 0) >= 3 && threeKind == null) {
                threeKind = rank;
            } else if (rankCount.getOrDefault(rank, 0) >= 2 && !rank.equals(threeKind)) {
                pair = rank;
            }
        }

        if (threeKind != null && pair != null) {
            List<Rank> highCards = new ArrayList<>();
            highCards.add(threeKind);
            highCards.add(pair);
            return new EvaluatedHand(HandRank.FULL_HOUSE, highCards);
        }

        return null;
    }

    private static List<Rank> findTwoPairs(List<Card> cards) {
        Map<Rank, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }

        List<Rank> pairs = new ArrayList<>();
        for (Rank rank : Rank.values()) {
            if (rankCount.getOrDefault(rank, 0) >= 2) {
                pairs.add(rank);
            }
        }


        pairs.sort((a, b) -> b.ordinal() - a.ordinal());
        return pairs;
    }

    private static EvaluatedHand findStraight(List<Card> cards) {
        Set<Integer> uniqueRanks = new HashSet<>();
        for (Card card : cards) {
            uniqueRanks.add(card.getRank().ordinal());
        }

        List<Integer> sortedRanks = new ArrayList<>(uniqueRanks);
        Collections.sort(sortedRanks, Collections.reverseOrder());


        if (uniqueRanks.contains(Rank.ACE.ordinal())) {
            sortedRanks.add(0, 0);
        }

        int consecutive = 1;
        int highest = sortedRanks.get(0);
        for (int i = 1; i < sortedRanks.size(); i++) {
            if (sortedRanks.get(i) == sortedRanks.get(i - 1) - 1) {
                consecutive++;
                if (consecutive == 5) {
                    Rank straightHigh = Rank.values()[sortedRanks.get(i - 1) + 1];
                    return new EvaluatedHand(null, Collections.singletonList(straightHigh));
                }
            } else {
                consecutive = 1;
            }
        }

        return null;
    }

    private static List<Rank> getKickers(List<Card> sortedCards, List<Rank> excluded, int count) {
        List<Rank> kickers = new ArrayList<>();
        for (Card card : sortedCards) {
            if (!excluded.contains(card.getRank()) && !kickers.contains(card.getRank())) {
                kickers.add(card.getRank());
                if (kickers.size() == count) break;
            }
        }
        return kickers;
    }
}
