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

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.jboss.maven.plugins.qstools.Constants;
import org.jboss.maven.plugins.qstools.DependencyProvider;
import org.jboss.maven.plugins.qstools.QSCheckerException;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.w3c.dom.Document;

public abstract class AbstractProjectFixer implements QSFixer {

    @Requirement
    private Context context;

    @Requirement
    private DependencyProvider dependencyProvider;

    @Requirement
    private ConfigurationProvider configurationProvider;

    private XPath xPath = XPathFactory.newInstance().newXPath();

    private Log log;

    private MavenSession mavenSession;

    @Override
    @SuppressWarnings("unchecked")
    public void fix(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log) throws QSCheckerException {
        this.mavenSession = mavenSession;
        this.log = log;
        try {
            List<String> ignoredQuickstarts = (List<String>) context.get(Constants.IGNORED_QUICKSTARTS_CONTEXT);
            for (MavenProject mavenProject : reactorProjects) {
                if (!ignoredQuickstarts.contains(mavenProject.getBasedir().getName())) {
                    Document doc = PositionalXMLReader.readXML(new FileInputStream(mavenProject.getFile()));
                    processProject(mavenProject, doc);
                } else {
                    log.debug("Ignoring " + mavenProject.getBasedir().getName() + ". It is listed on .quickstarts_ignore file");
                }
            }
        } catch (Exception e) {
            // Any other exception is a problem.
            throw new QSCheckerException(e);
        }
    }

    public abstract void processProject(final MavenProject project, Document doc) throws Exception;

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

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return the configurationProvider
     */
    public ConfigurationProvider getConfigurationProvider() {
        return configurationProvider;
    }

    public void writeXML(Document doc, OutputStream out) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // NOI18N
        t.setOutputProperty(OutputKeys.INDENT, "yes"); // NOI18N
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // NOI18N
        Source source = new DOMSource(doc);
        Result result = new StreamResult(out);
        t.transform(source, result);
    }

}
