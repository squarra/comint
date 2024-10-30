package org.example;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class MessageBuilderMain {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        MessageBuilder builder = new MessageBuilder("ReceiptConfirmationMessage", "3.5.0.0")
                .messageType("300")
                .element("RelatedReference")
                .text("RelatedType", "2006")
                .text("RelatedIdentifier", "a5423bb0-7e18-11ee-b850-005056b36a19")
                .text("RelatedMessageDateTime", "2023-11-08T10:15:43Z");

        String xml = builder.toString();
        System.out.println(xml);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Validator validator = factory.newSchema(new File("src/main/resources/schemas/taf_cat_complete_sector_3.5.0.0.xsd")).newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }
}
