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
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.jboss.maven.plugins.qstools.DependencyProvider;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractProjectChecker implements QSChecker {

    @Requirement
    private Context context;

    @Requirement
    private DependencyProvider dependencyProvider;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    private Log log;

    private MavenSession mavenSession;

    private int violationsQtd;

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

    protected int getLineNumberFromNode(Node node) {
        return Integer.parseInt((String) node.getUserData(PositionalXMLReader.LINE_NUMBER_KEY_NAME));
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

    /**
     * @return the dependencyProvider
     */
    protected DependencyProvider getDependencyProvider() {
        return dependencyProvider;
    }

    /**
     * @return the xPath
     */
    protected XPath getxPath() {
        return xPath;
    }

    /**
     * @return the log
     */
    protected Log getLog() {
        return log;
    }

    /**
     * @return the mavenSession
     */
    protected MavenSession getMavenSession() {
        return mavenSession;
    }

}
