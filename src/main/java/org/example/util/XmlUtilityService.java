package org.example.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@ApplicationScoped
public class XmlUtilityService {

    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;
    private final SchemaFactory schemaFactory;

    public XmlUtilityService() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        configureSecureDocumentBuilderFactory();
        transformerFactory = TransformerFactory.newInstance();
        configureSecureTransformerFactory();
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        configureSecureSchemaFactory();
    }

    private void configureSecureDocumentBuilderFactory() {
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureSecureTransformerFactory() {
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureSecureSchemaFactory() {
        try {
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        return documentBuilderFactory.newDocumentBuilder();
    }

    public Document createDocument() throws ParserConfigurationException {
        return createDocumentBuilder().newDocument();
    }

    public Transformer createTransformer() {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            return transformer;
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Schema createSchema(InputStream schemaFile) throws SAXException {
        return schemaFactory.newSchema(new StreamSource(schemaFile));
    }

    public Document parseXmlString(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        return createDocumentBuilder().parse(new InputSource(new StringReader(xmlString)));
    }
}
