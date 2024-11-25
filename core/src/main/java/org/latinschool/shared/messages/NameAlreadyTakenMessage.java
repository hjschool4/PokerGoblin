
package org.latinschool.shared.messages;

public class NameAlreadyTakenMessage extends BaseMessage {
    private String message;

    public NameAlreadyTakenMessage() {
        
    }

    public NameAlreadyTakenMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
