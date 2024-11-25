
package org.latinschool.shared.messages;

import org.latinschool.shared.Card;

public class DealCards extends BaseMessage {
    private Card[] playerCards;

    
    public DealCards() {}

    public DealCards(Card[] playerCards) {
        this.playerCards = playerCards;
    }

    public Card[] getPlayerCards() {
        return playerCards;
    }
}
