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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.jboss.maven.plugins.qschecker.QSChecker;
import org.jboss.maven.plugins.qschecker.QSCheckerException;
import org.jboss.maven.plugins.qschecker.Violation;
import org.jboss.maven.plugins.qschecker.maven.MavenDependency;
import org.jboss.maven.plugins.qschecker.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractProjectChecker implements QSChecker {

    private StringSearchInterpolator interpolator = new StringSearchInterpolator();

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    protected Log log;

    protected MavenSession mavenSession;
    
    private int violationsQtd;
    
    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getViolatonsQtd()
     */
    @Override
    public int getViolatonsQtd() {
        return violationsQtd;
    }
    
    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#resetViolationsQtd()
     */
    @Override
    public void resetViolationsQtd() {
        violationsQtd = 0;
    }

    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        this.mavenSession = mavenSession;
        this.log = log;
        Map<String, List<Violation>> results = new TreeMap<String, List<Violation>>();

        try {
            for (MavenProject mavenProject : reactorProjects) {
                Document doc = PositionalXMLReader.readXML(new FileInputStream(mavenProject.getFile()));
                processProject(mavenProject, doc, results);
            }
            if (results.size() > 0) {
                violationsQtd = results.size();
                log.info("There are " + results.size() + " checkers errors");
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return results;
    }

    protected MavenDependency getDependencyFromNode(MavenProject project, Node dependency) throws InterpolationException {
        String groupId = null;
        String artifactId = null;
        String declaredVersion = null;
        String interpoledVersion = null;
        String type = null;
        String scope = null;
        for (int x = 0; x < dependency.getChildNodes().getLength(); x++) {
            Node node = dependency.getChildNodes().item(x);
            if ("groupId".equals(node.getNodeName())) {
                groupId = node.getTextContent();
            }
            if ("artifactId".equals(node.getNodeName())) {
                artifactId = node.getTextContent();
            }
            if ("version".equals(node.getNodeName())) {
                declaredVersion = node.getTextContent();
                interpoledVersion = resolveMavenProperty(project, declaredVersion);
            }
            if ("type".equals(node.getNodeName())) {
                type = node.getTextContent();
            }
            if ("scope".equals(node.getNodeName())) {
                scope = node.getTextContent();
            }
        }
        return new MavenDependency(groupId, artifactId, declaredVersion, interpoledVersion, type, scope);
    }
    
    
    protected int getLineNumberFromNode(Node node){
        return Integer.parseInt((String) node.getUserData(PositionalXMLReader.LINE_NUMBER_KEY_NAME));
    }

    private String resolveMavenProperty(MavenProject project, String textContent) throws InterpolationException {
        interpolator.clearFeedback(); // Clear the feedback messages from previous interpolate(..) calls.
        // Associate project.model with ${project.*} and ${pom.*} prefixes
        PrefixedValueSourceWrapper modelWrapper = new PrefixedValueSourceWrapper(new ObjectBasedValueSource(project.getModel()), "project.", true);
        interpolator.addValueSource(modelWrapper);
        interpolator.addValueSource(new PropertiesBasedValueSource(project.getModel().getProperties()));
        return interpolator.interpolate(textContent);
    }

    /**
     * Adds violation referencing the pom.xml file as the violated file
     * 
     */
    protected void addViolation(final File file, final Map<String, List<Violation>> results, int lineNumber, String violationMessage) {
        // Get relative path based on maven work dir
        String fileAsString = file.getAbsolutePath().replaceAll((mavenSession.getExecutionRootDirectory() + "/"), "");
        if (results.get(fileAsString) == null) {
            results.put(fileAsString, new ArrayList<Violation>());
        }
        results.get(fileAsString).add(new Violation(getClass(), lineNumber, violationMessage));
    }

    public abstract void processProject(final MavenProject project, Document doc, final Map<String, List<Violation>> results) throws Exception;

}
