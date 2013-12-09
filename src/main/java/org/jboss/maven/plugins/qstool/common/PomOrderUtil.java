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
package org.jboss.maven.plugins.qstool.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component(role = PomOrderUtil.class)
public class PomOrderUtil {

    public Map<String, Node> getElementsOrder(MavenProject project, Document doc, List<String> pomOrder)
        throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Map<String, Node> elementsFound = new LinkedHashMap<String, Node>();
        // Find all elements position
        for (String element : pomOrder) {
            Node elementNode = (Node) xPath.evaluate("/project/" + element, doc, XPathConstants.NODE);
            if (elementNode != null) {
                elementsFound.put(element, elementNode);
            }

        }
        return elementsFound;
    }
}
