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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "samePropertyValueChecker")
public class SamePropertyValueChecker implements QSChecker {

    private int violationsQtd;

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    private Properties projectProperties = new Properties();

    @Requirement
    private ConfigurationProvider configurationProvider;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#check(org.apache.maven.project.MavenProject,
     * org.apache.maven.execution.MavenSession, java.util.List, org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();
        Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
        try {
            if (rules.isCheckerIgnored(this)) {
                String msg = "Skiping %s for %s:%s";
                log.warn(String.format(msg,
                    this.getClass().getSimpleName(),
                    project.getGroupId(),
                    project.getArtifactId()));
            } else {
                // iterate over all reactor projects to iterate on all declared properties
                for (MavenProject mavenProject : reactorProjects) {
                    Document doc = PositionalXMLReader.readXML(new FileInputStream(mavenProject.getFile()));
                    NodeList propertiesNodes = (NodeList) xPath.evaluate("/project/properties/*", doc, XPathConstants.NODESET);
                    // find all declared properties
                    for (int x = 0; x < propertiesNodes.getLength(); x++) {
                        Node property = propertiesNodes.item(x);
                        String propertyName = property.getNodeName();
                        String propertyValue = property.getTextContent();

                        //skip ignored property
                        if (rules.getIgnoredDifferentValuesProperties().contains(propertyName)) {
                            continue;
                        }

                        if (projectProperties.get(propertyName) == null) {
                            projectProperties.put(propertyName, propertyValue);
                        } else if (projectProperties.get(propertyName) != null && !projectProperties.get(propertyName).equals(propertyValue)) {
                            // The property was used but with an different value
                            int lineNumber = (Integer) property.getUserData(PositionalXMLReader.BEGIN_LINE_NUMBER_KEY_NAME);
                            String rootDirectory = (mavenSession.getExecutionRootDirectory() + File.separator).replace("\\", "\\\\");
                            String fileAsString = mavenProject.getFile().getAbsolutePath().replace(rootDirectory, "");
                            if (results.get(fileAsString) == null) {
                                results.put(fileAsString, new ArrayList<Violation>());
                            }
                            String msg = "Property [%s] was declared with a value [%s] that differ from previous value [%s]";
                            results.get(fileAsString).add(
                                new Violation(getClass(), lineNumber, String.format(msg, propertyName, propertyValue, projectProperties.get(propertyName))));
                            violationsQtd++;
                        }
                    }
                }
                if (violationsQtd > 0) {
                    log.info("There are " + violationsQtd + " checkers errors");
                }
            }

        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check if all declared properties uses the same version value across the projects/modules";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getViolatonsQtd()
     */
    @Override
    public int getViolatonsQtd() {
        return violationsQtd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#resetViolationsQtd()
     */
    @Override
    public void resetViolationsQtd() {
        violationsQtd = 0;
    }

}
