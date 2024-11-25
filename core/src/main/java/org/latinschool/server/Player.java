
package org.latinschool.server;

import com.esotericsoftware.kryonet.Connection;
import org.latinschool.shared.Card;

public class Player {
    private Connection connection;
    private String name;
    private int chips;
    private boolean hasFolded;
    private Card[] cards;
    private boolean isTurn;
    private String lastAction; 

    private int contributed;
    public Player(Connection connection, String name) {
        this.connection = connection;
        this.name = name;
        this.chips = 1000; 
        this.hasFolded = false;
        this.cards = new Card[0];
        this.isTurn = false;
        this.lastAction = "none"; 
        this.contributed = 0;
    }

    public Connection getConnection() { return connection; }
    public String getName() { return name; }
    public int getChips() { return chips; }
    public boolean hasFolded() { return hasFolded; }
    public Card[] getCards() { return cards; }
    public boolean isTurn() { return isTurn; }
    public String getLastAction() { return lastAction; } 

    public void setCards(Card[] cards) { this.cards = cards; }
    public void setFolded(boolean folded) { this.hasFolded = folded; }
    public void setTurn(boolean turn) { this.isTurn = turn; }
    public void setLastAction(String action) { this.lastAction = action; } 


    public void bet(int amount) {
        if (amount > chips) {
            amount = chips; 
        }
        chips -= amount;
    }

    public void addChips(int amount) {
        chips += amount;
    }

    public void reset() {
        this.hasFolded = false;
        this.cards = new Card[0];
        this.isTurn = false;
        this.lastAction = "none"; 
    }
    public int getContributed() {
        return this.contributed;
    }

    public void addContributed(int amount) {
        this.contributed += amount;
    }

    public void resetContributed() {
        this.contributed = 0;
    }
}
