package greed.client.event;

public class ChatMessageEvent extends Event {

    private final String message;

    public ChatMessageEvent(String message) {
        this.message = message;

    }

    public String getMessage() {
        return message;
    }

}
