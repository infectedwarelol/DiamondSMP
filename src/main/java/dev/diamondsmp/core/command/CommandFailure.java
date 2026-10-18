package dev.diamondsmp.core.command;

public final class CommandFailure extends RuntimeException {

    private final String key;
    private final transient Object[] placeholders;

    public CommandFailure(String key, Object... placeholders) {
        super(key, null, false, false);
        this.key = key;
        this.placeholders = placeholders;
    }

    public String key() {
        return key;
    }

    public Object[] placeholders() {
        return placeholders;
    }
}
