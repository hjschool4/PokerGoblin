
package org.latinschool.shared.messages;

public class WaitingMessage extends BaseMessage {
    private String message;

    public WaitingMessage() {
        
    }

    public WaitingMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
