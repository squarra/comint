package org.example;

import jakarta.xml.soap.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageValidatorTest {
    private static final String MESSAGE_IDENTIFIER = "00000000-0000-0000-0000-000000000000";

    @Mock
    private MessageHeaderExtractor messageHeaderExtractor;
    @Mock
    private XmlSchemaService xmlSchemaService;

    private MessageValidator messageValidator;
    private Messages messages;
    private Object message;

    @BeforeEach
    void setUp() {
        messages = new Messages(ResourceBundle.getBundle("messages"));
        messageValidator = new MessageValidator(messageHeaderExtractor, messages, xmlSchemaService);
    }

    @Test
    void shouldRejectNotANode() {
        message = 10;
        assertSchemaValidationException(MessageKey.MESSAGE_NOT_NODE);
    }

    @Test
    void shouldRejectNotAnElement() {
        message = createMessage().getDocument();
        assertSchemaValidationException(MessageKey.MESSAGE_NOT_ELEMENT, Node.DOCUMENT_NODE);
    }

    private void assertSchemaValidationException(MessageKey messageKey, Object... args) {
        SchemaValidationException exception = assertThrows(
                SchemaValidationException.class,
                () -> messageValidator.validateMessage(message, MESSAGE_IDENTIFIER));
        assertEquals(messages.get(messageKey, args), exception.getMessage());
    }

    private TestMessageBuilder createMessage() {
        return new TestMessageBuilder("ReceiptConfirmationMessage", "3.5.0.0")
                .messageType("300")
                .element("RelatedReference")
                .text("RelatedType", "2006")
                .text("RelatedIdentifier", "a5423bb0-7e18-11ee-b850-005056b36a19")
                .text("RelatedMessageDateTime", "2023-11-08T10:15:43Z");
    }
}