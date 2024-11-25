
package org.latinschool.shared.messages;

import org.latinschool.server.HandEvaluator.HandRank;

public class GameResult extends BaseMessage {
    private String winnerName;
    private HandRank winnerRank;

    public GameResult() {}

    public GameResult(String winnerName, HandRank winnerRank) {
        this.winnerName = winnerName;
        this.winnerRank = winnerRank;
    }

    public String getWinnerName() { return winnerName; }

    public HandRank getWinnerRank() { return winnerRank; }
}
