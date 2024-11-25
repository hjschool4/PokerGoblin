
package org.latinschool.shared.messages;

public class GameReady extends BaseMessage {
    private String[] playerNames;

    public GameReady() {}

    public GameReady(String[] playerNames) {
        this.playerNames = playerNames;
    }

    public String[] getPlayerNames() {
        return playerNames;
    }
}
