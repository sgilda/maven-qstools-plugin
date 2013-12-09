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

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.QSFixer;
import org.jboss.maven.plugins.qstools.checkers.PomNameChecker;
import org.jboss.maven.plugins.qstools.common.PomNameUtil;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Fixer for {@link PomNameChecker}
 * 
 * @author rafaelbenevides
 * 
 */
@Component(role = QSFixer.class, hint = "PomNameFixer")
public class PomNameFixer extends AbstractBaseFixerAdapter {

    @Requirement
    private PomNameUtil pomNameUtil;

    @Override
    public String getFixerDescription() {
        return "Replace the <name/> on pom.xml with the expected pattern";
    }

    @Override
    public void fixProject(MavenProject project, Document doc) throws Exception {
        Rules rules = getConfigurationProvider().getQuickstartsRules(project.getGroupId());
        String pattern = pomNameUtil.getExpectedPattern(project, rules);
        if (!pattern.equals(project.getName())) {
            Node nameNode = (Node) getxPath().evaluate("/project/name", doc, XPathConstants.NODE);
            if (nameNode == null) {
                nameNode = doc.createElement("name");
                Node projectNode = (Node) getxPath().evaluate("/project", doc, XPathConstants.NODE);
                projectNode.appendChild(doc.createTextNode("    "));
                projectNode.appendChild(nameNode);
            }
            nameNode.setTextContent(pattern);
            XMLWriter.writeXML(doc, project.getFile());
        }

    }

}
