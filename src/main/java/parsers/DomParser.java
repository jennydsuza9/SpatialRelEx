/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.parsers;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import main.java.spatialrelex.markup.Doc;
import main.java.spatialrelex.markup.SpatialElement;
import main.java.spatialrelex.markup.SpatialRelation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jenny D'Souza
 */
public class DomParser {
    
    static Document dom;
    
    private static void parseSpatialRelations(NodeList nl, String type, Doc document) throws IOException {
        if (nl == null || nl.getLength() <= 0)
            return;
        
        for (int i = 0; i < nl.getLength(); i++) {
            
            Element el = (Element)nl.item(i);
            type = (type.equals("QSLINK") || type.equals("OLINK")) ? "LINK" : type;
            SpatialRelation sr = new SpatialRelation(type, "1"); 
            sr.processRelation(document, el);
        }
    }    
    
    private static void parseSpatialElements(NodeList nl, String type, Doc document) throws IOException {
        if (nl == null || nl.getLength() <= 0)
            return;
        
        for (int i = 0; i < nl.getLength(); i++) {
            
            Element el = (Element)nl.item(i);
            
            SpatialElement se = new SpatialElement(type, el);
            
            if (se.start == -1 && se.end == -1)
                continue;
            
            document.addDocSpatialElement(se);
        }
    }        
    
    /**
     * Parses a document for its text, its spatial element types if type flag is set,
     * and spatial relation participant roles if roles flag is set.
     * 
     */
    private static void parseDocument(Doc document) throws IOException, UnsupportedEncodingException, InterruptedException {
        //get the root element
        Element docEle = (Element) dom.getDocumentElement();

        document.text = docEle.getElementsByTagName("TEXT").item(0).getTextContent();
        
        //applies the Stanford CoreNLP parser to the text.
        StanfordParser.parse(document);                
        
        for (String type : SpatialElement.TYPES) {
            //gets nodelist of all spatial elements of type
            NodeList nl = docEle.getElementsByTagName(type);
            parseSpatialElements(nl, type, document);
        }
        
        for (String type : SpatialRelation.TYPES) {
            //gets nodelist of all spatial relations of type
            NodeList nl = docEle.getElementsByTagName(type);
            parseSpatialRelations(nl, type, document);
        }
    }    
    
    /**
     * Parses an XML file for text and its spatial information.
     * 
     * @param xmlFileName is the file being parsed.
     * @param document is the object which stores the spatial information of the file.
     * @throws org.xml.sax.SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public static void parseXmlAnnotationsFile(String xmlFileName, Doc document) throws org.xml.sax.SAXException, ParserConfigurationException, IOException, UnsupportedEncodingException, InterruptedException{
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();

        //parse using builder to get DOM representation of the XML file
        dom = db.parse(xmlFileName);
        parseDocument(document);
    }          
    
    public static String prettyFormat(String input, int indent) throws IOException {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer(); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "US-ASCII");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {            
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    public static String prettyFormat(String input) throws IOException {
        return prettyFormat(input, 2);
    }     
    
}
