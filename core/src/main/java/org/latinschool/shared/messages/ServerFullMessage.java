
package org.latinschool.shared.messages;

public class ServerFullMessage extends BaseMessage {
    private String message;

    public ServerFullMessage() {
        
    }

    public ServerFullMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
