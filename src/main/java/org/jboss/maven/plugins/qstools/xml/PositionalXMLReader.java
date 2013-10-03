/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Rafael Benevides
 * 
 */
public class PositionalXMLReader {

    /**
     * Attribute name to identify line for the beginning of current element : {@value}
     * 
     * @see Element#getUserData(String)
     */
    final public static String BEGIN_LINE_NUMBER_KEY_NAME = "beginLineNumber";

    /**
     * Attribute name to identify column for the beginning of current element : {@value}
     * 
     * @see Element#getUserData(String)
     */
    final public static String BEGIN_COLUMN_NUMBER_KEY_NAME = "beginColumnNumber";

    /**
     * Attribute name to identify line for the ending of current element : {@value}
     * 
     * @see Element#getUserData(String)
     */
    final public static String END_LINE_NUMBER_KEY_NAME = "endLineNumber";

    /**
     * Attribute name to identify column for the ending of current element : {@value}
     * 
     * @see Element#getUserData(String)
     */
    final public static String END_COLUMN_NUMBER_KEY_NAME = "endColumnNumber";

    public static Document readXML(final InputStream xmlInputStream) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(false);
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<Element>();
        final StringBuilder textBuffer = new StringBuilder();

        final DefaultHandler handler = new DefaultHandler2() {
            private Locator locator;
            private int prevLineNumber = 0;
            private int prevColumnNumber = 0;

            @Override
            public void setDocumentLocator(final Locator locator)
            {
                this.locator = locator; // Save the locator, so that it can be used later
                                        // for line tracking when traversing nodes.
            }

            private void updateLocator()
            {
                prevLineNumber = this.locator.getLineNumber();
                prevColumnNumber = this.locator.getColumnNumber();
            }

            @Override
            public void startElement(
                final String uri,
                final String localName,
                final String qName,
                final Attributes attributes
                ) throws SAXException
            {
                addTextIfNeeded();

                final Element element = doc.createElement(qName);

                for (int i = 0; i < attributes.getLength(); i++) {
                    element.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }

                element.setUserData(PositionalXMLReader.BEGIN_LINE_NUMBER_KEY_NAME, Integer.valueOf(prevLineNumber), null);
                element.setUserData(PositionalXMLReader.BEGIN_COLUMN_NUMBER_KEY_NAME, Integer.valueOf(prevColumnNumber), null);

                updateLocator();

                element.setUserData(PositionalXMLReader.END_LINE_NUMBER_KEY_NAME, Integer.valueOf(prevLineNumber), null);
                element.setUserData(PositionalXMLReader.END_COLUMN_NUMBER_KEY_NAME, Integer.valueOf(prevColumnNumber), null);

                elementStack.push(element);
            }

            @Override
            public void endElement(
                final String uri,
                final String localName,
                final String qName
                )
            {

                addTextIfNeeded();

                final Element closedEl = elementStack.pop();

                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                }
                else {
                    final Element parentEl = elementStack.peek();

                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void startEntity(String name) throws SAXException
            {
                // addTextOrCommentIfNeeded(3);
            }

            @Override
            public void endEntity(String name) throws SAXException
            {
                // addTextOrCommentIfNeeded(4);
            }

            @Override
            public void startCDATA()
            {
                // addTextOrCommentIfNeeded(5);
            }

            @Override
            public void endCDATA()
            {
                // addTextOrCommentIfNeeded(6);
            }

            @Override
            public void characters(
                final char[] ch,
                final int start,
                final int length
                ) throws SAXException
            {
                textBuffer.append(ch, start, length);
                // updateLocator();
            }

            // Report an XML comment anywhere in the document.
            @Override
            public void comment(
                final char[] ch,
                final int start,
                final int length
                ) throws SAXException
            {
                addTextIfNeeded();
                addComment(new String(ch, start, length));
                updateLocator();
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded()
            {
                if (textBuffer.length() > 0) {
                    final Element element = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());

                    element.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }

            }

            // Outputs text accumulated under the current node
            private void addComment(final String comment)
            {

                if (elementStack.isEmpty()) { // Is this the root element?
                    Comment cmt = doc.createComment(comment);
                    doc.appendChild(cmt);
                }
                else {
                    final Element element = elementStack.peek();
                    final Node commentNode = doc.createComment(comment);

                    element.appendChild(commentNode);
                }

            }
        };

        // Needed for DefaultHandler2
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        parser.parse(xmlInputStream, handler);

        return doc;
    }

}
