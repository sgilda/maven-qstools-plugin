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
package org.jboss.maven.plugins.qstools.checkers;

import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.common.PomOrderUtil;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "pomElementOrderChecker")
public class PomElementOrderChecker extends AbstractBaseCheckerAdapter {

    @Requirement
    private PomOrderUtil pomOrderUtil;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if POM xml elements are in specific order";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void checkProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        List<String> pomElementsOrder = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getPomOrder();
        Map<String, Node> elementsFound = pomOrderUtil.getElementsOrder(project, doc, pomElementsOrder);

        // Compare found elements order
        String previousElement = null;
        for (String element : elementsFound.keySet()) {
            int lineNumber = XMLUtil.getLineNumberFromNode(elementsFound.get(element));
            if (previousElement != null) {
                int previousElementLineNumber = XMLUtil.getLineNumberFromNode(elementsFound.get(previousElement));
                if (lineNumber < previousElementLineNumber) {
                    String msg = "Element [%s] is not in the correct order: " + pomElementsOrder
                        + ". It shoud come after [%s] on line %s";
                    addViolation(project.getFile(),
                        results,
                        lineNumber,
                        String.format(msg, element, previousElement, previousElementLineNumber));
                }
            }
            previousElement = element;
        }
    }
}
