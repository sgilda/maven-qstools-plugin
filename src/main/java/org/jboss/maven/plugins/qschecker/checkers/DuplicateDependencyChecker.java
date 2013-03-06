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
package org.jboss.maven.plugins.qschecker.checkers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.Violation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "duplicateDependencyChecker")
public class DuplicateDependencyChecker extends AbstractProjectChecker {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check if the POM has any duplicate dependency";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.checkers.AbstractPomChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        // Check Managed Dependencies
        Set<String> declaredManagedDependencies = new HashSet<String>();
        NodeList managedDependency = (NodeList) xPath.evaluate("/project/dependencyManagement/dependencies/dependency/artifactId", doc, XPathConstants.NODESET);
        for (int x = 0; x < managedDependency.getLength(); x++) {
            Node artifact = managedDependency.item(x);
            String artifactName = artifact.getTextContent();
            int lineNumber = getLineNumberFromNode(artifact);
            if (!declaredManagedDependencies.add(artifactName)) { // return false if already exists
                String msg = "Managed Dependency [%s] is declared more than once";
                addViolation(project.getFile(), results, lineNumber, String.format(msg, artifactName));
            }
        }
        // Check Dependencies
        Set<String> declaredDependencies = new HashSet<String>();
        NodeList dependencies = (NodeList) xPath.evaluate("/project/dependencies/dependency/artifactId", doc, XPathConstants.NODESET);
        for (int x = 0; x < dependencies.getLength(); x++) {
            Node artifact = dependencies.item(x);
            String artifactName = artifact.getTextContent();
            int lineNumber = getLineNumberFromNode(artifact);
            if (!declaredDependencies.add(artifactName)) { // return false if already exists
                String msg = "Dependency [%s] is declared more than once";
                addViolation(project.getFile(), results, lineNumber, String.format(msg, artifactName));
            }
        }

    }

}
