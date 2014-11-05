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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.common.ProjectUtil;
import org.jboss.maven.plugins.qstools.config.Rules;
import org.jboss.maven.plugins.qstools.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rafael Benevides
 *
 */
@Component(role = QSChecker.class, hint = "MavenApprovedRepositoriesChecker")
public class MavenApprovedRepositoriesChecker extends AbstractBaseCheckerAdapter {

    @Requirement
    private ProjectUtil projectUtil;

    @Override
    public String getCheckerDescription() {
        return "Check if the Quickstart Contains the JBoss Maven Repository profile";
    }

    @Override
    public void checkProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        Rules rules = getConfigurationProvider().getQuickstartsRules(project.getGroupId());
        if (!rules.isCheckerIgnored(MavenCentralRepositoryChecker.class)) {
            this.setCheckerMessage("RepositoryChecker is ignored because MavenCentralRepositoryChecker is active");
        } else {
            Node repositoriesNode = (Node) getxPath().evaluate("/project/repositories", doc, XPathConstants.NODE);
            // only valid for top-level projects
            if (!projectUtil.isSubProjec(project)) {
                if (repositoriesNode == null) {
                    addViolation(project.getFile(), results, 0, "pom.xml doesn't contains the JBoss Maven Repository profile");
                } else {
                    NodeList ids = (NodeList) getxPath().evaluate("/project/repositories/repository/id", doc, XPathConstants.NODESET);
                    Set<String> approvedIds = rules.getMavenApprovedRepositories().keySet();
                    for (int x = 0; x < ids.getLength(); x++) {
                        String id = ids.item(x).getTextContent();
                        if (!approvedIds.contains(id)) {
                            int lineNumber = XMLUtil.getLineNumberFromNode(ids.item(x));
                            addViolation(project.getFile(), results, lineNumber, "The following id [" + id + "] is not an approved JBoss Maven Repository Id - Please, run mvn org.jboss.maven.plugins:qstools:repositories to fix it");
                        }
                    }
                    NodeList urls = (NodeList) getxPath().evaluate("/project/repositories/repository/url", doc, XPathConstants.NODESET);
                    Collection<String> approvedUrlsRaw = rules.getMavenApprovedRepositories().values();
                    Set<String> approvedUrls = new HashSet<String>();
                    for (String  rawValue: approvedUrlsRaw){
                        approvedUrls.add(rawValue.split("[|]")[0]);
                    }
                    for (int x = 0; x < ids.getLength(); x++) {
                        String url = urls.item(x).getTextContent();
                        if (!approvedUrls.contains(url)) {
                            int lineNumber = XMLUtil.getLineNumberFromNode(urls.item(x));
                            addViolation(project.getFile(), results, lineNumber, "The following url [" + url + "] is not an approved JBoss Maven Repository URL - Please, run mvn org.jboss.maven.plugins:qstools:repositories to fix it");
                        }
                    }
                }
            }
        }
    }
}
