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

import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jboss.maven.plugins.qstools.QSChecker;
import org.jboss.maven.plugins.qstools.Violation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Rafael Benevides
 *
 */
@Component(role = QSChecker.class, hint = "mavenCompilerChecker")
public class MavenCompilerChecker extends AbstractProjectChecker {

    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qstools.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Check for the right usage of maven-compile-plugin";
    }

    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qstools.checkers.AbstractProjectChecker#processProject(org.apache.maven.project.MavenProject, org.w3c.dom.Document, java.util.Map)
     */
    @Override
    public void processProject(MavenProject project, Document doc, Map<String, List<Violation>> results) throws Exception {
        String compilerSource = getConfigurationProvider().getQuickstartsRules(project.getGroupId()).getExpectedCompilerSource();
        String target = project.getProperties().getProperty("maven.compiler.target");
        String compiler = project.getProperties().getProperty("maven.compiler.source");
        if (target == null || compiler == null){
            addViolation(project.getFile(), results, 1, "pom.xml should define <maven.compiler.source/> and <maven.compiler.target/> properties");
        }else if (!target.equals(compilerSource) || !compiler.equals(compilerSource)){
            addViolation(project.getFile(), results, 1, "<maven.compiler.source/> and <maven.compiler.target/> should be set to " + compilerSource);
        }
        List<Plugin> plugins = project.getModel().getBuild().getPlugins();
        boolean containsSourceDefinition = false;
        for (Plugin plugin: plugins){
            if (plugin.getArtifactId().equals("maven-compiler-plugin")){
                Xpp3Dom pluginConfig = (Xpp3Dom) plugin.getConfiguration();
                if (pluginConfig == null || pluginConfig.getChildren().length == 0){
                    Node compilerNode = (Node) getxPath().evaluate("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']", doc, XPathConstants.NODE);
                    int lineNumber = compilerNode == null ? -1 : getLineNumberFromNode(compilerNode);
                    //Plugin exist on model but it isn't on XML
                    if (lineNumber != -1){
                        addViolation(project.getFile(), results, lineNumber, "You should NOT declare 'maven-compile-plugin'");
                    }
                }else{
                    Xpp3Dom[] configurations = pluginConfig.getChildren();
                    for (Xpp3Dom config: configurations){
                        if (config.getName().equals("source") || config.getName().equals("target")){
                            containsSourceDefinition = true;
                        }
                    }
                }
            }
        }
        if (containsSourceDefinition){
            Node compilerNode = (Node) getxPath().evaluate("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration", doc, XPathConstants.NODE);
            int lineNumber = compilerNode == null ? -1 : getLineNumberFromNode(compilerNode);
            if (lineNumber != -1){
                addViolation(project.getFile(), results, lineNumber, "You should not define 'source' or 'target' for 'maven-compiler-plugin'");
            }
        }

    }

}
