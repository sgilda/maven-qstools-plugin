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

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.Violation;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 * 
 */
@Component(role = QSChecker.class, hint = "SameVersionChecker")
public class SameVersionChecker extends AbstractProjectChecker {

    private String rootVersion;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractProjectChecker#check(org.apache.maven.project.MavenProject,
     * org.apache.maven.execution.MavenSession, java.util.List, org.apache.maven.plugin.logging.Log)
     */
    @Override
    public Map<String, List<Violation>> check(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        try {
            Document doc = PositionalXMLReader.readXML(new FileInputStream(project.getFile()));
            Node versionNode = (Node) getxPath().evaluate("/project/version", doc, XPathConstants.NODE);
            if (versionNode == null) {
                rootVersion = project.getVersion();
            } else {
                rootVersion = versionNode.getTextContent();
            }
        } catch (Exception e) {
            throw new QSCheckerException(e);
        }
        return super.check(project, mavenSession, reactorProjects, log);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check if all modules uses the same version";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.maven.plugins.qstools.checkers.AbstractProjectChecker#processProject(org.apache.maven.project.MavenProject,
     * org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Node versionNode = (Node) getxPath().evaluate("/project/version", doc, XPathConstants.NODE);
        if (versionNode != null && !versionNode.getTextContent().equals(rootVersion)) {
            int lineNumber = getLineNumberFromNode(versionNode);
            String msg = "This project uses a version [%s] different from the root version [%s]";
            addViolation(project.getFile(), results, lineNumber, String.format(msg, versionNode.getTextContent(), rootVersion));
        }
    }

}
