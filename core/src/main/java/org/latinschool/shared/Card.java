
package org.latinschool.shared;

import java.io.Serializable;
public class Card implements Serializable {
    private Suit suit;
    private Rank rank;

    public Card() {

    }

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() { return suit; }

    public Rank getRank() { return rank; }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
