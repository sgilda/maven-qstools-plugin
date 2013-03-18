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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;
import org.jboss.maven.plugins.qschecker.Violation;
import org.jboss.maven.plugins.qschecker.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "unusedPropertiesChecker")
public class UnusedPropertiesChecker implements QSChecker {

    private Map<String, PomInformation> declaredProperties = new HashMap<String, PomInformation>();

    private Set<String> usedProperties = new HashSet<String>();

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Checks if a project and its modules has a declared and unused propery";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qschecker.QSChecker#check(org.apache.maven.project.MavenProject,
     * org.apache.maven.execution.MavenSession, java.util.List, org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        try {
            // iterate over all reactor projects to find what properties was declared and what was really used
            for (MavenProject mavenProject : reactorProjects) {
                Document doc = PositionalXMLReader.readXML(new FileInputStream(mavenProject.getFile()));
                NodeList propertiesNodes = (NodeList) xPath.evaluate("/project/properties/*", doc, XPathConstants.NODESET);
                NodeList allNodes = (NodeList) xPath.evaluate("//*", doc, XPathConstants.NODESET);
                // find all declared properties
                for (int x = 0; x < propertiesNodes.getLength(); x++) {
                    Node property = propertiesNodes.item(x);
                    String propertyName = property.getNodeName();
                    int lineNumber = Integer.parseInt((String) property.getUserData(PositionalXMLReader.LINE_NUMBER_KEY_NAME));
                    PomInformation pi = new PomInformation(mavenProject, lineNumber);
                    declaredProperties.put(propertyName, pi);
                }
                // find all uses for properties expression
                Pattern p = Pattern.compile("\\$\\{\\w+(.\\w+)*(-\\w+)*\\}");
                for (int x = 0; x < allNodes.getLength(); x++) {
                    Node node = allNodes.item(x);
                    String nodeContent = node.getTextContent();
                    if (p.matcher(nodeContent).matches()) {
                        String usedProperty = node.getTextContent().replaceAll("[${}]", "");
                        usedProperties.add(usedProperty);
                    }
                }
            }
            // search if all declared properties have been used
            for (String declared : declaredProperties.keySet()) {
                if (!declared.startsWith("project") && // Escape project configuration
                        !usedProperties.contains(declared)) {
                    PomInformation pomInformation = declaredProperties.get(declared);
                    // Get relative path based on maven work dir
                    String fileAsString = pomInformation.getProject().getFile().getAbsolutePath().replaceAll((mavenSession.getExecutionRootDirectory() + "/"), "");
                    if (results.get(fileAsString) == null) {
                        results.put(fileAsString, new ArrayList<Violation>());
                    }
                    String msg = "Property [%s] was declared but was never used";
                    results.get(fileAsString).add(new Violation(getClass(), pomInformation.getLine(), String.format(msg, declared)));
                }
            }
            if (results.size() > 0) {
                log.info("There are " + results.size() + " checkers errors");
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return results;
    }

    private class PomInformation {
        private MavenProject project;

        private int line;

        public PomInformation(MavenProject project, int line) {
            this.project = project;
            this.line = line;
        }

        public MavenProject getProject() {
            return project;
        }

        public int getLine() {
            return line;
        }

    }

}
