package org.latinschool.shared.messages;


public class PlayerAction extends BaseMessage {
    public enum ActionType { CHECK, FOLD, CALL, RAISE }

    private ActionType action;
    private int amount; 

    public PlayerAction() {}

    public PlayerAction(ActionType action, int amount) {
        this.action = action;
        this.amount = amount;
    }

    public ActionType getAction() { return action; }

    public int getAmount() { return amount; }
}

