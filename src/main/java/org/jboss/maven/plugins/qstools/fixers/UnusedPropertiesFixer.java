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
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSToolsException;
import org.jboss.maven.plugins.qstools.common.UnusedPropertiesUtil;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.PositionalXMLReader;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Fixer for {@link org.jboss.maven.plugins.qstools.checkers.UnusedPropertiesChecker}
 * 
 * @author Paul Robinson
 */
@Component(role = QSFixer.class, hint = "UnusedPropertiesFixer")
public class UnusedPropertiesFixer implements QSFixer {

    protected XPath xPath = XPathFactory.newInstance().newXPath();

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Requirement
    private UnusedPropertiesUtil unusedPropertiesUtil;

    @Override
    public String getFixerDescription() {
        return "Remove unused properties from pom.xml files";
    }

    @Override
    public void fix(MavenProject project, MavenSession mavenSession, List<MavenProject> reactorProjects, Log log)
        throws QSToolsException {

        try {
            Rules rules = configurationProvider.getQuickstartsRules(project.getGroupId());
            List<UnusedPropertiesUtil.PomInformation> unusedPropertyInfo = unusedPropertiesUtil.findUnusedProperties(reactorProjects,
                rules);

            for (UnusedPropertiesUtil.PomInformation pomInfo : unusedPropertyInfo) {

                Document doc = PositionalXMLReader.readXML(new FileInputStream(pomInfo.getProject().getFile()));
                Node unusedPropertyNode = (Node) xPath.evaluate("/project/properties/" + pomInfo.getProperty(),
                    doc,
                    XPathConstants.NODE);

                // Get comment over the element
                Node commentNode = null;
                if (unusedPropertyNode.getPreviousSibling() != null
                    && unusedPropertyNode.getPreviousSibling() != null
                    && unusedPropertyNode.getPreviousSibling().getPreviousSibling().getNodeType() == Node.COMMENT_NODE) {
                    commentNode = unusedPropertyNode.getPreviousSibling().getPreviousSibling();
                }
                // If the element had a comment, remove it too.
                if (commentNode != null) {
                    XMLUtil.removePreviousWhiteSpace(commentNode);
                    commentNode.getParentNode().removeChild(commentNode);
                }
                XMLUtil.removePreviousWhiteSpace(unusedPropertyNode);
                unusedPropertyNode.getParentNode().removeChild(unusedPropertyNode);

                XMLUtil.writeXML(doc, pomInfo.getProject().getFile());
            }

        } catch (Exception e) {
            throw new QSToolsException(e);
        }
    }

    @Override
    public int order() {
        return 0;
    }

}
