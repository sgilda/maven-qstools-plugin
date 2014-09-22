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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtil {

    public static void removePreviousWhiteSpace(Node node) {

        Node prev = node.getPreviousSibling();
        if (prev != null && prev.getNodeType() == Node.TEXT_NODE && prev.getNodeValue().trim().length() == 0) {
            node.getParentNode().removeChild(prev);
        }
    }

    public static int getLineNumberFromNode(Node node) {
        if (node == null) {
            return 0;
        }
        return (Integer) node.getUserData(PositionalXMLReader.BEGIN_LINE_NUMBER_KEY_NAME);
    }

    /**
     * Checks if a Node has a valid element
     * 
     * @param node Node to be verified
     * 
     * @return true if has Child elements
     */
    public static boolean hasChildElements(Node node) {
        NodeList nodelist = node.getChildNodes();
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node childNode = nodelist.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

}
