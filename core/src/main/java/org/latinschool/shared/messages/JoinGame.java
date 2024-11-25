package org.latinschool.shared.messages;

public class JoinGame extends BaseMessage {
    private String playerName;

    public JoinGame() {}

    public JoinGame(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() { return playerName; }
}

