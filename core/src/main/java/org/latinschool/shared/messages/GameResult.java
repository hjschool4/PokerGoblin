
package org.latinschool.shared.messages;

import org.latinschool.server.HandEvaluator.HandRank;

public class GameResult extends BaseMessage {
    private String winnerName;
    private HandRank winnerRank;

    private String[] playerNames;

    public GameResult() {}

    public GameResult(String winnerName, HandRank winnerRank, String[] playerNames) {
        this.winnerName = winnerName;
        this.winnerRank = winnerRank;
        this.playerNames = playerNames;
    }

    public String getWinnerName() { return winnerName; }

    public HandRank getWinnerRank() { return winnerRank; }
    public String[] getPlayerNames() {
        return playerNames;
    }
}
