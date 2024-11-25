
package org.latinschool.shared.messages;

import org.latinschool.shared.Card;

public class UpdateGameState extends BaseMessage {
    private Card[] communityCards;
    private String currentPlayer;
    private PlayerStatus[] players;
    private int pot;

    public UpdateGameState() { }

    public UpdateGameState(Card[] communityCards, String currentPlayer, PlayerStatus[] players, int pot) {
        this.communityCards = communityCards;
        this.currentPlayer = currentPlayer;
        this.players = players;
        this.pot = pot;
    }

    public Card[] getCommunityCards() { return communityCards; }
    public String getCurrentPlayer() { return currentPlayer; }
    public PlayerStatus[] getPlayers() { return players; }
    public int getPot() { return pot; }

    public static class PlayerStatus {
        private String name;
        private int chips;
        private boolean hasFolded;
        private String lastAction; 

        public PlayerStatus() { }

        public PlayerStatus(String name, int chips, boolean hasFolded, String lastAction) {
            this.name = name;
            this.chips = chips;
            this.hasFolded = hasFolded;
            this.lastAction = lastAction;
        }

        public String getName() { return name; }
        public int getChips() { return chips; }
        public boolean hasFolded() { return hasFolded; }
        public String getLastAction() { return lastAction; }
    }
}
