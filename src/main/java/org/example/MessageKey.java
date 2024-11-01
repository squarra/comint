package org.example;

public enum MessageKey {
    MESSAGE_NOT_NODE("message.not.node"),
    MESSAGE_NOT_ELEMENT("message.not.element"),
    MESSAGE_WRONG_ROOT("message.wrong.root"),
    MESSAGE_MULTIPLE_CHILDREN("message.multiple.children"),
    MESSAGE_IDENTIFIER_MISSING("message.identifier.missing"),
    MESSAGE_IDENTIFIER_MISMATCH("message.identifier.mismatch"),
    MESSAGE_TYPE_VERSION_MISSING("message.type.version.missing"),
    SCHEMA_NOT_FOUND("schema.not.found");

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
