package net.cyberpunk042.log;

/**
 * Who receives chat-forwarded log messages.
 */
public enum ChatRecipients {
    OPS,
    ALL,
    NONE;
    
    public static ChatRecipients parse(String value) {
        if (value == null) return OPS;
        String upper = value.trim().toUpperCase();
        for (ChatRecipients r : values()) {
            if (r.name().equals(upper)) return r;
        }
        return OPS;
    }
}
