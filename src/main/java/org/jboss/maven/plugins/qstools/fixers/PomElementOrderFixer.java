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
package org.jboss.maven.plugins.qstools.fixers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.jboss.maven.plugins.qstoolsc.common.PomOrderUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@Component(role = QSFixer.class, hint = "PomElementOrderFixer")
public class PomElementOrderFixer extends AbstractBaseFixerAdapter {

    @Requirement
    private PomOrderUtil pomOrderUtil;

    @Override
    public String getFixerDescription() {
        return "Fix the pom.xml element order";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        List<String> pomElementsOrder = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getPomOrder();
        Map<String, Node> elementsFound = pomOrderUtil.getElementsOrder(project, doc, pomElementsOrder);
        List<String> elementsList = new ArrayList<String>(elementsFound.keySet());

        for (String element : elementsList) {
            for (String anotherElement : elementsList) {
                if (elementsList.indexOf(element) < elementsList.indexOf(anotherElement)) {
                    Node elementNode = (Node) getxPath().evaluate("/project/" + element, doc, XPathConstants.NODE);
                    XMLUtil.removePreviousWhiteSpace(elementNode);

                    // Get comment over the element
                    Node commentNode = null;
                    Node n = XMLUtil.getPreviousCommentElement(elementNode);
                    if (n != null && n.getNodeType() == Node.COMMENT_NODE) {
                        commentNode = n;
                        XMLUtil.removePreviousWhiteSpace(elementNode.getPreviousSibling());
                    }

                    Node anotherElementNode = (Node) getxPath().evaluate("/project/" + anotherElement, doc, XPathConstants.NODE);
                    anotherElementNode.getParentNode().insertBefore(elementNode, anotherElementNode);

                    // If the element had a comment, move it too.
                    if (commentNode != null) {
                        elementNode.getParentNode().insertBefore(commentNode, anotherElementNode);
                    }
                }
            }
        }
        XMLWriter.writeXML(doc, project.getFile());
    }

    @Override
    public int order() {
        return 10;
    }

}
