package org.latinschool.shared.messages;

public class ErrorMessage extends BaseMessage {
    private String message;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
